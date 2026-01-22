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

    init(viewModel: AlertRelayViewModel) {
        self.viewModel = viewModel
        // Customize TabBar to match Future Deep v11
        let appearance = UITabBarAppearance()
        appearance.configureWithTransparentBackground()
        appearance.backgroundColor = UIColor(Color.black.opacity(0.8))
        appearance.backgroundEffect = UIBlurEffect(style: .systemUltraThinMaterialDark)

        UITabBar.appearance().standardAppearance = appearance
        UITabBar.appearance().scrollEdgeAppearance = appearance
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
            .tint(RelayColors.primary) // Laser Blue tint for tabs
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

    @State private var pulse = false

    var body: some View {
        NavigationStack {
            ZStack {
                // Background
                RelayColors.deep.ignoresSafeArea()

                // Cinematic Gradient
                LinearGradient(
                    colors: [
                        RelayColors.primary.opacity(0.15),
                        RelayColors.tertiary.opacity(0.05),
                        .black
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        emergencyCard

                        if viewModel.statusText != "Idle" {
                            Text(viewModel.statusText)
                                .font(.footnote.bold())
                                .foregroundStyle(RelayColors.primary)
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(.vertical, 8)
                                .background(.ultraThinMaterial)
                                .clipShape(Capsule())
                                .overlay(Capsule().stroke(RelayColors.primary.opacity(0.3), lineWidth: 1))
                                .shadow(color: RelayColors.primary.opacity(0.3), radius: 10)
                        }

                        relayCard
                        overrideCard
                        activityCard

                        if !isPro {
                            proUpsellCard
                        }
                    }
                    .padding(16)
                }
            }
            .navigationTitle(isPro ? "PulseLink Pro" : "PulseLink")
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarBackground(Color.black.opacity(0.5), for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        if isPro {
                            Text("PRO")
                                .font(.caption2.bold())
                                .padding(4)
                                .background(
                                    LinearGradient(colors: [RelayColors.primary, RelayColors.tertiary], startPoint: .leading, endPoint: .trailing)
                                )
                                .foregroundColor(.black)
                                .cornerRadius(4)
                        }
                        Image(systemName: "antenna.radiowaves.left.and.right")
                            .foregroundStyle(RelayColors.primary)
                            .shadow(color: RelayColors.primary, radius: 5)
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
                        .foregroundStyle(.white)
                    Text("Send a trusted emergency and override DND.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(.red)
                    .shadow(color: .red.opacity(0.5), radius: 5)
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
                    .buttonStyle(GlassButtonStyle(color: .red))

                case .arming(let seconds):
                    Button {
                        showCancelSheet = true
                    } label: {
                        Label("Arming \(seconds)s", systemImage: "timer")
                            .font(.title3.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(GlassButtonStyle(color: .orange))
                    .scaleEffect(pulse ? 1.02 : 1.0)
                    .animation(.easeInOut(duration: 0.5).repeatForever(autoreverses: true), value: pulse)
                    .onAppear { pulse = true }

                case .active:
                    Button {
                        showCancelSheet = true
                    } label: {
                        Label("Cancel Emergency", systemImage: "hand.raised.fill")
                            .font(.title3.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(GlassButtonStyle(color: .red))
                    .shadow(color: .red, radius: 20)
                }

                if viewModel.emergencyState == .active {
                    Text("Emergency is live. Contacts will be alerted even if DND is on; ringer set to max.")
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .shadow(color: .red.opacity(0.5), radius: 2)
                }
            }
        }
    }

    private var relayCard: some View {
        Card {
            HStack {
                Text("Relay")
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Image(systemName: "waveform.path.ecg.rectangle")
                    .foregroundStyle(RelayColors.primary)
            }

            Button {
                Task { await viewModel.sendTestAlert() }
            } label: {
                Label("Send Test Alert", systemImage: "paperplane.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(GlassButtonStyle(color: RelayColors.primary))
        }
    }

    private var overrideCard: some View {
        Card {
            HStack {
                Text("Alert delivery")
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Image(systemName: "bell.and.waves.left.and.right.fill")
                    .foregroundStyle(RelayColors.primary)
            }
            Toggle("Override Do Not Disturb", isOn: $viewModel.overrideDND)
                .tint(RelayColors.primary)
            Toggle("Max volume on urgent", isOn: $viewModel.maxVolumeOnUrgent)
                .tint(.red)
            Text("PulseLink will request critical alerts permission to bypass silent mode.")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private var activityCard: some View {
        Card {
            HStack {
                Text("Recent activity")
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Text(Date.now, style: .time)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Label("Emergency drill queued", systemImage: "bolt.fill")
                .foregroundStyle(RelayColors.tertiary)
            Label("Check-in acknowledged", systemImage: "checkmark.circle.fill")
                .foregroundStyle(.secondary)
        }
    }

    private var proUpsellCard: some View {
        Card {
            HStack {
                Text("Upgrade to Pro")
                    .font(.headline)
                    .foregroundStyle(.white)
                Spacer()
                Image(systemName: "star.fill")
                    .foregroundStyle(RelayColors.tertiary)
            }
            Text("Unlock Trusted Contacts and Priority Support.")
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
                                    Text(contact.name)
                                        .font(.headline)
                                        .foregroundStyle(.white)
                                    Text(contact.role)
                                        .font(.caption)
                                        .foregroundStyle(RelayColors.primary)
                                }
                                Spacer()
                                PresenceDot(presence: contact.presence)
                                if contact.unread > 0 {
                                    Badge(text: "\(contact.unread)")
                                }
                            }
                        }
                        .listRowBackground(Color.black.opacity(0.5))
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(
                ZStack {
                    RelayColors.deep
                    LinearGradient(colors: [.black, RelayColors.deep], startPoint: .top, endPoint: .bottom)
                }
                .ignoresSafeArea()
            )
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
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarBackground(Color.black.opacity(0.8), for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
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
                                        .background(
                                            msg.isUrgent ? Color.red.opacity(0.2) :
                                                (msg.isIncoming ? Color(.secondarySystemBackground).opacity(0.2) : RelayColors.primary.opacity(0.2))
                                        )
                                        .background(.ultraThinMaterial)
                                        .foregroundStyle(msg.isUrgent ? .red : .white)
                                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                                .stroke(msg.isUrgent ? Color.red.opacity(0.5) : (msg.isIncoming ? Color.white.opacity(0.1) : RelayColors.primary.opacity(0.3)), lineWidth: 1)
                                        )
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
                Toggle("Mark urgent", isOn: $urgent)
                    .font(.caption)
                    .padding(.horizontal)
                    .foregroundStyle(urgent ? .red : .secondary)
                    .tint(.red)

                HStack {
                    TextField("Message", text: $draft)
                        .textFieldStyle(PlainTextFieldStyle())
                        .padding(10)
                        .background(Color.white.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .foregroundStyle(.white)

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
            .background(.regularMaterial)
        }
        .background(RelayColors.deep.ignoresSafeArea())
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
        NavigationStack {
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
                    .foregroundStyle(RelayColors.primary)
                }
                Section("Alerts") {
                    Toggle("Override Do Not Disturb", isOn: $viewModel.overrideDND)
                        .tint(RelayColors.primary)
                    Toggle("Max volume on urgent", isOn: $viewModel.maxVolumeOnUrgent)
                        .tint(.red)
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
            }
            .scrollContentBackground(.hidden)
            .background(RelayColors.deep.ignoresSafeArea())
            .navigationTitle("Settings")
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarBackground(Color.black.opacity(0.8), for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
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
            Text("This will permanently delete your account and all associated data.")
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
        // Glassmorphism effect
        .background(.ultraThinMaterial)
        .backgroundColor(Color(white: 0.1, opacity: 0.2)) // Fallback/Tint
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(RelayColors.primary.opacity(0.2), lineWidth: 1)
        )
        .shadow(color: Color.black.opacity(0.3), radius: 10, x: 0, y: 5)
    }
}

extension View {
    func backgroundColor(_ color: Color) -> some View {
        background(color)
    }
}

struct GlassButtonStyle: ButtonStyle {
    let color: Color
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(.white)
            .background(
                ZStack {
                    color.opacity(0.6)
                    if configuration.isPressed {
                        Color.white.opacity(0.2)
                    }
                }
            )
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(color.opacity(0.8), lineWidth: 1)
            )
            .shadow(color: color.opacity(0.3), radius: 8)
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
            .animation(.easeOut(duration: 0.2), value: configuration.isPressed)
    }
}

private struct PresenceDot: View {
    let presence: Presence
    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)
                .shadow(color: color.opacity(0.8), radius: 4)
            Text(presence.label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color.black.opacity(0.3))
        .clipShape(Capsule())
        .overlay(Capsule().stroke(Color.white.opacity(0.1), lineWidth: 0.5))
    }

    private var color: Color {
        switch presence {
        case .online: return RelayColors.primary
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
            .foregroundStyle(.black)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(RelayColors.primary)
            .clipShape(Capsule())
            .shadow(color: RelayColors.primary.opacity(0.5), radius: 5)
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
                    .background(Color.white.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.1)))

                Button {
                    onSubmit(pinInput)
                } label: {
                    Label("Confirm cancel", systemImage: "hand.raised.fill")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                }
                .buttonStyle(GlassButtonStyle(color: .red))
            }
            .padding()
            .presentationDetents([.fraction(0.4)])
            .background(RelayColors.deep.ignoresSafeArea())
        }
    }
}

// MARK: - Theme

enum RelayColors {
    // Future Deep v11
    // Primary: #00F3FF -> Laser Blue
    static let primary = Color(red: 0.0, green: 0.953, blue: 1.0)
    // Tertiary: #E000FF -> Neon Purple
    static let tertiary = Color(red: 0.878, green: 0.0, blue: 1.0)
    // Background: #000000 -> Pitch Black
    static let deep    = Color.black

    // Glass
    static let glass   = Color(red: 0.08, green: 0.08, blue: 0.08).opacity(0.6)
}

#Preview {
    ContentView(viewModel: AlertRelayViewModel())
        .preferredColorScheme(.dark)
}
