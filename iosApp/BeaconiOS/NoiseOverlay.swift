import SwiftUI
import UIKit

struct NoiseOverlay: View {
    @State private var noiseImage: UIImage?

    var body: some View {
        GeometryReader { proxy in
            if let image = noiseImage {
                Image(uiImage: image)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: proxy.size.width, height: proxy.size.height)
                    .blendMode(.overlay)
                    .opacity(0.08) // Slight grain
            } else {
                Color.clear.onAppear {
                    // Generate noise on appear
                    DispatchQueue.global(qos: .userInitiated).async {
                        let img = self.generateNoiseImage(size: CGSize(width: 512, height: 512))
                        DispatchQueue.main.async {
                            self.noiseImage = img
                        }
                    }
                }
            }
        }
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }

    func generateNoiseImage(size: CGSize) -> UIImage? {
        let width = Int(size.width)
        let height = Int(size.height)
        let bitsPerComponent = 8
        let bytesPerPixel = 4
        let bytesPerRow = width * bytesPerPixel
        var data = [UInt8](repeating: 0, count: width * height * bytesPerPixel)

        for i in 0..<(width * height) {
            let offset = i * bytesPerPixel
            let val = UInt8.random(in: 0...255)
            data[offset] = val     // R
            data[offset + 1] = val // G
            data[offset + 2] = val // B
            data[offset + 3] = 128 // A (Semi-transparent)
        }

        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue)

        guard let context = CGContext(data: &data, width: width, height: height, bitsPerComponent: bitsPerComponent, bytesPerRow: bytesPerRow, space: colorSpace, bitmapInfo: bitmapInfo.rawValue),
              let cgImage = context.makeImage() else { return nil }

        return UIImage(cgImage: cgImage)
    }
}
