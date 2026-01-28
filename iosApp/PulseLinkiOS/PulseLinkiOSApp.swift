import SwiftUI
import Shared
#if canImport(FirebaseCore)
import FirebaseCore
#endif
#if canImport(FirebaseMessaging)
import FirebaseMessaging
#endif
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

@main
struct PulseLinkiOSApp: App {
    @StateObject private var relay = AlertRelayViewModel()
    // Keep reference to delegate to prevent deallocation
    @State private var messagingDelegate = MessagingDelegateAdapter()

    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: relay)
                .task {
                    // Configure Firebase if GoogleService-Info.plist is present
                    _ = FirebaseBootstrap.isConfigured
                    // Initialize AdMob
                    #if canImport(GoogleMobileAds)
                    GADMobileAds.sharedInstance().start(completionHandler: nil)
                    #endif
                    // Set messaging delegate
                    #if canImport(FirebaseMessaging)
                    Messaging.messaging().delegate = messagingDelegate
                    #endif
                    // Ask for critical alert permission up front
                    _ = await NotificationManager.shared.requestCriticalAlertsPermission()
                }
        }
    }
}
