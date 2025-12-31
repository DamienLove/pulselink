import SwiftUI
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

struct ContentView: View {
    @ObservedObject var viewModel: AlertRelayViewModel
    @State private var showCancelSheet = false
    @State private var pinInput = ""
    @State private var selectedContact: ContactCard?

    var isPro: Bool {
        #if PRO
        return true
        #else
        return false
        #endif
    }

    var body: some View {
        if viewModel.isLoggedIn {
            TabView {
                HomeTab(viewModel: viewModel,
                        showCancelSheet: $showCancelSheet,
                        pinInput: $pinInput,
                        isPro: isPro)
                    .tabItem {
                        Label("Home", systemImage: "shield.lefthalf.filled")
                    }
                    .badge(isPro ? Text("Pro") : nil)

                if isPro {
                    ContactsTab(viewModel: viewModel, selectedContact: $selectedContact)
                        .tabItem {
                            Label("Contacts", systemImage: "person.2.fill")
                        }
                }

                SettingsTab(viewModel: viewModel)
                    .tabItem {
                        Label("Settings", systemImage: "gear")
                    }
            }
            .sheet(isPresented: $showCancelSheet) {
                CancelEmergencySheet(pinInput: $pinInput) { pin in
                    if viewModel.cancelEmergency(withPin: pin) {
                        pinInput = ""
                        showCancelSheet = false
                    }
                }
            }
        } else {
            LoginView {
                // Login success handled by Auth listener in ViewModel
            }
        }
    }
}

// MARK: - Home

