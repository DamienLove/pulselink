import Foundation

enum BeaconPresence: String {
    case online, recent, offline

    var label: String {
        switch self {
        case .online: return "Online"
        case .recent: return "Recent"
        case .offline: return "Offline"
        }
    }
}

struct BeaconContactCard: Identifiable, Hashable {
    let id = UUID()
    let threadId: String
    var lineId: String? = nil
    let name: String
    let address: String
    let role: String
    let presence: BeaconPresence
    var unread: Int
    var isFavorite: Bool
    var isPrivate: Bool
    var isTrusted: Bool
}

struct BeaconConversationMessage: Identifiable {
    let id = UUID()
    let sender: String
    let text: String
    let timestamp: Date
    let isIncoming: Bool
    let isUrgent: Bool
}
