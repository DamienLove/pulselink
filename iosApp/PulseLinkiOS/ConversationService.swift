import Foundation

#if canImport(FirebaseFirestore)
import FirebaseFirestore
import FirebaseAuth
#endif

protocol ConversationProvider {
    // Legacy load
    func loadConversations() async throws -> [ContactCard: [ConversationMessage]]
    // Realtime listeners
    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration?
    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration?
    func send(message: ConversationMessage, to contact: ContactCard) async throws
}

// Wrapper for ListenerRegistration to be platform-agnostic if needed,
// but since we import FirebaseFirestore, we can use it directly or return an opaque object.
// For simplicity in this environment, we use 'Any' or just return the Firebase object if available,
// or a dummy closure wrapper.
#if canImport(FirebaseFirestore)
// ListenerRegistration is provided by FirebaseFirestore
// MockListener conforms to Firebase's ListenerRegistration for InMemoryConversationProvider
// ListenerRegistration is available from FirebaseFirestore
class MockListener: ListenerRegistration {
    func remove() {}
}
#else
protocol ListenerRegistration {
    func remove()
}
class MockListener: ListenerRegistration {
    func remove() {}
}
#endif

final class InMemoryConversationProvider: ConversationProvider {
    private var store: [ContactCard: [ConversationMessage]] = [:]

    init(seed: [ContactCard: [ConversationMessage]] = [:]) {
        // If seed is empty, provide some mock data compatible with new fields
        if seed.isEmpty {
            let c1 = ContactCard(threadId: "1", name: "Alex Rivera", address: "5551234567", role: "Friend", presence: .online, unread: 2, isFavorite: true, isPrivate: false, isTrusted: false)
            let c2 = ContactCard(threadId: "2", name: "Morgan Lee", address: "5559876543", role: "Family", presence: .recent, unread: 0, isFavorite: false, isPrivate: true, isTrusted: true)
            store = [
                c1: [],
                c2: []
            ]
        } else {
            store = seed
        }
    }

    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] {
        store
    }

    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration? {
        // Return initial data
        onChange(Array(store.keys))
        return MockListener()
    }

    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration? {
        onChange(store[contact] ?? [])
        return MockListener()
    }

    func send(message: ConversationMessage, to contact: ContactCard) async throws {
        var arr = store[contact] ?? []
        arr.append(message)
        store[contact] = arr
    }
}

final class FirestoreConversationProvider: ConversationProvider {
    #if canImport(FirebaseFirestore)
    private let db = Firestore.firestore()
    private let userId: String

    init(userId: String) {
        self.userId = userId
    }

    // Android Path: users/{uid}/synced_threads
    private var threadsCollection: CollectionReference {
        db.collection("users").document(userId).collection("synced_threads")
    }

    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] {
        // 1. Fetch threads
        let threadsSnapshot = try await threadsCollection.getDocuments()

        // 2. Fetch messages in parallel using TaskGroup
        return try await withThrowingTaskGroup(of: (ContactCard, [ConversationMessage]).self) { group in
            for threadDoc in threadsSnapshot.documents {
                group.addTask {
                    let data = threadDoc.data()
                    let address = data["address"] as? String ?? "Unknown"
                    // Use display name if available, otherwise fall back to address
                    let name = data["display_name"] as? String ?? address
                    let contact = ContactCard(
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

                    var messages: [ConversationMessage] = []
                    for msgDoc in messagesSnapshot.documents {
                        let msgData = msgDoc.data()
                        let type = msgData["type"] as? Int ?? 1 // 1=in, 2=out
                        let timestamp = msgData["date"] as? Int64 ?? 0
                        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)

                        let msg = ConversationMessage(
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

            var result: [ContactCard: [ConversationMessage]] = [:]
            for try await (contact, messages) in group {
                result[contact] = messages
            }
            return result
        }
    }

    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration? {
        return threadsCollection.addSnapshotListener { snapshot, error in
            guard let documents = snapshot?.documents else {
                print("Error listening to threads: \(error?.localizedDescription ?? "Unknown")")
                return
            }

            let contacts = documents.compactMap { doc -> ContactCard? in
                let data = doc.data()
                let address = data["address"] as? String ?? "Unknown"
                let name = data["display_name"] as? String ?? address

                return ContactCard(
                    threadId: doc.documentID,
                    name: name,
                    address: address,
                    role: "Contact",
                    presence: .offline, // Presence logic requires separate listeners or logic
                    unread: data["unread"] as? Int ?? 0,
                    isFavorite: data["isFavorite"] as? Bool ?? false,
                    isPrivate: data["isPrivate"] as? Bool ?? false,
                    isTrusted: data["isTrusted"] as? Bool ?? false
                )
            }
            onChange(contacts)
        }
    }

    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration? {
        let threadRef = threadsCollection.document(contact.threadId)
        return threadRef.collection("messages")
            .order(by: "date", descending: false)
            .limit(to: 50)
            .addSnapshotListener { snapshot, error in
                guard let documents = snapshot?.documents else {
                    print("Error listening to messages: \(error?.localizedDescription ?? "Unknown")")
                    return
                }

                let messages = documents.compactMap { doc -> ConversationMessage? in
                    let data = doc.data()
                    let type = data["type"] as? Int ?? 1
                    let timestamp = data["date"] as? Int64 ?? 0
                    let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)

                    return ConversationMessage(
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

    func send(message: ConversationMessage, to contact: ContactCard) async throws {
        // Use contact.address (phone number) instead of contact.name (display name)
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
    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] { [:] }
    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration? { return nil }
    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration? { return nil }
    func send(message: ConversationMessage, to contact: ContactCard) async throws {}
    #endif
}
