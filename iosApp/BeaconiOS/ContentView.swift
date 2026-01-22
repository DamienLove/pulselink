import SwiftUI
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

struct ContentView: View {
    @ObservedObject var viewModel: BeaconViewModel

    var isPro: Bool {
        #if PRO
        return true
        #else
        return false
        #endif
    }

    init(viewModel: BeaconViewModel) {
        self.viewModel = viewModel
        // Customize TabBar for Future Deep v11
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
                BeaconTab(viewModel: viewModel, filter: .inbox)
                    .tabItem {
                        Label("Inbox", systemImage: "bubble.left.and.bubble.right.fill")
                    }
                    .badge(isPro ? "Pro" : nil)

                BeaconTab(viewModel: viewModel, filter: .trusted)
                    .tabItem {
                        Label("Trusted", systemImage: "shield.fill")
                    }

                BeaconTab(viewModel: viewModel, filter: .favorites)
                    .tabItem {
                        Label("Favorites", systemImage: "star.fill")
                    }

                if isPro {
                    BeaconTab(viewModel: viewModel, filter: .private)
                        .tabItem {
                            Label("Private", systemImage: "lock.fill")
                        }
                }

                SettingsTab(viewModel: viewModel, isPro: isPro)
                    .tabItem {
                        Label("Settings", systemImage: "gear")
                    }
            }
            .tint(RelayColors.primary)
        } else {
            LoginView {
                // Auth listener will handle transition
            }
        }
    }
}

enum BeaconTabFilter {
    case inbox, trusted, favorites, `private`

    var title: String {
        switch self {
        case .inbox: return "Beacon Inbox"
        case .trusted: return "Trusted"
        case .favorites: return "Favorites"
        case .private: return "Private Safe"
        }
    }
}

private struct BeaconTab: View {
    @ObservedObject var viewModel: BeaconViewModel
    let filter: BeaconTabFilter
    @State private var searchText = ""
    @State private var subFilter: InboxSubFilter = .all
    @State private var pinInput = ""
    @State private var isUnlocked = false
    @State private var showPinSheet = false
    @AppStorage("privateSafePin") private var storedPin: String = ""
    @AppStorage("themeColor") private var themeColor: ThemeColor = .cyan
    @AppStorage("bubbleStyle") private var bubbleStyle: BubbleStyle = .rounded

    var isPro: Bool {
        #if PRO
        return true
        #else
        return false
        #endif
    }

    enum InboxSubFilter: String, CaseIterable {
        case all = "All"
        case read = "Read"
        case unread = "Unread"
    }