private struct HomeTab: View {
    @ObservedObject var viewModel: AlertRelayViewModel
    @Binding var showCancelSheet: Bool
    @Binding var pinInput: String
    let isPro: Bool

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    emergencyCard
                    if isPro {
                        relayCard
                        overrideCard
                        activityCard
                    } else {
                        proUpsellCard
                    }
                }
                .padding(16)
            }
            .background(
                LinearGradient(colors: [.black, RelayColors.deep],
                               startPoint: .topLeading,
                               endPoint: .bottomTrailing)
                    .ignoresSafeArea()
            )
            .navigationTitle(isPro ? "PulseLink Pro" : "PulseLink")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        if isPro {
                            Text("PRO")
                                .font(.caption2.bold())
                                .padding(4)
                                .background(RelayColors.accent)
                                .foregroundColor(.white)
                                .cornerRadius(4)
                        }
                        Image(systemName: "antenna.radiowaves.left.and.right")
                            .foregroundStyle(RelayColors.primary)
                    }
                }
            }
        }
    }

    private var emergencyCard: some View {
        Card {
            HStack {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Emergency")
                        .font(.headline)
                    Text("Send a trusted emergency and override DND / ringer.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(.red)
            }

            VStack(spacing: 14) {
                switch viewModel.emergencyState {
                case .idle:
                    Button {
                        viewModel.startEmergencyCountdown()
                    } label: {
                        Label("Send Emergency", systemImage: "bolt.fill")
                            .font(.title3.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.red)

                case .arming(let seconds):
                    Button {
                        showCancelSheet = true
                    } label: {
                        Label("Arming \(seconds)s", systemImage: "timer")
                            .font(.title3.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.orange)

                case .active:
                    Button {
                        showCancelSheet = true
                    } label: {
                        Label("Cancel Emergency", systemImage: "hand.raised.fill")
                            .font(.title3.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.red)
                }

                if viewModel.emergencyState == .active {
                    Text("Emergency is live. Contacts will be alerted even if DND is on; ringer set to max.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
        }
    }

    private var relayCard: some View {
        Card {
            HStack {
                Text("Relay")
                    .font(.headline)
                Spacer()
                Image(systemName: "waveform.path.ecg.rectangle")
                    .foregroundStyle(RelayColors.accent)
            }
            Text(viewModel.statusText)
                .font(.subheadline)
                .foregroundStyle(.secondary)

            Button {
                Task { await viewModel.sendTestAlert() }
            } label: {
                Label("Send Test Alert", systemImage: "paperplane.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(RelayColors.primary)
        }
    }

    private var overrideCard: some View {
        Card {
            HStack {
                Text("Alert delivery")
                    .font(.headline)
                Spacer()
                Image(systemName: "bell.and.waves.left.and.right.fill")
                    .foregroundStyle(RelayColors.primary)
            }
            Toggle("Override Do Not Disturb for trusted alerts", isOn: $viewModel.overrideDND)
            Toggle("Max volume on urgent messages", isOn: $viewModel.maxVolumeOnUrgent)
                .tint(.red)
            Text("iOS limits full control of DND/volume; PulseLink will request critical alerts permission and play loud tones for urgent messages.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private var activityCard: some View {
        Card {
            HStack {
                Text("Recent activity")
                    .font(.headline)
                Spacer()
                Text(Date.now, style: .time)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Label("Emergency drill queued for 3 contacts", systemImage: "bolt.fill")
                .foregroundStyle(RelayColors.accent)
            Label("Check-in acknowledged - Morgan", systemImage: "checkmark.circle.fill")
                .foregroundStyle(.secondary)
            Label("Widget updated with latest status", systemImage: "rectangle.dashed.badge.record")
                .foregroundStyle(.secondary)
        }
    }

    private var proUpsellCard: some View {
        Card {
            HStack {
                Text("Upgrade to Pro")
                    .font(.headline)
                Spacer()
                Image(systemName: "star.fill")
                    .foregroundStyle(RelayColors.accent)
            }
            Text("Unlock SMS Relay, Contacts, and DND Overrides.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }
}

// MARK: - Contacts

private struct ContactsTab: View {
    @ObservedObject var viewModel: AlertRelayViewModel
    @Binding var selectedContact: ContactCard?
    @State private var draftMessage = ""
    @State private var urgent = true

    var body: some View {
        NavigationStack {
            List {
                Section("Trusted contacts") {
                    ForEach(viewModel.trustedContacts) { contact in
                        NavigationLink(value: contact) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(contact.name).font(.headline)
                                    Text(contact.role).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                PresenceDot(presence: contact.presence)
                                if contact.unread > 0 {
                                    // Explicit 'text' label required for Swift 6 / Xcode 16.1
                                    Badge(text: "\(contact.unread)")
                                }
                            }
                        }
                    }
                }
            }
            .navigationDestination(for: ContactCard.self) { contact in
                ConversationView(
                    contact: contact,
                    messages: viewModel.messages(for: contact),
                    onSend: { text, urgent in
                        viewModel.sendMessage(to: contact, text: text, urgent: urgent)
                    },
                    onAppear: { viewModel.startListeningToConversation(contact: contact) },
                    onDisappear: { viewModel.stopListeningToConversation() }
                )
            }
            .navigationTitle("Contacts")
        }
    }
}

private struct ConversationView: View {
    let contact: ContactCard
    let messages: [ConversationMessage]
    let onSend: (String, Bool) -> Void
    var onAppear: (() -> Void)? = nil
    var onDisappear: (() -> Void)? = nil

    @State private var draft = ""
    @State private var urgent = true

    var body: some View {
        VStack {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 12) {
                        ForEach(messages) { msg in
                            HStack {
                                if msg.isIncoming { Spacer() }
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(msg.text)
                                        .padding(12)
                                        .background(msg.isUrgent ? Color.red.opacity(0.15) : Color(.secondarySystemBackground))
                                        .foregroundStyle(msg.isUrgent ? .red : .primary)
                                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                    Text(msg.timestamp, style: .time)
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                                if !msg.isIncoming { Spacer() }
                            }
                            .id(msg.id)
                        }
                    }
                    .padding()
                }
                .onAppear {
                    if let last = messages.last { proxy.scrollTo(last.id, anchor: .bottom) }
                }
            }
            .onAppear { onAppear?() }
            .onDisappear { onDisappear?() }

            VStack(spacing: 8) {
                Toggle("Mark urgent (override DND, boost volume)", isOn: $urgent)
                    .font(.caption)
                    .padding(.horizontal)
                HStack {
                    TextField("Message", text: $draft)
                        .textFieldStyle(.roundedBorder)
                    Button {
                        guard !draft.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                        onSend(draft, urgent)
                        draft = ""
                    } label: {
                        Image(systemName: "paperplane.fill")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(RelayColors.primary)
                }
                .padding(.horizontal)
                .padding(.bottom, 8)
            }
            .background(.thinMaterial)
        }
        .navigationTitle(contact.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Settings

private struct SettingsTab: View {
    @ObservedObject var viewModel: AlertRelayViewModel
    @State private var baseUrlDraft: String = ""
    @State private var showDeleteConfirmation = false
    @State private var isDeleting = false

    var body: some View {
        Form {
            Section("Relay") {
                TextField("Relay base URL", text: Binding(
                    get: { baseUrlDraft.isEmpty ? viewModel.baseUrl : baseUrlDraft },
                    set: { baseUrlDraft = $0 }
                ), prompt: Text("https://example.com"))
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                Button("Apply URL") {
                    viewModel.baseUrl = baseUrlDraft.trimmingCharacters(in: .whitespacesAndNewlines)
                }
            }
            Section("Alerts") {
                Toggle("Override Do Not Disturb", isOn: $viewModel.overrideDND)
                Toggle("Max volume on urgent", isOn: $viewModel.maxVolumeOnUrgent)
            }
            Section("Account") {
                Button("Sign Out", role: .destructive) {
                    #if canImport(FirebaseAuth)
                    try? FirebaseAuth.Auth.auth().signOut()
                    #endif
                }

                Button("Delete Account", role: .destructive) {
                    showDeleteConfirmation = true
                }
            }
            Section("About") {
                Label("Matches Android experience: emergency, trusted contacts, urgent chat", systemImage: "arrow.left.arrow.right")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .navigationTitle("Settings")
        .alert("Delete Account", isPresented: $showDeleteConfirmation) {
            Button("Cancel", role: .cancel) { }
            Button("Delete", role: .destructive) {
                isDeleting = true
                Task {
                    do {
                        try await viewModel.deleteAccount()
                    } catch {
                        isDeleting = false
                    }
                }
            }
        } message: {
            Text("This will permanently delete your account and all associated data. This action cannot be undone.")
        }
        .overlay {
            if isDeleting {
                ZStack {
                    Color.black.opacity(0.4).ignoresSafeArea()
                    ProgressView("Deleting...")
                        .padding()
                        .background(.regularMaterial)
                        .cornerRadius(8)
                }
            }
        }
    }
}

// MARK: - Components

private struct Card<Content: View>: View {
    let content: Content
    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            content
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct PresenceDot: View {
    let presence: Presence
    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(color)
                .frame(width: 10, height: 10)
            Text(presence.label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color(.secondarySystemBackground))
        .clipShape(Capsule())
    }

    private var color: Color {
        switch presence {
        case .online: return .green
        case .recent: return .yellow
        case .offline: return .red
        }
    }
}

private struct Badge: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.caption.bold())
            .foregroundStyle(.white)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(RelayColors.accent)
            .clipShape(Capsule())
    }
}

private struct CancelEmergencySheet: View {
    @Binding var pinInput: String
    var onSubmit: (String) -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("Cancel Emergency")
                    .font(.title2.bold())
                Text("Enter your PIN to cancel the active alert.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                SecureField("PIN", text: $pinInput)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .multilineTextAlignment(.center)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                Button {
                    onSubmit(pinInput)
                } label: {
                    Label("Confirm cancel", systemImage: "hand.raised.fill")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(.red)
            }
            .padding()
            .presentationDetents([.fraction(0.4)])
        }
    }
}

// MARK: - Theme

enum RelayColors {
    static let primary = Color(red: 0.39, green: 0.40, blue: 0.95)   // Indigo (#6366f1)
    static let accent  = Color(red: 0.75, green: 0.53, blue: 0.12)   // Gold deep
    static let deep    = Color(red: 0.01, green: 0.02, blue: 0.03)   // Deep Dark (#030407)
}

#Preview {
    ContentView(viewModel: AlertRelayViewModel())
}
