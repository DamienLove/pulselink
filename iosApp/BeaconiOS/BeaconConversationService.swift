import Foundation
#if canImport(FirebaseFirestore)
import FirebaseFirestore
import FirebaseAuth
#endif

protocol BeaconConversationProvider {
    // Legacy load
    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]]
    // Realtime listeners
    func listenToConversations(onChange: @escaping ([BeaconContactCard]) -> Void) -> ListenerRegistration?
    func listenToMessages(for contact: BeaconContactCard, onChange: @escaping ([BeaconConversationMessage]) -> Void) -> ListenerRegistration?
    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws
}

#if canImport(FirebaseFirestore)
// ListenerRegistration is available
#else
protocol ListenerRegistration {
    func remove()
}
class MockListener: ListenerRegistration {
    func remove() {}
}
#endif

final class MockBeaconConversationProvider: BeaconConversationProvider {
    private var store: [BeaconContactCard: [BeaconConversationMessage]] = [:]

    init() {
        let c1 = BeaconContactCard(threadId: "1", name: "Alex Rivera", address: "5551234567", role: "Friend", presence: .online, unread: 2, isFavorite: true, isPrivate: false, isTrusted: false)
        let c2 = BeaconContactCard(threadId: "2", name: "Morgan Lee", address: "5559876543", role: "Family", presence: .recent, unread: 0, isFavorite: false, isPrivate: true, isTrusted: true)

        store[c1] = [
            BeaconConversationMessage(id: UUID().uuidString, sender: "Alex", text: "Hey, how are you?", timestamp: Date().addingTimeInterval(-3600), isIncoming: true, isUrgent: false),
            BeaconConversationMessage(id: UUID().uuidString, sender: "You", text: "I'm good!", timestamp: Date().addingTimeInterval(-3500), isIncoming: false, isUrgent: false)
        ]
        store[c2] = []
    }

    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] {
        store
    }

    func listenToConversations(onChange: @escaping ([BeaconContactCard]) -> Void) -> ListenerRegistration? {
        onChange(Array(store.keys))
        return MockListener()
    }

    func listenToMessages(for contact: BeaconContactCard, onChange: @escaping ([BeaconConversationMessage]) -> Void) -> ListenerRegistration? {
        onChange(store[contact] ?? [])
        return MockListener()
    }

    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {
        var arr = store[contact] ?? []
        arr.append(message)
        store[contact] = arr
    }
}

final class FirestoreBeaconConversationProvider: BeaconConversationProvider {
    #if canImport(FirebaseFirestore)
    private let db = Firestore.firestore()
    private let userId: String

    init(userId: String) {
        self.userId = userId
    }

    private var threadsCollection: CollectionReference {
        db.collection("users").document(userId).collection("synced_threads")
    }

    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] {
        let threadsSnapshot = try await threadsCollection.getDocuments()

        return try await withThrowingTaskGroup(of: (BeaconContactCard, [BeaconConversationMessage]).self) { group in
            for threadDoc in threadsSnapshot.documents {
                group.addTask {
                    let data = threadDoc.data()
                    let address = data["address"] as? String ?? "Unknown"
                    let name = data["display_name"] as? String ?? address
                    let contact = BeaconContactCard(
                        threadId: threadDoc.documentID,
                        name: name,
                        address: address,
                        role: "Contact",
                        presence: .offline,
                        unread: data["unread"] as? Int ?? 0,
                        isFavorite: data["isFavorite"] as? Bool ?? false,
                        isPrivate: data["isPrivate"] as? Bool ?? false,
                        isTrusted: data["isTrusted"] as? Bool ?? false
                    )

                    let messagesSnapshot = try await threadDoc.reference.collection("messages")
                        .order(by: "date", descending: false)
                        .limit(to: 50)
                        .getDocuments()

                    var messages: [BeaconConversationMessage] = []
                    for msgDoc in messagesSnapshot.documents {
                        let msgData = msgDoc.data()
                        let type = msgData["type"] as? Int ?? 1
                        let timestamp = msgData["date"] as? Int64 ?? 0
                        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)

                        let msg = BeaconConversationMessage(
                            id: msgDoc.documentID,
                            sender: type == 1 ? address : "You",
                            text: msgData["body"] as? String ?? "",
                            timestamp: date,
                            isIncoming: type == 1,
                            isUrgent: false
                        )
                        messages.append(msg)
                    }
                    return (contact, messages)
                }
            }

            var result: [BeaconContactCard: [BeaconConversationMessage]] = [:]
            for try await (contact, messages) in group {
                result[contact] = messages
            }
            return result
        }
    }

    func listenToConversations(onChange: @escaping ([BeaconContactCard]) -> Void) -> ListenerRegistration? {
        return threadsCollection.addSnapshotListener { snapshot, error in
            guard let documents = snapshot?.documents else { return }

            let contacts = documents.compactMap { doc -> BeaconContactCard? in
                let data = doc.data()
                let address = data["address"] as? String ?? "Unknown"
                let name = data["display_name"] as? String ?? address

                return BeaconContactCard(
                    threadId: doc.documentID,
                    name: name,
                    address: address,
                    role: "Contact",
                    presence: .offline,
                    unread: data["unread"] as? Int ?? 0,
                    isFavorite: data["isFavorite"] as? Bool ?? false,
                    isPrivate: data["isPrivate"] as? Bool ?? false,
                    isTrusted: data["isTrusted"] as? Bool ?? false
                )
            }
            onChange(contacts)
        }
    }

    func listenToMessages(for contact: BeaconContactCard, onChange: @escaping ([BeaconConversationMessage]) -> Void) -> ListenerRegistration? {
        let threadRef = threadsCollection.document(contact.threadId)
        return threadRef.collection("messages")
            .order(by: "date", descending: false)
            .limit(to: 50)
            .addSnapshotListener { snapshot, error in
                guard let documents = snapshot?.documents else { return }

                let messages = documents.compactMap { doc -> BeaconConversationMessage? in
                    let data = doc.data()
                    let type = data["type"] as? Int ?? 1
                    let timestamp = data["date"] as? Int64 ?? 0
                    let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)

                    return BeaconConversationMessage(
                        id: doc.documentID,
                        sender: type == 1 ? contact.address : "You",
                        text: data["body"] as? String ?? "",
                        timestamp: date,
                        isIncoming: type == 1,
                        isUrgent: false
                    )
                }
                onChange(messages)
            }
    }

    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {
        let docData: [String: Any] = [
            "address": contact.address,
            "body": message.text,
            "date": Int64(message.timestamp.timeIntervalSince1970 * 1000),
            "sender": "iOS"
        ]

        try await db.collection("users").document(userId)
            .collection("outbox").addDocument(data: docData)
    }
    #else
    init(userId: String) {}
    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] { [:] }
    func listenToConversations(onChange: @escaping ([BeaconContactCard]) -> Void) -> ListenerRegistration? { return nil }
    func listenToMessages(for contact: BeaconContactCard, onChange: @escaping ([BeaconConversationMessage]) -> Void) -> ListenerRegistration? { return nil }
    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {}
    #endif
}
