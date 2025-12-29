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

                BeaconTab(viewModel: viewModel, filter: .private)
                    .tabItem {
                        Label("Private", systemImage: "lock.fill")
                    }

                SettingsTab(viewModel: viewModel)
                    .tabItem {
                        Label("Settings", systemImage: "gear")
                    }
            }
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
    @State private var pinInput = ""
    @State private var isUnlocked = false
    @State private var showPinSheet = false
    @AppStorage("privateSafePin") private var storedPin: String = ""
    @AppStorage("themeColor") private var themeColor: ThemeColor = .blue
    @AppStorage("bubbleStyle") private var bubbleStyle: BubbleStyle = .rounded

    var isPro: Bool {
        #if PRO
        return true
        #else
        return false
        #endif
    }

    var filteredContacts: [BeaconContactCard] {
        let base: [BeaconContactCard]
        switch filter {
        case .inbox: base = viewModel.inboxContacts
        case .trusted: base = viewModel.trustedContacts
        case .favorites: base = viewModel.favoriteContacts
        case .private: base = viewModel.privateContacts
        }

        if searchText.isEmpty {
            return base
        } else {
            return base.filter { $0.name.localizedCaseInsensitiveContains(searchText) || $0.address.contains(searchText) }
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                if filter == .private && !isUnlocked {
                    VStack(spacing: 20) {
                        Image(systemName: "lock.circle.fill")
                            .font(.system(size: 60))
                            .foregroundStyle(.secondary)
                        Text(storedPin.isEmpty ? "Setup Private Safe" : "Private Safe Locked")
                            .font(.title2.bold())
                        Button(storedPin.isEmpty ? "Set PIN" : "Unlock") {
                            showPinSheet = true
                        }
                        .buttonStyle(.borderedProminent)
                    }
                } else {
                    List(filteredContacts) { contact in
                        NavigationLink(value: contact) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(contact.name).font(.headline)
                                    Text(contact.role).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if contact.unread > 0 {
                                    Text("\(contact.unread)")
                                        .font(.caption.bold())
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(themeColor.color)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
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
            .navigationDestination(for: BeaconContactCard.self) { contact in
                ConversationView(
                    contact: contact,
                    messages: viewModel.messages(for: contact),
                    onSend: { text in
                        viewModel.sendMessage(to: contact, text: text)
                    }
                )
            }
            .navigationTitle(isPro && filter == .inbox ? "\(filter.title) Pro" : filter.title)
            .sheet(isPresented: $showPinSheet) {
                NavigationStack {
                    VStack(spacing: 20) {
                        Text(storedPin.isEmpty ? "Create a PIN" : "Enter PIN")
                            .font(.headline)
                        SecureField("PIN", text: $pinInput)
                            .keyboardType(.numberPad)
                            .padding()
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .padding(.horizontal)

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
                                    // Shake animation or error feedback could go here
                                    pinInput = ""
                                }
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(pinInput.count < 4)
                    }
                    .padding()
                    .presentationDetents([.height(300)])
                }
            }
        }
    }
}

private struct ConversationView: View {
    let contact: BeaconContactCard
    let messages: [BeaconConversationMessage]
    let onSend: (String) -> Void

    @State private var draft = ""
    @AppStorage("themeColor") private var themeColor: ThemeColor = .blue
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
                                        .background(msg.isIncoming ? Color(.secondarySystemBackground) : themeColor.color)
                                        .foregroundStyle(msg.isIncoming ? .primary : .white)
                                        .clipShape(bubbleStyle.shape)
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

            HStack {
                TextField("Message", text: $draft)
                    .textFieldStyle(.roundedBorder)
                Button {
                    guard !draft.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                    onSend(draft)
                    draft = ""
                } label: {
                    Image(systemName: "paperplane.fill")
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
            .background(.thinMaterial)
        }
        .navigationTitle(contact.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct SettingsTab: View {
    @ObservedObject var viewModel: BeaconViewModel
    @AppStorage("themeColor") private var themeColor: ThemeColor = .blue
    @AppStorage("bubbleStyle") private var bubbleStyle: BubbleStyle = .rounded

    var body: some View {
        NavigationStack {
            Form {
                Section("Appearance") {
                    Picker("Accent Color", selection: $themeColor) {
                        ForEach(ThemeColor.allCases, id: \.self) { color in
                            Text(color.rawValue.capitalized).tag(color)
                        }
                    }
                    Picker("Bubble Style", selection: $bubbleStyle) {
                        ForEach(BubbleStyle.allCases, id: \.self) { style in
                            Text(style.rawValue.capitalized).tag(style)
                        }
                    }
                }
                Section("Account") {
                    Button("Sign Out", role: .destructive) {
                        #if canImport(FirebaseAuth)
                        try? FirebaseAuth.Auth.auth().signOut()
                        #endif
                    }
                }
                Section("About") {
                    Text("Beacon iOS")
                }
            }
            .navigationTitle("Settings")
        }
    }
}

enum ThemeColor: String, CaseIterable {
    case blue, purple, orange, green, pink

    var color: Color {
        switch self {
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
}
