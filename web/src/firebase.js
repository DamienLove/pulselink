import { initializeApp, getApps } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getFunctions } from "firebase/functions";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID
};

// Bolt: Prevent crash on missing config (e.g. CI/CD or code review envs)
let app;
if (!firebaseConfig.apiKey) {
  console.warn("Firebase API key missing. App will run in mock mode or fail gracefully.");
  const apps = getApps();
  if (apps.length > 0) {
      app = apps[0];
  } else {
      // Initialize with dummy values to allow UI to render without crashing immediately
      app = initializeApp({
          ...firebaseConfig,
          apiKey: "dummy-key",
          projectId: "dummy-project"
      });
  }
} else {
  app = initializeApp(firebaseConfig);
}

export const auth = getAuth(app);
export const db = getFirestore(app);
export const functions = getFunctions(app, "us-central1");
