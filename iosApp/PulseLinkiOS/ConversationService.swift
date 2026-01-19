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
    func requestSync() async throws
}

#if canImport(FirebaseFirestore)
// ListenerRegistration is available from FirebaseFirestore
// CompositeListener helps manage multiple listeners (legacy + lines)
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
class CompositeListener: ListenerRegistration {
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

    func requestSync() async throws {}
}

final class FirestoreConversationProvider: ConversationProvider {
    #if canImport(FirebaseFirestore)
    private let db = Firestore.firestore()
    private let userId: String

    // Internal state to hold merged contacts from legacy and lines
    private var legacyContacts: [ContactCard] = []
    // Map lineId to list of contacts
    private var lineContacts: [String: [ContactCard]] = [:]

    init(userId: String) {
        self.userId = userId
    }

    // Android Path: users/{uid}/synced_threads
    private var legacyThreadsCollection: CollectionReference {
        db.collection("users").document(userId).collection("synced_threads")
    }

    private var linesCollection: CollectionReference {
        db.collection("users").document(userId).collection("lines")
    }

    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] {
        return [:] // Not primarily used in realtime UI flow
    }

    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration? {
        let composite = CompositeListener()

        // 1. Listen to Legacy Synced Threads
        let legacyListener = legacyThreadsCollection.addSnapshotListener { [weak self] snapshot, _ in
            guard let self = self, let documents = snapshot?.documents else { return }
            self.legacyContacts = self.parseContacts(documents, lineId: nil)
            self.mergeAndNotify(onChange: onChange)
        }
        composite.add(legacyListener)

        // 2. Listen to Lines collection to discover active devices/lines
        // We maintain a map of listeners for each discovered line.
        var lineListeners: [String: ListenerRegistration] = [:]

        // This listener watches for changes in the 'lines' collection (added/removed lines)
        let linesListener = linesCollection.addSnapshotListener { [weak self] snapshot, _ in
            guard let self = self, let lines = snapshot?.documents else { return }

            let currentLineIds = Set(lines.map { $0.documentID })

            // Remove listeners for deleted lines
            for (lineId, listener) in lineListeners {
                if !currentLineIds.contains(lineId) {
                    listener.remove()
                    lineListeners.removeValue(forKey: lineId)
                    self.lineContacts.removeValue(forKey: lineId)
                }
            }

            // Add listeners for new lines
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

            // If we have lines but no contacts yet (e.g. empty lines), trigger update
            // (or if all lines were removed)
            self.mergeAndNotify(onChange: onChange)
        }
        composite.add(linesListener)

        // When the composite listener is removed (e.g. user logs out), we must also remove all the dynamic line listeners.
        let wrapper = WrapperListener(composite: composite) {
            lineListeners.values.forEach { $0.remove() }
            lineListeners.removeAll()
        }
        return wrapper
    }

    private func parseContacts(_ documents: [QueryDocumentSnapshot], lineId: String?) -> [ContactCard] {
        return documents.compactMap { doc -> ContactCard? in
            let data = doc.data()
            let address = data["address"] as? String ?? "Unknown"
            // Android SmaSyncWorker populates display_name
            let name = data["display_name"] as? String ?? address

            return ContactCard(
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

    private func mergeAndNotify(onChange: @escaping ([ContactCard]) -> Void) {
        // Merge legacy and line contacts.
        var seen = Set<String>()
        var result: [ContactCard] = []

        // Flatten all line contacts
        let allLineContacts = lineContacts.values.flatMap { $0 }

        // Prioritize Line contacts
        for c in allLineContacts {
            let key = c.address // Use address to dedup logical conversations
            if !seen.contains(key) {
                seen.insert(key)
                result.append(c)
            }
        }

        // Add Legacy contacts if not seen
        for c in legacyContacts {
            let key = c.address
            if !seen.contains(key) {
                seen.insert(key)
                result.append(c)
            }
        }

        onChange(result)
    }

    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration? {
        // Determine path based on lineId
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
        // Include lineId in outbox if available
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
    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] { [:] }
    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration? { return nil }
    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration? { return nil }
    func send(message: ConversationMessage, to contact: ContactCard) async throws {}
    func requestSync() async throws {}
    #endif
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
