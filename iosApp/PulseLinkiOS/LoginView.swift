import SwiftUI
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif

struct LoginView: View {
    @State private var email = ""
    @State private var password = ""
    @State private var error: String?
    @State private var isBusy = false

    var onLoginSuccess: () -> Void

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "waveform.path.ecg.rectangle")
                .font(.system(size: 60))
                .foregroundStyle(RelayColors.primary)
                .padding(.bottom, 20)

            Text("PulseLink")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Sign in to sync your Android messages")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            VStack(spacing: 12) {
                TextField("Email", text: $email)
                    .textFieldStyle(.roundedBorder)
                    .autocapitalization(.none)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)

                SecureField("Password", text: $password)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.password)
            }
            .padding(.horizontal)

            if let error = error {
                Text(error)
                    .foregroundColor(.red)
                    .font(.caption)
                    .multilineTextAlignment(.center)
            }

            Button(action: login) {
                if isBusy {
                    ProgressView()
                        .tint(.white)
                } else {
                    Text("Sign In")
                        .fontWeight(.bold)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(RelayColors.accent)
            .disabled(isBusy || email.isEmpty || password.isEmpty)
            .padding(.horizontal)

            Button(action: loginAnonymously) {
                if isBusy {
                    ProgressView()
                } else {
                    Text("SMS Only Mode (Anonymous)")
                        .font(.footnote)
                }
            }
            .disabled(isBusy)
            .padding(.top, 8)

            Spacer()
        }
        .padding()
        .background(Color(UIColor.systemBackground))
    }

    private func login() {
        #if canImport(FirebaseAuth)
        guard !email.isEmpty, !password.isEmpty else { return }
        isBusy = true
        error = nil
        Auth.auth().signIn(withEmail: email, password: password) { result, err in
            isBusy = false
            if let err = err {
                self.error = err.localizedDescription
                return
            }
            onLoginSuccess()
        }
        #else
        onLoginSuccess()
        #endif
    }

    private func loginAnonymously() {
        #if canImport(FirebaseAuth)
        isBusy = true
        error = nil
        Auth.auth().signInAnonymously { result, err in
            isBusy = false
            if let err = err {
                self.error = err.localizedDescription
                return
            }
            onLoginSuccess()
        }
        #else
        onLoginSuccess()
        #endif
    }
}
