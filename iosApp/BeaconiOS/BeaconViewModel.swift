import Foundation
import Shared
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif
#if canImport(FirebaseFunctions)
import FirebaseFunctions
#endif

@MainActor
final class BeaconViewModel: ObservableObject {
    @Published var contacts: [BeaconContactCard] = []
    @Published private(set) var conversations: [UUID: [BeaconConversationMessage]] = [:]
    @Published var statusText: String? = nil

    @Published var isLoggedIn: Bool = false
    private var provider: BeaconConversationProvider
    private var threadsListener: ListenerRegistration?
    private var activeMessageListener: ListenerRegistration?

    init() {
        // Initial provider setup
        provider = MockBeaconConversationProvider()

        setupAuthListener()
    }

    deinit {
        threadsListener?.remove()
        activeMessageListener?.remove()
    }

    private func setupAuthListener() {
        #if canImport(FirebaseAuth)
        if FirebaseBootstrap.isConfigured {
            Auth.auth().addStateDidChangeListener { [weak self] _, user in
                Task { @MainActor [weak self] in
                    guard let self = self else { return }
                    if let uid = user?.uid {
                        self.isLoggedIn = true
                        Task { await DeviceManager.shared.registerDevice(userId: uid) }
                        self.provider = FirestoreBeaconConversationProvider(userId: uid)
                        self.startListeningToThreads()
                        await BeaconDeviceManager.shared.registerDevice()
                    } else {
                        self.isLoggedIn = false
                        self.threadsListener?.remove()
                        self.activeMessageListener?.remove()
                        self.provider = MockBeaconConversationProvider()
                        self.contacts = []
                        self.conversations = [:]
                    }
                }
            }
        }
        #else
        self.isLoggedIn = true
        startListeningToThreads()
        #endif
    }

    private func startListeningToThreads() {
        threadsListener?.remove()
        threadsListener = provider.listenToConversations { [weak self] newContacts in
            DispatchQueue.main.async {
                self?.contacts = newContacts.sorted(by: { $0.unread > $1.unread })
            }
        }
    }

    func messages(for contact: BeaconContactCard) -> [BeaconConversationMessage] {
        conversations[contact.id] ?? []
    }

    // MARK: - Filters

    var inboxContacts: [BeaconContactCard] {
        contacts.filter { !$0.isPrivate }
    }

    var trustedContacts: [BeaconContactCard] {
        contacts.filter { $0.isTrusted && !$0.isPrivate }
    }

    var favoriteContacts: [BeaconContactCard] {
        contacts.filter { $0.isFavorite && !$0.isPrivate }
    }

    var privateContacts: [BeaconContactCard] {
        contacts.filter { $0.isPrivate }
    }

    func sendMessage(to contact: BeaconContactCard, text: String) {
        let message = BeaconConversationMessage(
            sender: "You",
            text: text,
            timestamp: Date(),
            isIncoming: false,
            isUrgent: false
        )

        Task {
            do {
                try await provider.send(message: message, to: contact)
                // Only optimistic update if send didn't throw
                await MainActor.run {
                    if activeMessageListener == nil {
                        var msgs = conversations[contact.id] ?? []
                        msgs.append(message)
                        conversations[contact.id] = msgs
                    }
                    statusText = nil
                }
            } catch {
                await MainActor.run {
                    // For now we just log, or bind this to a UI alert if ContentView supports it
                    statusText = "Error sending: \(error.localizedDescription)"
                    print("Error sending message: \(error)")
                }
            }
        }
    }

    func startListeningToConversation(contact: BeaconContactCard) {
        activeMessageListener?.remove()
        activeMessageListener = provider.listenToMessages(for: contact) { [weak self] messages in
            DispatchQueue.main.async {
                self?.conversations[contact.id] = messages
            }
        }
    }

    func stopListeningToConversation() {
        activeMessageListener?.remove()
        activeMessageListener = nil
    }

    func deleteAccount() async throws {
        #if canImport(FirebaseAuth) && canImport(FirebaseFunctions)
        // Use Firebase Functions SDK for better handling of auth and regions
        let functions = Functions.functions()
        _ = try await functions.httpsCallable("deleteAccount").call()
        try? Auth.auth().signOut()
        #elseif canImport(FirebaseAuth)
        // Fallback if Functions SDK is missing but Auth exists (unlikely in this setup but safe)
        guard let user = Auth.auth().currentUser else { return }
        try? await user.delete() // Simple auth deletion if cloud function not reachable via SDK
        #else
        // Mock success
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        #endif
    }
}
