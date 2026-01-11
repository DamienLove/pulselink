import Foundation
#if canImport(FirebaseFirestore)
import FirebaseFirestore
#endif

#if canImport(FirebaseFirestore)
// ListenerRegistration is available via FirebaseFirestore import
#else
public protocol ListenerRegistration {
    func remove()
}
#endif

class MockListener: ListenerRegistration {
    func remove() {}
}

class CompositeListener: ListenerRegistration {
    private var listeners: [ListenerRegistration] = []
    private let queue = DispatchQueue(label: "com.pulselink.beacon.compositeListener", attributes: .concurrent)

    func add(_ listener: ListenerRegistration) {
        queue.async(flags: .barrier) {
            self.listeners.append(listener)
        }
    }

    func remove() {
        queue.sync {
            self.listeners
        }.forEach { $0.remove() }

        queue.async(flags: .barrier) {
            self.listeners.removeAll()
        }
    }
}

class WrapperListener: ListenerRegistration {
    let composite: ListenerRegistration
    let cleanup: () -> Void

    init(composite: ListenerRegistration, cleanup: @escaping () -> Void) {
        self.composite = composite
        self.cleanup = cleanup
    }

    func remove() {
        composite.remove()
        cleanup()
    }
}
