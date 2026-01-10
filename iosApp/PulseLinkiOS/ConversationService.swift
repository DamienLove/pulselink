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
}

final class FirestoreConversationProvider: ConversationProvider {
    #if canImport(FirebaseFirestore)
    private let db = Firestore.firestore()
    private let userId: String

    // Internal state to hold merged contacts from legacy and lines
    private var legacyContacts: [ContactCard] = []
    private var lineContacts: [ContactCard] = []
    // To simplify, we only listen to the FIRST active line found to match "Bare min" functionality.
    // Full multi-line support would require a more complex listener management.

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
        // This is a "snapshot" load, distinct from realtime listeners.
        // For simplicity, we just implement legacy load here or basic line load.
        // But since the UI relies on listenToConversations, we can return empty or implement a basic fetch.
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

        // 2. Listen to Lines to find active devices
        // This is a nested listener structure.
        // We listen to "lines" collection. If lines exist, we pick the first one and listen to its threads.
        // Ideally we would manage a map of listeners for all lines.

        // Use a class-level variable to hold the threads listener so we can replace it if lines change.
        // But since we are inside `listenToConversations`, we need to return a single registration.
        // We will attach the threads listener to the composite, but we need to be careful not to leak.
        // Actually, since Swift closures capture context, let's just listen to lines and inside that closure manage the threads listener.

        // We need a way to store the *current* threads listener so we can cancel it if line ID changes.
        // This is tricky with `CompositeListener` if we don't expose removal logic.
        // Let's keep it simple: We listen to `lines`.
        // Note: This implementation assumes a relatively stable single line (active phone).

        var currentLineThreadsListener: ListenerRegistration?

        let linesListener = linesCollection.addSnapshotListener { [weak self] snapshot, _ in
            guard let self = self else { return }
            let lines = snapshot?.documents ?? []

            // If no lines, clear line contacts
            if lines.isEmpty {
                self.lineContacts = []
                self.mergeAndNotify(onChange: onChange)
                currentLineThreadsListener?.remove()
                currentLineThreadsListener = nil
                return
            }

            // Pick the first line (or prioritize 'primaryDeviceId' if we parsed it, but first is fine for now)
            let firstLineId = lines[0].documentID

            // If we are already listening to this line, do nothing
            // (We would need to track currentLineId state, which is hard in this closure scope unless captured)
            // For robustness, we will recreate the listener if we haven't tracked state,
            // but let's just assume we replace it to be safe and simple.

            currentLineThreadsListener?.remove()

            let threadsRef = self.linesCollection.document(firstLineId).collection("threads")
            currentLineThreadsListener = threadsRef.addSnapshotListener { [weak self] threadSnap, _ in
                guard let self = self, let threadDocs = threadSnap?.documents else { return }
                self.lineContacts = self.parseContacts(threadDocs, lineId: firstLineId)
                self.mergeAndNotify(onChange: onChange)
            }
        }
        composite.add(linesListener)

        // We need to make sure currentLineThreadsListener is cleaned up when composite is removed.
        // Since `currentLineThreadsListener` is local to the closure scope of `linesListener` (mostly),
        // we can't easily add it to `composite` externally.
        // However, `linesListener`'s removal stops the callback. But `currentLineThreadsListener` stays alive?
        // Yes, if not removed.
        // We should wrap this logic in a dedicated class or simpler helper, but to stick to this file:
        // We will make `CompositeListener` hold a cleanup closure.

        // Actually, let's just piggyback on `composite.remove` by overriding it?
        // No, `CompositeListener` is a simple array wrapper.
        // Memory leak risk is small if `linesListener` is removed, but the inner listener might persist if not explicitly removed.
        // Correct fix: When `linesListener` fires, it sets up `currentLineThreadsListener`.
        // We need to ensure that if `composite.remove()` is called, `currentLineThreadsListener` is also removed.
        // We can capture a wrapper object.

        // Workaround: Add a specialized cleanup to composite?
        // Since I can't easily change CompositeListener to hold a closure without defining it fully.
        // Let's just define a custom listener object here.

        let wrapper = WrapperListener(composite: composite) {
            currentLineThreadsListener?.remove()
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
        // If duplicates exist (same threadId/address), prioritize line contacts.
        // Keying by 'address' might be safer than 'threadId' across sources, but 'threadId' is what we use for Firestore paths.
        // Actually, legacy threads have threadId from old DB, line threads from new DB. They might collide or differ.
        // We will just show all unique items.

        var seen = Set<String>()
        var result: [ContactCard] = []

        // Prioritize Line contacts
        for c in lineContacts {
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
    #else
    init(userId: String) {}
    func loadConversations() async throws -> [ContactCard: [ConversationMessage]] { [:] }
    func listenToConversations(onChange: @escaping ([ContactCard]) -> Void) -> ListenerRegistration? { return nil }
    func listenToMessages(for contact: ContactCard, onChange: @escaping ([ConversationMessage]) -> Void) -> ListenerRegistration? { return nil }
    func send(message: ConversationMessage, to contact: ContactCard) async throws {}
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
