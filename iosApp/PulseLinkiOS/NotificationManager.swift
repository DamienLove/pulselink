import Foundation
import UserNotifications
import AVFoundation
import AudioToolbox

final class NotificationManager {
    static let shared = NotificationManager()

    private init() {}

    func requestCriticalAlertsPermission() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let options: UNAuthorizationOptions = [.alert, .sound, .criticalAlert]
        do {
            let granted = try await center.requestAuthorization(options: options)
            return granted
        } catch {
            return false
        }
    }

    func postCriticalLocal(title: String, body: String, volumeBoost: Bool) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        if volumeBoost {
            content.sound = UNNotificationSound.defaultCriticalSound(withAudioVolume: 1.0)
        } else {
            content.sound = .default
        }
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.1, repeats: false)
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: trigger)
        UNUserNotificationCenter.current().add(request)
    }

    func playAlertToneIfNeeded(volumeBoost: Bool) {
        guard volumeBoost else { return }
        // Simple attention sound bundled with system; no custom asset required.
        AudioServicesPlaySystemSound(1005) // Tri-tone
    }
}
