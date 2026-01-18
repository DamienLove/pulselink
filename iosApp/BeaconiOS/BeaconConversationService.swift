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
    func requestSync() async throws
}

#if canImport(FirebaseFirestore)
// ListenerRegistration is available
class CompositeListener: ListenerRegistration {
    private var listeners: [ListenerRegistration] = []

    func add(_ listener: ListenerRegistration) {
        listeners.append(listener)
    }

    func remove() {
        listeners.forEach { $0.remove() }
        listeners.removeAll()
    }
}
class WrapperListener: ListenerRegistration {
    let composite: ListenerRegistration
    let cleanup: () -> Void

    init(composite: ListenerRegistration, cleanup: @escaping () -> Void) {
        self.composite = composite
        self.cleanup = cleanup
    }

    func remove() {
        composite.remove()
        cleanup()
    }
}
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
            BeaconConversationMessage(sender: "Alex", text: "Hey, how are you?", timestamp: Date().addingTimeInterval(-3600), isIncoming: true, isUrgent: false),
            BeaconConversationMessage(sender: "You", text: "I'm good!", timestamp: Date().addingTimeInterval(-3500), isIncoming: false, isUrgent: false)
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

    func requestSync() async throws {}
}

final class FirestoreBeaconConversationProvider: BeaconConversationProvider {
    #if canImport(FirebaseFirestore)
    private let db = Firestore.firestore()
    private let userId: String

    private var legacyContacts: [BeaconContactCard] = []
    // Map lineId to list of contacts
    private var lineContacts: [String: [BeaconContactCard]] = [:]

    init(userId: String) {
        self.userId = userId
    }

    private var legacyThreadsCollection: CollectionReference {
        db.collection("users").document(userId).collection("synced_threads")
    }

    private var linesCollection: CollectionReference {
        db.collection("users").document(userId).collection("lines")
    }

    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] {
        return [:] // Using listeners for UI
    }

    func listenToConversations(onChange: @escaping ([BeaconContactCard]) -> Void) -> ListenerRegistration? {
        let composite = CompositeListener()

        // 1. Legacy
        let legacyListener = legacyThreadsCollection.addSnapshotListener { [weak self] snapshot, _ in
            guard let self = self, let documents = snapshot?.documents else { return }
            self.legacyContacts = self.parseContacts(documents, lineId: nil)
            self.mergeAndNotify(onChange: onChange)
        }
        composite.add(legacyListener)

        // 2. Lines collection to discover active devices/lines
        var lineListeners: [String: ListenerRegistration] = [:]

        // Listen to active lines
        let linesListener = linesCollection.addSnapshotListener { [weak self] snapshot, _ in
            guard let self = self, let lines = snapshot?.documents else { return }

            let currentLineIds = Set(lines.map { $0.documentID })

            // Cleanup removed lines
            for (lineId, listener) in lineListeners {
                if !currentLineIds.contains(lineId) {
                    listener.remove()
                    lineListeners.removeValue(forKey: lineId)
                    self.lineContacts.removeValue(forKey: lineId)
                }
            }

            // Add new line listeners
            for lineDoc in lines {
                let lineId = lineDoc.documentID
                if lineListeners[lineId] == nil {
                    let threadsRef = self.linesCollection.document(lineId).collection("threads")
                    let listener = threadsRef.addSnapshotListener { [weak self] threadSnap, _ in
                        guard let self = self, let threadDocs = threadSnap?.documents else { return }
                        self.lineContacts[lineId] = self.parseContacts(threadDocs, lineId: lineId)
                        self.mergeAndNotify(onChange: onChange)
                    }
                    lineListeners[lineId] = listener
                }
            }

            self.mergeAndNotify(onChange: onChange)
        }
        composite.add(linesListener)

        return WrapperListener(composite: composite) {
            lineListeners.values.forEach { $0.remove() }
            lineListeners.removeAll()
        }
    }

    private func parseContacts(_ documents: [QueryDocumentSnapshot], lineId: String?) -> [BeaconContactCard] {
        return documents.compactMap { doc -> BeaconContactCard? in
            let data = doc.data()
            let address = data["address"] as? String ?? "Unknown"
            let name = data["display_name"] as? String ?? address

            return BeaconContactCard(
                threadId: doc.documentID,
                lineId: lineId,
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
    }

    private func mergeAndNotify(onChange: @escaping ([BeaconContactCard]) -> Void) {
        var seen = Set<String>()
        var result: [BeaconContactCard] = []

        // Flatten all line contacts
        let allLineContacts = lineContacts.values.flatMap { $0 }

        for c in allLineContacts {
            let key = c.address
            if !seen.contains(key) {
                seen.insert(key)
                result.append(c)
            }
        }
        for c in legacyContacts {
            let key = c.address
            if !seen.contains(key) {
                seen.insert(key)
                result.append(c)
            }
        }
        onChange(result)
    }

    func listenToMessages(for contact: BeaconContactCard, onChange: @escaping ([BeaconConversationMessage]) -> Void) -> ListenerRegistration? {
        let collectionRef: CollectionReference
        if let lineId = contact.lineId {
            collectionRef = linesCollection.document(lineId).collection("threads").document(contact.threadId).collection("messages")
        } else {
            collectionRef = legacyThreadsCollection.document(contact.threadId).collection("messages")
        }

        return collectionRef
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
        var docData: [String: Any] = [
            "address": contact.address,
            "body": message.text,
            "date": Int64(message.timestamp.timeIntervalSince1970 * 1000),
            "sender": "iOS"
        ]

        if let lineId = contact.lineId {
            docData["lineId"] = lineId
        }

        try await db.collection("users").document(userId)
            .collection("outbox").addDocument(data: docData)
    }

    func requestSync() async throws {
        let data: [String: Any] = [
            "syncRequestedAt": Int64(Date().timeIntervalSince1970 * 1000),
            "remoteWebAccessEnabled": true
        ]
        try await db.collection("users").document(userId).setData(data, merge: true)
    }
    #else
    init(userId: String) {}
    func loadConversations() async throws -> [BeaconContactCard: [BeaconConversationMessage]] { [:] }
    func listenToConversations(onChange: @escaping ([BeaconContactCard]) -> Void) -> ListenerRegistration? { return nil }
    func listenToMessages(for contact: BeaconContactCard, onChange: @escaping ([BeaconConversationMessage]) -> Void) -> ListenerRegistration? { return nil }
    func send(message: BeaconConversationMessage, to contact: BeaconContactCard) async throws {}
    func requestSync() async throws {}
    #endif
}
