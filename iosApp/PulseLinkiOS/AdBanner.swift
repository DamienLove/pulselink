import SwiftUI
#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

struct AdBanner: UIViewRepresentable {
    func makeUIView(context: Context) -> some UIView {
        #if canImport(GoogleMobileAds)
        let banner = GADBannerView(adSize: GADAdSizeBanner)
        // Test Ad Unit ID for development
        banner.adUnitID = "ca-app-pub-3940256099942544/2934735716"
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootVC = windowScene.windows.first?.rootViewController {
            banner.rootViewController = rootVC
        }
        banner.load(GADRequest())
        return banner
        #else
        return UIView()
        #endif
    }

    func updateUIView(_ uiView: UIViewType, context: Context) {}
}