    var filteredContacts: [BeaconContactCard] {
        let base: [BeaconContactCard]
        switch filter {
        case .inbox: base = viewModel.inboxContacts
        case .trusted: base = viewModel.trustedContacts
        case .favorites: base = viewModel.favoriteContacts
        case .private: base = viewModel.privateContacts
        }

        let subFiltered: [BeaconContactCard]
        if filter != .private {
            switch subFilter {
            case .all: subFiltered = base
            case .read: subFiltered = base.filter { $0.unread == 0 }
            case .unread: subFiltered = base.filter { $0.unread > 0 }
            }
        } else {
            subFiltered = base
        }

        if searchText.isEmpty {
            return subFiltered
        } else {
            return subFiltered.filter { $0.name.localizedCaseInsensitiveContains(searchText) || $0.address.contains(searchText) }
        }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                RelayColors.deep.ignoresSafeArea()

                // Cinematic Gradient
                LinearGradient(
                    colors: [
                        RelayColors.primary.opacity(0.1),
                        RelayColors.tertiary.opacity(0.05),
                        .black
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                VStack {
                    if filter != .private {
                        VStack(spacing: 8) {
                            Picker("Filter", selection: $subFilter) {
                                ForEach(InboxSubFilter.allCases, id: \.self) { f in
                                    Text(f.rawValue).tag(f)
                                }
                            }
                            .pickerStyle(.segmented)
                            .padding(.horizontal)

                            // Custom segmented control styling is hard in SwiftUI native picker,
                            // relying on dark mode default which is decent.

                            if let date = viewModel.lastUpdated {
                                Text("Synced \(date, style: .time)")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .padding(.top, 10)
                    }

                    Group {
                        if filter == .private && !isUnlocked {
                            VStack(spacing: 20) {
                                Image(systemName: "lock.circle.fill")
                                    .font(.system(size: 60))
                                    .foregroundStyle(RelayColors.primary.opacity(0.7))
                                    .shadow(color: RelayColors.primary.opacity(0.5), radius: 10)
                                Text(storedPin.isEmpty ? "Setup Private Safe" : "Private Safe Locked")
                                    .font(.title2.bold())
                                    .foregroundStyle(.white)
                                Button(storedPin.isEmpty ? "Set PIN" : "Unlock") {
                                    showPinSheet = true
                                }
                                .buttonStyle(GlassButtonStyle(color: RelayColors.primary))
                            }
                            .frame(maxHeight: .infinity)
                        } else {
                            List(filteredContacts) { contact in
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
                                        if contact.unread > 0 {
                                            Text("\(contact.unread)")
                                                .font(.caption.bold())
                                                .foregroundStyle(.black)
                                                .padding(.horizontal, 8)
                                                .padding(.vertical, 4)
                                                .background(themeColor.color) // Use user theme
                                                .clipShape(Capsule())
                                                .shadow(color: themeColor.color.opacity(0.6), radius: 4)
                                        }
                                    }
                                }
                                .listRowBackground(Color.black.opacity(0.3))
                            }
                            .scrollContentBackground(.hidden)
                            .searchable(text: $searchText)
                            .overlay {
                                if filteredContacts.isEmpty {
                                    ContentUnavailableView(
                                        "No conversations",
                                        systemImage: "bubble.left.and.bubble.right",
                                        description: Text("Start a new chat on your Android device.")
                                    )
                                }
                            }
                        }
                    }
                }
            }
            .navigationDestination(for: BeaconContactCard.self) { contact in
                ConversationView(
                    contact: contact,
                    messages: viewModel.messages(for: contact),
                    onSend: { text in
                        viewModel.sendMessage(to: contact, text: text)
                    },
                    onAppear: { viewModel.startListeningToConversation(contact: contact) },
                    onDisappear: { viewModel.stopListeningToConversation() }
                )
            }
            .navigationTitle(isPro && filter == .inbox ? "\(filter.title) Pro" : filter.title)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbarBackground(Color.black.opacity(0.8), for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .sheet(isPresented: $showPinSheet) {
                NavigationStack {
                    VStack(spacing: 20) {
                        Text(storedPin.isEmpty ? "Create a PIN" : "Enter PIN")
                            .font(.headline)
                            .foregroundStyle(.white)
                        SecureField("PIN", text: $pinInput)
                            .keyboardType(.numberPad)
                            .padding()
                            .background(Color.white.opacity(0.1))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.1)))
                            .padding(.horizontal)
                            .foregroundStyle(.white)

                        Button(storedPin.isEmpty ? "Save PIN" : "Unlock") {
                            if storedPin.isEmpty {
                                if pinInput.count >= 4 {
                                    storedPin = pinInput
                                    isUnlocked = true
                                    showPinSheet = false
                                    pinInput = ""
                                }
                            } else {
                                if pinInput == storedPin {
                                    isUnlocked = true
                                    showPinSheet = false
                                    pinInput = ""
                                } else {
                                    pinInput = ""
                                }
                            }
                        }
                        .buttonStyle(GlassButtonStyle(color: RelayColors.primary))
                        .disabled(pinInput.count < 4)
                    }
                    .padding()
                    .presentationDetents([.height(300)])
                    .background(RelayColors.deep.ignoresSafeArea())
                }
            }
        }
    }
}

private struct ConversationView: View {
    let contact: BeaconContactCard
    let messages: [BeaconConversationMessage]
    let onSend: (String) -> Void
    var onAppear: (() -> Void)? = nil
    var onDisappear: (() -> Void)? = nil

