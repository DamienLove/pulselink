import Foundation
import Shared
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif
#if canImport(FirebaseFunctions)
import FirebaseFunctions
#endif

enum Presence: String {
    case online, recent, offline

    var label: String {
        switch self {
        case .online: return "Online"
        case .recent: return "Recent"
        case .offline: return "Offline"
        }
    }
}

struct ContactCard: Identifiable, Hashable {
    let id = UUID()
    let threadId: String
    var lineId: String? = nil
    let name: String
    let address: String // Phone number or unique ID
    let role: String
    let presence: Presence
    var unread: Int
    var isFavorite: Bool
    var isPrivate: Bool
    var isTrusted: Bool
}

struct ConversationMessage: Identifiable {
    let id = UUID()
    let sender: String
    let text: String
    let timestamp: Date
    let isIncoming: Bool
    let isUrgent: Bool
}

enum EmergencyState: Equatable {
    case idle
    case arming(Int)   // countdown seconds
    case active
}

@MainActor
final class AlertRelayViewModel: ObservableObject {
    @Published var statusText: String = "Idle"
    @Published var emergencyState: EmergencyState = .idle
    @Published var contacts: [ContactCard] = []
    @Published private(set) var conversations: [UUID: [ConversationMessage]] = [:]
    @Published var overrideDND: Bool = true
    @Published var maxVolumeOnUrgent: Bool = true
    @Published var baseUrl: String

    var trustedContacts: [ContactCard] {
        contacts.filter { $0.isTrusted && !$0.isPrivate }
    }

    private let client: AlertRelayClient
    // conversationProvider needs to be var to be updated. (Removed duplicate 'let' declaration that was here)
    private var conversationProvider: ConversationProvider
    private var armingTask: Task<Void, Never>?
    private let pinCode = "1234" // demo PIN; replace with secure storage
    private let emergencyLocationService = EmergencyLocationService()

    // Listeners
    private var threadsListener: ListenerRegistration?
    private var activeMessageListener: ListenerRegistration?

    @Published var isLoggedIn: Bool = false

    init(baseUrl: String = AlertRelay.shared.DEFAULT_BASE_URL) {
        self.baseUrl = baseUrl
        client = AlertRelayFactory.shared.build(baseUrl: baseUrl)

        // Initial provider setup (placeholder)
        conversationProvider = InMemoryConversationProvider()

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
                        self.conversationProvider = FirestoreConversationProvider(userId: uid)
                        self.startListeningToThreads()
                        Task {
                            await DeviceManager.shared.registerDevice()
                            try? await self.conversationProvider.requestSync()
                        }
                    } else {
                        self.isLoggedIn = false
                        self.threadsListener?.remove()
                        self.activeMessageListener?.remove()
                        self.conversationProvider = InMemoryConversationProvider()
                        self.contacts = []
                        self.conversations = [:]
                    }
                }
            }
        }
        #else
        // In preview/mock mode, simulate login or stay logged out
        self.isLoggedIn = true // Assume logged in for previews if desired, or false to test login
        startListeningToThreads()
        #endif
    }

    // MARK: - Relay

    func sendTestAlert() async {
        statusText = "Sending…"
        do {
            let recipient = AlertRecipient(
                phoneNumber: nil,
                pushToken: nil,
                email: nil
            )
            let request = AlertRequest(
                message: "Test alert from iOS shell",
                severity: AlertSeverity.emergency,
                recipients: [recipient],
                senderId: nil,
                location: nil,
                metadata: [:]
            )

            let response = try await client.sendAlert(request: request)
            statusText = "Status: \(response.status) id=\(response.relayId ?? "n/a")"
        } catch {
            statusText = "Failed: \(error.localizedDescription)"
        }
    }

    // MARK: - Emergency flow

    func startEmergencyCountdown(seconds: Int = 5) {
        guard emergencyState == .idle else { return }
        emergencyState = .arming(seconds)
        armingTask?.cancel()
        armingTask = Task { [weak self] in
            var remaining = seconds
            while remaining > 0 && !(Task.isCancelled) {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                remaining -= 1
                await MainActor.run {
                    self?.emergencyState = .arming(remaining)
                }
            }
            guard !Task.isCancelled else { return }
            await MainActor.run {
                self?.emergencyState = .active
                self?.statusText = "Emergency active"
            }
            await self?.recordEmergencyLocation()
        }
    }

    func cancelEmergency(withPin pin: String) -> Bool {
        guard emergencyState != .idle else { return true }
        guard pin == pinCode else { return false }
        armingTask?.cancel()
        emergencyState = .idle
        statusText = "Emergency canceled"
        return true
    }

    func cancelEmergencyBypassPin() {
        armingTask?.cancel()
        emergencyState = .idle
        statusText = "Emergency canceled"
    }

    private func recordEmergencyLocation() async {
        let success = await emergencyLocationService.recordEmergencyLocation(message: "Emergency triggered from iOS")
        if !success {
            await MainActor.run {
                self.statusText = "Emergency active (location unavailable)"
            }
        }
    }

    // MARK: - Conversations

    func messages(for contact: ContactCard) -> [ConversationMessage] {
        conversations[contact.id] ?? []
    }

    func sendMessage(to contact: ContactCard, text: String, urgent: Bool) {
        let message = ConversationMessage(
            sender: "You",
            text: text,
            timestamp: Date(),
            isIncoming: false,
            isUrgent: urgent
        )
        Task {
            do {
                try await conversationProvider.send(message: message, to: contact)
                // UI update handled by listener if active, or optimistically here?
                // Optimistic update:
                await MainActor.run {
                    // Only append if we aren't listening (to avoid duplicates if listener is fast)
                    // But listener handles deduping usually by replacing the list.
                    // For now, let's rely on listener if active.
                    if activeMessageListener == nil {
                         var msgs = conversations[contact.id] ?? []
                         msgs.append(message)
                         conversations[contact.id] = msgs
                    }

                    statusText = urgent ? "Urgent message sent (ringer boost, DND override)" : "Message sent"
                    if urgent {
                        NotificationManager.shared.playAlertToneIfNeeded(volumeBoost: maxVolumeOnUrgent)
                        NotificationManager.shared.postCriticalLocal(
                            title: "Urgent to \(contact.name)",
                            body: text,
                            volumeBoost: maxVolumeOnUrgent
                        )
                    }
                }
            } catch {
                await MainActor.run {
                    statusText = "Send failed: \(error.localizedDescription)"
                }
            }
        }
    }

    func startListeningToConversation(contact: ContactCard) {
        activeMessageListener?.remove()
        activeMessageListener = conversationProvider.listenToMessages(for: contact) { [weak self] messages in
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
        let functions = Functions.functions()
        _ = try await functions.httpsCallable("deleteAccount").call()
        try? Auth.auth().signOut()
        #elseif canImport(FirebaseAuth)
        guard let user = Auth.auth().currentUser else { return }
        try? await user.delete()
        #else
        // Mock success
        try? await Task.sleep(nanoseconds: 1_000_000_000)
        #endif
    }

    func refresh() {
        Task {
            try? await conversationProvider.requestSync()
        }
    }

    private func startListeningToThreads() {
        threadsListener?.remove()
        threadsListener = conversationProvider.listenToConversations { [weak self] newContacts in
            DispatchQueue.main.async {
                self?.contacts = newContacts.sorted(by: { $0.unread > $1.unread })
                // We don't clear conversations map here to preserve cached messages if any
            }
        }
    }
}
