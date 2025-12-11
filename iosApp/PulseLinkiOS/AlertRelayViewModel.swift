import Foundation
import Shared
#if canImport(FirebaseAuth)
import FirebaseAuth
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
    let name: String
    let role: String
    let presence: Presence
    var unread: Int
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
    @Published var triggerPin: String {
        didSet { UserDefaults.standard.set(triggerPin, forKey: Self.pinDefaultsKey) }
    }

    private let client: AlertRelayClient
    private let conversationProvider: ConversationProvider
    private var armingTask: Task<Void, Never>?
    private static let pinDefaultsKey = "pulselink.triggerPin"

    init(baseUrl: String = AlertRelay.shared.DEFAULT_BASE_URL) {
        self.baseUrl = baseUrl
        client = AlertRelayFactory.shared.build(baseUrl: baseUrl)
        if FirebaseBootstrap.isConfigured {
            FirebaseBootstrap.ensureAnonymousAuth()
            #if canImport(FirebaseAuth)
            if let uid = Auth.auth().currentUser?.uid {
                conversationProvider = FirestoreConversationProvider(userId: uid)
            } else {
                conversationProvider = InMemoryConversationProvider()
            }
            #else
            conversationProvider = InMemoryConversationProvider()
            #endif
        } else {
            conversationProvider = InMemoryConversationProvider()
        }
        triggerPin = UserDefaults.standard.string(forKey: Self.pinDefaultsKey) ?? "1234"
        seedSampleData()
        Task { await loadConversations() }
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
        }
    }

    func cancelEmergency(withPin pin: String) -> Bool {
        guard emergencyState != .idle else { return true }
        guard pin == triggerPin else { return false }
        armingTask?.cancel()
        emergencyState = .idle
        statusText = "Emergency canceled"
        return true
    }

    func updateTriggerPin(_ newPin: String) {
        let trimmed = newPin.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        triggerPin = trimmed
    }

    func cancelEmergencyBypassPin() {
        armingTask?.cancel()
        emergencyState = .idle
        statusText = "Emergency canceled"
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
            try? await conversationProvider.send(message: message, to: contact)
            await MainActor.run {
                var msgs = conversations[contact.id] ?? []
                msgs.append(message)
                conversations[contact.id] = msgs
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
        }
    }

    private func seedSampleData() {
        let c1 = ContactCard(name: "Alex Rivera", role: "Emergency", presence: .online, unread: 2)
        let c2 = ContactCard(name: "Morgan Lee", role: "Check-in", presence: .recent, unread: 0)
        let c3 = ContactCard(name: "Jamie Patel", role: "Backup", presence: .offline, unread: 0)
        contacts = [c1, c2, c3]

        conversations[c1.id] = [
            ConversationMessage(sender: "Alex", text: "Are you safe?", timestamp: Date().addingTimeInterval(-3600), isIncoming: true, isUrgent: true),
            ConversationMessage(sender: "You", text: "Yes, testing alert.", timestamp: Date().addingTimeInterval(-3500), isIncoming: false, isUrgent: false)
        ]
        conversations[c2.id] = [
            ConversationMessage(sender: "Morgan", text: "Check-in please", timestamp: Date().addingTimeInterval(-1800), isIncoming: true, isUrgent: false)
        ]
    }

    private func loadConversations() async {
        if let fetched = try? await conversationProvider.loadConversations() {
            await MainActor.run {
                for (contact, msgs) in fetched {
                    conversations[contact.id] = msgs
                    if !contacts.contains(where: { $0.id == contact.id }) {
                        contacts.append(contact)
                    }
                }
            }
        }
    }
}
