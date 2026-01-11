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

final class InMemoryConversationProvider: ConversationProvider {
    private var store: [ContactCard: [ConversationMessage]] = [:]

    init(seed: [ContactCard: [ConversationMessage]] = [:]) {
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

    private var legacyContacts: [ContactCard] = []
    private var lineContacts: [ContactCard] = []
    private let queue = DispatchQueue(label: "com.pulselink.firestoreProvider", attributes: .concurrent)

    init(userId: String) {
        self.userId = userId
    }

    private var legacyThreadsCollection: CollectionReference {
        db.collection("users").document(userId).collection("synced_threads")
    }

    private var linesCollection: CollectionReference {
        db.collection("users").document(userId).collection("lines")
    }

    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] {
        return [:] // Using listeners for UI
    }

    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration? {
        let composite = CompositeListener()

        // 1. Listen to Legacy Synced Threads
        let legacyListener = legacyThreadsCollection.addSnapshotListener { [weak self] snapshot, error in
            if let error = error {
                print("Error listening to legacy threads: \(error.localizedDescription)")
                return
            }
            guard let self = self, let documents = snapshot?.documents else { return }
            self.queue.async(flags: .barrier) {
                self.legacyContacts = self.parseContacts(documents, lineId: nil)
                self.mergeAndNotify(onChange: onChange)
            }
        }
        composite.add(legacyListener)

        // 2. Listen to Lines
        // Thread synchronization for currentLineThreadsListener
        var currentLineThreadsListener: ListenerRegistration?
        let listenerQueue = DispatchQueue(label: "com.pulselink.lineListener")

        let linesListener = linesCollection.addSnapshotListener { [weak self] snapshot, error in
            if let error = error {
                print("Error listening to lines: \(error.localizedDescription)")
                return
            }
            guard let self = self else { return }
            let lines = snapshot?.documents ?? []

            listenerQueue.async {
                // If no lines, clear line contacts
                if lines.isEmpty {
                    self.queue.async(flags: .barrier) {
                        self.lineContacts = []
                        self.mergeAndNotify(onChange: onChange)
                    }
                    currentLineThreadsListener?.remove()
                    currentLineThreadsListener = nil
                    return
                }

                let firstLineId = lines[0].documentID
                currentLineThreadsListener?.remove()

                let threadsRef = self.linesCollection.document(firstLineId).collection("threads")
                currentLineThreadsListener = threadsRef.addSnapshotListener { [weak self] threadSnap, error in
                    if let error = error {
                        print("Error listening to line threads: \(error.localizedDescription)")
                        return
                    }
                    guard let self = self, let threadDocs = threadSnap?.documents else { return }
                    self.queue.async(flags: .barrier) {
                        self.lineContacts = self.parseContacts(threadDocs, lineId: firstLineId)
                        self.mergeAndNotify(onChange: onChange)
                    }
                }
            }
        }
        composite.add(linesListener)

        return WrapperListener(composite: composite) {
            listenerQueue.sync {
                currentLineThreadsListener?.remove()
            }
        }
    }

    private func parseContacts(_ documents: [QueryDocumentSnapshot], lineId: String?) -> [ContactCard] {
        return documents.compactMap { doc -> ContactCard? in
            let data = doc.data()
            let address = data["address"] as? String ?? "Unknown"
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
        // Must be called inside barrier block
        var seen = Set<String>()
        var result: [ContactCard] = []

        for c in lineContacts {
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

        DispatchQueue.main.async {
            onChange(result)
        }
    }

    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration? {
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
                if let error = error {
                    print("Error listening to messages: \(error.localizedDescription)")
                    return
                }
                guard let documents = snapshot?.documents else { return }

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
        guard !contact.address.isEmpty, contact.address != "Unknown" else {
            throw NSError(domain: "PulseLink", code: 400, userInfo: [NSLocalizedDescriptionKey: "Invalid contact address"])
        }
        guard !message.text.isEmpty else {
            throw NSError(domain: "PulseLink", code: 400, userInfo: [NSLocalizedDescriptionKey: "Message body cannot be empty"])
        }

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
    #else
    init(userId: String) {}
    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] { [:] }
    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration? { return nil }
    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration? { return nil }
    func send(message: ConversationMessage, to contact: ContactCard) async throws {}
    #endif
}