    @State private var draft = ""
    @AppStorage("themeColor") private var themeColor: ThemeColor = .cyan
    @AppStorage("bubbleStyle") private var bubbleStyle: BubbleStyle = .rounded

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
                                            msg.isIncoming ? Color.white.opacity(0.1) : themeColor.color.opacity(0.8)
                                        )
                                        .background(.ultraThinMaterial)
                                        .foregroundStyle(msg.isIncoming ? .white : .black)
                                        .clipShape(bubbleStyle.shape)
                                        .overlay(
                                            bubbleStyle.shapeOverlay
                                                .stroke(msg.isIncoming ? Color.white.opacity(0.1) : Color.clear, lineWidth: 1)
                                        )
                                        .shadow(color: msg.isIncoming ? .clear : themeColor.color.opacity(0.4), radius: 5)
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

            HStack {
                TextField("Message", text: $draft)
                    .textFieldStyle(PlainTextFieldStyle())
                    .padding(10)
                    .background(Color.white.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .foregroundStyle(.white)

                Button {
                    guard !draft.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                    onSend(draft)
                    draft = ""
                } label: {
                    Image(systemName: "paperplane.fill")
                }
                .buttonStyle(GlassButtonStyle(color: themeColor.color))
            }
            .padding()
            .background(.ultraThinMaterial)
        }
        .background(RelayColors.deep.ignoresSafeArea())
        .navigationTitle(contact.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SettingsTab: View {
    @ObservedObject var viewModel: BeaconViewModel
    let isPro: Bool
    @State private var showDeleteConfirmation = false
    @State private var isDeleting = false
    @AppStorage("themeColor") private var themeColor: ThemeColor = .cyan
    @AppStorage("bubbleStyle") private var bubbleStyle: BubbleStyle = .rounded

    var body: some View {
        NavigationStack {
            Form {
                if isPro {
                    Section("Appearance") {
                        Picker("Accent Color", selection: $themeColor) {
                            ForEach(ThemeColor.allCases, id: \.self) { color in
                                Text(color.rawValue.capitalized).tag(color)
                            }
                        }
                        .listRowBackground(Color.black.opacity(0.5))

                        Picker("Bubble Style", selection: $bubbleStyle) {
                            ForEach(BubbleStyle.allCases, id: \.self) { style in
                                Text(style.rawValue.capitalized).tag(style)
                            }
                        }
                        .listRowBackground(Color.black.opacity(0.5))
                    }
                } else {
                     Section("Appearance") {
                         Text("Upgrade to Pro to customize themes.")
                             .foregroundStyle(.secondary)
                     }
                     .listRowBackground(Color.black.opacity(0.5))
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
                .listRowBackground(Color.black.opacity(0.5))

                Section("About") {
                    Text("Beacon iOS")
                        .foregroundStyle(.white)
                }
                .listRowBackground(Color.black.opacity(0.5))
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
                        print("Delete account error: \(error)")
                    }
                    isDeleting = false
                }
            }
        } message: {
            Text("This will permanently delete your account and all associated data.")
        }
    }
}

// Reuse GlassButtonStyle and RelayColors
struct GlassButtonStyle: ButtonStyle {
    let color: Color
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(color == .white ? .black : .white) // Fix contrast
            .padding(.horizontal, 16) // Ensure padding
            .padding(.vertical, 10)
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

enum ThemeColor: String, CaseIterable {
    case cyan, indigo, blue, purple, orange, green, pink

    var color: Color {
        switch self {
        case .cyan: return .cyan
        case .indigo: return .indigo
        case .blue: return .blue
        case .purple: return .purple
        case .orange: return .orange
        case .green: return .green
        case .pink: return .pink
        }
    }
}

enum BubbleStyle: String, CaseIterable {
    case rounded, square, capsule

    var shape: AnyShape {
        switch self {
        case .rounded: return AnyShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        case .square: return AnyShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
        case .capsule: return AnyShape(Capsule())
        }
    }

    var shapeOverlay: AnyShape {
        switch self {
        case .rounded: return AnyShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        case .square: return AnyShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
        case .capsule: return AnyShape(Capsule())
        }
    }
}

enum RelayColors {
    // Future Deep v11
    static let primary = Color(red: 0.0, green: 0.953, blue: 1.0)
    static let tertiary = Color(red: 0.878, green: 0.0, blue: 1.0)
    static let deep    = Color.black
    static let glass   = Color(red: 0.08, green: 0.08, blue: 0.08).opacity(0.6)
}
