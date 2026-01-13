import Foundation
#if canImport(FirebaseFirestore)
import FirebaseFirestore
import FirebaseAuth
import FirebaseMessaging
import UIKit
#endif

final class DeviceManager {
    static let shared = DeviceManager()

    private init() {}

    @MainActor
    func registerDevice() async {
        #if canImport(FirebaseFirestore)
        guard let user = Auth.auth().currentUser else {
            print("Cannot register device: No authenticated user")
            return
        }
        guard let deviceId = UIDevice.current.identifierForVendor?.uuidString else {
            print("Cannot register device: No vendor identifier available")
            return
        }

        let token = try? await Messaging.messaging().token()

        let db = Firestore.firestore()
        let deviceData: [String: Any] = [
            "uid": user.uid,
            "name": UIDevice.current.name,
            "model": UIDevice.current.model,
            "platform": "iOS",
            "app": "PulseLink",
            "fcmToken": token ?? "",
            "updatedAt": FieldValue.serverTimestamp()
        ]

        do {
            try await db.collection("devices").document(deviceId).setData(deviceData, merge: true)
            print("Device registered: \(deviceId)")
        } catch {
            print("Failed to register device: \(error.localizedDescription)")
        }
        #endif
    }
}
