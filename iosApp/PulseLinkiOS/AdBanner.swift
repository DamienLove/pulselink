import SwiftUI
#if canImport(GoogleMobileAds)
import GoogleMobileAds

struct AdBanner: UIViewRepresentable {
    func makeUIView(context: Context) -> GADBannerView {
        let banner = GADBannerView(adSize: GADAdSizeBanner)
        banner.adUnitID = "ca-app-pub-3940256099942544/2934735716" // Test ID

        // Root view controller is required for ad presentation
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let rootViewController = scene.windows.first?.rootViewController {
            banner.rootViewController = rootViewController
        }

        banner.load(GADRequest())
        return banner
    }

    func updateUIView(_ uiView: GADBannerView, context: Context) {}
}
#else
struct AdBanner: View {
    var body: some View {
        EmptyView()
    }
}
#endif
