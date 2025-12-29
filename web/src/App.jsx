import { useState, useEffect, useMemo, useRef, memo } from 'react';
import { auth, db, functions } from './firebase';
import {
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  sendPasswordResetEmail
} from "firebase/auth";
import {
  collection,
  query,
  orderBy,
  onSnapshot,
  addDoc,
  serverTimestamp,
  doc,
  setDoc,
  deleteDoc,
  getDocs,
  limit,
  writeBatch
} from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import './App.css';
import logo from './assets/pulselink-pro-logo.png';
import beaconLogo from './assets/beacon-logo.png';

const areThreadsEqual = (prev, next) => {
  return prev.isActive === next.isActive &&
         prev.showPreviews === next.showPreviews &&
         prev.onSelect === next.onSelect &&
         prev.thread.id === next.thread.id &&
         prev.thread.address === next.thread.address &&
         prev.thread.snippet === next.thread.snippet;
};

// Bolt: Optimized ThreadItem with memo to prevent unnecessary re-renders of the entire list
// when only the selection state changes or when unrelated threads update.
const ThreadItem = memo(({ thread, isActive, onSelect, showPreviews }) => (
  <button
    className={`thread-item ${isActive ? 'active' : ''}`}
    onClick={() => onSelect(thread)}
    aria-current={isActive ? 'true' : undefined}
    aria-label={`Select conversation with ${thread.address}`}
  >
    <div className="thread-name">{thread.address}</div>
    <div className="thread-snippet">{showPreviews ? thread.snippet : '••••••'}</div>
  </button>
), areThreadsEqual);

ThreadItem.displayName = 'ThreadItem';

const areMessagesEqual = (prev, next) => {
  return prev.showPreviews === next.showPreviews &&
         prev.msg.id === next.msg.id &&
         prev.msg.body === next.msg.body &&
         prev.msg.date === next.msg.date &&
         prev.msg.type === next.msg.type;
};

// Bolt: Optimized MessageItem with memo to prevent re-rendering all messages when typing
// or when new messages arrive (which creates new object references).
const MessageItem = memo(({ msg, showPreviews }) => (
  <div className={`message ${msg.type === 1 ? 'received' : 'sent'}`}>
    <div className="message-bubble">
      {showPreviews ? msg.body : '••••••'}
    </div>
    <div className="message-time">
      {new Date(msg.date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
    </div>
  </div>
), areMessagesEqual);

MessageItem.displayName = 'MessageItem';

const defaultTheme = {
  primaryColor: "#6750A4",
  secondaryColor: "#625B71",
  bubbleOutgoing: "#D0BCFF",
  bubbleIncoming: "#E8DEF8",
  backgroundColor: "#FFFFFF",
  iconSizeFactor: 1.0,
  fontStyle: "Default",
  bubbleCornerRadius: 12,
  inboxIconVariant: "Default",
  onBubbleOutgoing: "#000000",
  onBubbleIncoming: "#000000",
  onBackground: "#000000",
  topBarColor: "#FFFFFF",
  onTopBarColor: "#000000",
  bubbleCornerRadiusTopStart: null,
  bubbleCornerRadiusTopEnd: null,
  bubbleCornerRadiusBottomStart: null,
  bubbleCornerRadiusBottomEnd: null,
  timestampColor: null,
  dividerColor: null,
  appBackgroundGradientStart: null,
  appBackgroundGradientEnd: null,
  fontScale: 1.0,
  backgroundImageUrl: null,
  iconOverrides: {}
};

const themePresets = [
  {
    name: "Default Light",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 12,
      backgroundColor: "#FFFFFF",
      onBackground: "#111827",
      topBarColor: "#F3F4F6",
      onTopBarColor: "#111827",
      bubbleOutgoing: "#D0BCFF",
      onBubbleOutgoing: "#111827",
      bubbleIncoming: "#E8DEF8",
      onBubbleIncoming: "#111827",
      primaryColor: "#6750A4",
      secondaryColor: "#625B71",
      dividerColor: "#E5E7EB",
      inboxIconVariant: "default_light"
    }
  },
  {
    name: "Midnight OLED",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 14,
      backgroundColor: "#0B0B0F",
      onBackground: "#F1F5F9",
      topBarColor: "#111827",
      onTopBarColor: "#F8FAFC",
      bubbleOutgoing: "#1F2937",
      onBubbleOutgoing: "#F8FAFC",
      bubbleIncoming: "#0F172A",
      onBubbleIncoming: "#E2E8F0",
      primaryColor: "#38BDF8",
      secondaryColor: "#22D3EE",
      timestampColor: "#94A3B8",
      dividerColor: "#1F2937",
      inboxIconVariant: "midnight_oled"
    }
  },
  {
    name: "Ocean Deep",
    theme: {
      fontStyle: "Serif",
      bubbleCornerRadius: 8,
      backgroundColor: "#0F172A",
      onBackground: "#E2E8F0",
      topBarColor: "#1E293B",
      onTopBarColor: "#E2E8F0",
      bubbleOutgoing: "#2563EB",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#334155",
      onBubbleIncoming: "#E2E8F0",
      primaryColor: "#38BDF8",
      secondaryColor: "#1D4ED8",
      dividerColor: "#334155",
      inboxIconVariant: "ocean_deep"
    }
  },
  {
    name: "Rose Petal",
    theme: {
      fontStyle: "Cursive",
      bubbleCornerRadius: 18,
      backgroundColor: "#FFF1F2",
      onBackground: "#881337",
      topBarColor: "#FFE4E6",
      onTopBarColor: "#881337",
      bubbleOutgoing: "#FB7185",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#FECACA",
      onBubbleIncoming: "#7F1D1D",
      primaryColor: "#E11D48",
      secondaryColor: "#F43F5E",
      dividerColor: "#FBCFE8",
      inboxIconVariant: "rose_petal"
    }
  },
  {
    name: "Sunset Fade",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 24,
      appBackgroundGradientStart: "#FF5F6D",
      appBackgroundGradientEnd: "#FFC371",
      onBackground: "#FFFFFF",
      topBarColor: "#FF5F6D",
      onTopBarColor: "#FFFFFF",
      bubbleOutgoing: "#FFFFFF",
      onBubbleOutgoing: "#FF5F6D",
      bubbleIncoming: "#FFF7ED",
      onBubbleIncoming: "#C2410C",
      primaryColor: "#FF5F6D",
      secondaryColor: "#F97316",
      dividerColor: "#FED7AA",
      inboxIconVariant: "sunset_fade",
      bubbleCornerRadiusTopStart: 0,
      bubbleCornerRadiusBottomEnd: 0
    }
  },
  {
    name: "Citrus Pop",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 10,
      backgroundColor: "#F7FEE7",
      onBackground: "#365314",
      topBarColor: "#ECFCCB",
      onTopBarColor: "#365314",
      bubbleOutgoing: "#84CC16",
      onBubbleOutgoing: "#1A2E05",
      bubbleIncoming: "#DCFCE7",
      onBubbleIncoming: "#14532D",
      primaryColor: "#65A30D",
      secondaryColor: "#84CC16",
      dividerColor: "#D9F99D",
      inboxIconVariant: "citrus_pop"
    }
  },
  {
    name: "Forest Trail",
    theme: {
      fontStyle: "Serif",
      bubbleCornerRadius: 14,
      backgroundColor: "#ECFDF5",
      onBackground: "#064E3B",
      topBarColor: "#D1FAE5",
      onTopBarColor: "#064E3B",
      bubbleOutgoing: "#059669",
      onBubbleOutgoing: "#ECFDF5",
      bubbleIncoming: "#A7F3D0",
      onBubbleIncoming: "#064E3B",
      primaryColor: "#10B981",
      secondaryColor: "#059669",
      dividerColor: "#A7F3D0",
      inboxIconVariant: "forest_trail"
    }
  },
  {
    name: "Lavender Haze",
    theme: {
      fontStyle: "Cursive",
      bubbleCornerRadius: 16,
      backgroundColor: "#F5F3FF",
      onBackground: "#4C1D95",
      topBarColor: "#EDE9FE",
      onTopBarColor: "#4C1D95",
      bubbleOutgoing: "#C4B5FD",
      onBubbleOutgoing: "#312E81",
      bubbleIncoming: "#EDE9FE",
      onBubbleIncoming: "#4C1D95",
      primaryColor: "#7C3AED",
      secondaryColor: "#A78BFA",
      dividerColor: "#DDD6FE",
      inboxIconVariant: "lavender_haze",
      iconSizeFactor: 1.1
    }
  },
  {
    name: "Slate Mono",
    theme: {
      fontStyle: "Monospace",
      bubbleCornerRadius: 6,
      backgroundColor: "#F8FAFC",
      onBackground: "#0F172A",
      topBarColor: "#E2E8F0",
      onTopBarColor: "#0F172A",
      bubbleOutgoing: "#CBD5E1",
      onBubbleOutgoing: "#0F172A",
      bubbleIncoming: "#F1F5F9",
      onBubbleIncoming: "#0F172A",
      primaryColor: "#475569",
      secondaryColor: "#94A3B8",
      dividerColor: "#CBD5E1",
      inboxIconVariant: "slate_mono",
      iconSizeFactor: 0.95
    }
  },
  {
    name: "Aurora",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 20,
      appBackgroundGradientStart: "#0F766E",
      appBackgroundGradientEnd: "#6366F1",
      onBackground: "#F8FAFC",
      topBarColor: "#0F766E",
      onTopBarColor: "#F8FAFC",
      bubbleOutgoing: "#6366F1",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#14B8A6",
      onBubbleIncoming: "#FFFFFF",
      primaryColor: "#14B8A6",
      secondaryColor: "#6366F1",
      dividerColor: "#5EEAD4",
      inboxIconVariant: "aurora",
      iconSizeFactor: 1.15
    }
  },
  {
    name: "Desert Clay",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 12,
      backgroundColor: "#FFF7ED",
      onBackground: "#7C2D12",
      topBarColor: "#FFEDD5",
      onTopBarColor: "#7C2D12",
      bubbleOutgoing: "#FB923C",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#FED7AA",
      onBubbleIncoming: "#7C2D12",
      primaryColor: "#EA580C",
      secondaryColor: "#FDBA74",
      dividerColor: "#FED7AA",
      inboxIconVariant: "sunset_fade"
    }
  },
  {
    name: "Nord Frost",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 10,
      backgroundColor: "#ECEFF4",
      onBackground: "#2E3440",
      topBarColor: "#E5E9F0",
      onTopBarColor: "#2E3440",
      bubbleOutgoing: "#81A1C1",
      onBubbleOutgoing: "#ECEFF4",
      bubbleIncoming: "#D8DEE9",
      onBubbleIncoming: "#2E3440",
      primaryColor: "#5E81AC",
      secondaryColor: "#88C0D0",
      dividerColor: "#D8DEE9",
      inboxIconVariant: "default_light"
    }
  },
  {
    name: "Neon Noir",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 16,
      backgroundColor: "#0B0F14",
      onBackground: "#E2E8F0",
      topBarColor: "#111827",
      onTopBarColor: "#E2E8F0",
      bubbleOutgoing: "#22D3EE",
      onBubbleOutgoing: "#0B0F14",
      bubbleIncoming: "#1F2937",
      onBubbleIncoming: "#E2E8F0",
      primaryColor: "#22D3EE",
      secondaryColor: "#F472B6",
      dividerColor: "#1F2937",
      inboxIconVariant: "midnight_oled"
    }
  },
  {
    name: "Paperback",
    theme: {
      fontStyle: "Serif",
      bubbleCornerRadius: 14,
      backgroundColor: "#FFFBEB",
      onBackground: "#3F2D1C",
      topBarColor: "#FEF3C7",
      onTopBarColor: "#3F2D1C",
      bubbleOutgoing: "#FCD34D",
      onBubbleOutgoing: "#3F2D1C",
      bubbleIncoming: "#FDE68A",
      onBubbleIncoming: "#3F2D1C",
      primaryColor: "#B45309",
      secondaryColor: "#D97706",
      dividerColor: "#FDE68A",
      inboxIconVariant: "default_light"
    }
  },
  {
    name: "Mint Breeze",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 12,
      backgroundColor: "#ECFDF3",
      onBackground: "#064E3B",
      topBarColor: "#D1FAE5",
      onTopBarColor: "#064E3B",
      bubbleOutgoing: "#34D399",
      onBubbleOutgoing: "#064E3B",
      bubbleIncoming: "#A7F3D0",
      onBubbleIncoming: "#064E3B",
      primaryColor: "#10B981",
      secondaryColor: "#34D399",
      dividerColor: "#A7F3D0",
      inboxIconVariant: "forest_trail"
    }
  },
  {
    name: "Amethyst Night",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 18,
      appBackgroundGradientStart: "#312E81",
      appBackgroundGradientEnd: "#0F172A",
      onBackground: "#E2E8F0",
      topBarColor: "#312E81",
      onTopBarColor: "#E2E8F0",
      bubbleOutgoing: "#7C3AED",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#1E293B",
      onBubbleIncoming: "#E2E8F0",
      primaryColor: "#8B5CF6",
      secondaryColor: "#6366F1",
      dividerColor: "#312E81",
      inboxIconVariant: "lavender_haze"
    }
  }
];

const iconOverrideKeys = [
  { key: "icon.back", label: "Back" },
  { key: "icon.settings", label: "Settings" },
  { key: "icon.lock", label: "Private" },
  { key: "icon.search", label: "Search" },
  { key: "icon.close", label: "Close" },
  { key: "icon.inbox", label: "Inbox" },
  { key: "icon.archive", label: "Archive" },
  { key: "icon.unarchive", label: "Unarchive" },
  { key: "icon.delete", label: "Delete" },
  { key: "icon.send", label: "Send" },
  { key: "icon.ai", label: "AI" },
  { key: "icon.notifications", label: "Notifications" }
];

const normalizeTheme = (input = {}) => ({
  ...defaultTheme,
  ...input,
  iconSizeFactor: Number(input.iconSizeFactor ?? defaultTheme.iconSizeFactor),
  bubbleCornerRadius: Number(input.bubbleCornerRadius ?? defaultTheme.bubbleCornerRadius),
  fontScale: Number(input.fontScale ?? defaultTheme.fontScale),
  bubbleCornerRadiusTopStart: input.bubbleCornerRadiusTopStart ?? defaultTheme.bubbleCornerRadiusTopStart,
  bubbleCornerRadiusTopEnd: input.bubbleCornerRadiusTopEnd ?? defaultTheme.bubbleCornerRadiusTopEnd,
  bubbleCornerRadiusBottomStart: input.bubbleCornerRadiusBottomStart ?? defaultTheme.bubbleCornerRadiusBottomStart,
  bubbleCornerRadiusBottomEnd: input.bubbleCornerRadiusBottomEnd ?? defaultTheme.bubbleCornerRadiusBottomEnd,
  timestampColor: input.timestampColor ?? defaultTheme.timestampColor,
  dividerColor: input.dividerColor ?? defaultTheme.dividerColor,
  appBackgroundGradientStart: input.appBackgroundGradientStart ?? defaultTheme.appBackgroundGradientStart,
  appBackgroundGradientEnd: input.appBackgroundGradientEnd ?? defaultTheme.appBackgroundGradientEnd,
  backgroundImageUrl: input.backgroundImageUrl ?? defaultTheme.backgroundImageUrl,
  iconOverrides: input.iconOverrides ?? defaultTheme.iconOverrides
});

const buildThemeVars = (theme) => {
  const active = normalizeTheme(theme);
  return {
    "--accent": active.primaryColor,
    "--accent-strong": active.secondaryColor,
    "--bg": active.appBackgroundGradientEnd ?? active.backgroundColor,
    "--bg-accent": active.appBackgroundGradientStart ?? active.backgroundColor,
    "--surface": active.topBarColor ?? active.backgroundColor,
    "--surface-alt": active.bubbleIncoming ?? active.backgroundColor,
    "--ink": active.onBackground,
    "--muted": active.timestampColor ?? "#9aa4b2",
    "--border": active.dividerColor ?? "#25304a",
    "--bubble-outgoing": active.bubbleOutgoing,
    "--bubble-incoming": active.bubbleIncoming,
    "--on-bubble-outgoing": active.onBubbleOutgoing,
    "--on-bubble-incoming": active.onBubbleIncoming,
    "--app-gradient-start": active.appBackgroundGradientStart ?? active.backgroundColor,
    "--app-gradient-end": active.appBackgroundGradientEnd ?? active.backgroundColor
  };
};

const buildThemePreviewStyle = (theme) => {
  const active = normalizeTheme(theme);
  const style = {
    backgroundColor: active.backgroundColor
  };
  if (active.appBackgroundGradientStart && active.appBackgroundGradientEnd) {
    style.backgroundImage = `linear-gradient(135deg, ${active.appBackgroundGradientStart}, ${active.appBackgroundGradientEnd})`;
  }
  if (active.backgroundImageUrl) {
    style.backgroundImage = `url(${active.backgroundImageUrl})`;
    style.backgroundSize = 'cover';
    style.backgroundPosition = 'center';
  }
  return style;
};

const parseList = (raw) =>
  raw
    .split(',')
    .map(item => item.trim())
    .filter(item => item.length > 0);

const buildContactDocId = (contact) => {
  const phone = contact.phoneNumber?.trim();
  const email = contact.email?.trim().toLowerCase();
  if (phone) return phone;
  if (email) return `email_${email}`;
  return contact.displayName.trim().toLowerCase().replace(/\s+/g, '_') || `contact_${Date.now()}`;
};

const toMillis = (value) => {
  if (!value) return 0;
  if (typeof value === 'number') return value;
  if (typeof value.toMillis === 'function') return value.toMillis();
  if (typeof value.seconds === 'number') return value.seconds * 1000;
  return 0;
};

const alertBadgeCopy = {
  emergency: 'Emergency',
  check_in: 'Check-in',
  non_urgent: 'Alert'
};

const alertBadgeColor = {
  emergency: '#f43f5e',
  check_in: '#22c55e',
  non_urgent: '#60a5fa'
};

const buildAlertSnippet = (body = '') => {
  const firstLine = body.split('\n')[0] ?? '';
  if (firstLine.length <= 88) return firstLine;
  return `${firstLine.slice(0, 85)}...`;
};

const loadGoogleMaps = (() => {
  let loaderPromise;
  return (apiKey) => {
    if (typeof window === 'undefined') {
      return Promise.reject(new Error('Maps unavailable in this environment.'));
    }
    if (!apiKey) {
      return Promise.reject(new Error('Maps API key missing.'));
    }
    if (window.google?.maps) {
      return Promise.resolve(window.google.maps);
    }
    if (!loaderPromise) {
      loaderPromise = new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&v=weekly`;
        script.async = true;
        script.defer = true;
        script.onload = () => resolve(window.google.maps);
        script.onerror = () => reject(new Error('Failed to load Google Maps.'));
        document.head.appendChild(script);
      });
    }
    return loaderPromise;
  };
})();

function App() {
  const [user, setUser] = useState(null);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [threads, setThreads] = useState([]);
  const [selectedThread, setSelectedThread] = useState(null);
  const [messages, setMessages] = useState([]);
  const [profile, setProfile] = useState({
    ownerName: '',
    avatarUrl: '',
    email: '',
    phoneNumber: ''
  });
  const [contacts, setContacts] = useState([]);
  const [contactSearch, setContactSearch] = useState('');
  const [contactForm, setContactForm] = useState({
    displayName: '',
    phoneNumber: '',
    email: '',
    additionalPhones: '',
    additionalEmails: '',
    escalationTier: 'EMERGENCY',
    includeLocation: true,
    autoCall: false,
    allowRemoteOverride: true,
    allowRemoteSoundChange: false
  });
  const [editingContactId, setEditingContactId] = useState(null);
  const [contactStatus, setContactStatus] = useState('');
  const [profileStatus, setProfileStatus] = useState('');
  const [themePrefs, setThemePrefs] = useState(defaultTheme);
  const [themeStatus, setThemeStatus] = useState('');
  const [publicThemes, setPublicThemes] = useState([]);
  const [themeGalleryStatus, setThemeGalleryStatus] = useState('');
  const [themeSearch, setThemeSearch] = useState('');
  const [themePublishForm, setThemePublishForm] = useState({
    name: '',
    authorName: '',
    authorHandle: '',
    anonymous: false,
    backgroundImageUrl: ''
  });
  const [themePublishStatus, setThemePublishStatus] = useState('');
  const [remoteSettings, setRemoteSettings] = useState({
    remoteWebAccessEnabled: false,
    autoUpdateContactInfo: true,
    timeFormat: 'AUTO'
  });
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [authError, setAuthError] = useState('');
  const [composeAddress, setComposeAddress] = useState('');
  const [composeBody, setComposeBody] = useState('');
  const [sendStatus, setSendStatus] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [activePanel, setActivePanel] = useState('home');
  const [alertLocations, setAlertLocations] = useState([]);
  const [alertStatus, setAlertStatus] = useState('');
  const [severityFilter, setSeverityFilter] = useState('emergency');
  const [incomingOnly, setIncomingOnly] = useState(true);
  const [selectedAlertId, setSelectedAlertId] = useState(null);
  const [mapStatus, setMapStatus] = useState('');
  const [geoStatus, setGeoStatus] = useState('');
  const [userLocation, setUserLocation] = useState(null);
  const [settingsStatus, setSettingsStatus] = useState('');
  const [deleteStatus, setDeleteStatus] = useState('');
  const [showPreviews, setShowPreviews] = useState(true);
  const [autoScroll, setAutoScroll] = useState(true);
  const messagesEndRef = useRef(null);
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const mapMarkersRef = useRef(new Map());
  const mapInfoRef = useRef(null);
  const mapHomeMarkerRef = useRef(null);
  const mapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
  const defaultMapCenter = useMemo(() => ({ lat: 39.5, lng: -98.35 }), []);
  const themeVars = useMemo(() => buildThemeVars(themePrefs), [themePrefs]);

  const filteredAlerts = useMemo(() => {
    return alertLocations.filter((alert) => {
      if (incomingOnly && !alert.incoming) return false;
      if (severityFilter === 'emergency') return alert.severity === 'emergency';
      if (severityFilter === 'check_in') return alert.severity === 'check_in';
      if (severityFilter === 'non_urgent') return alert.severity === 'non_urgent';
      return true;
    });
  }, [alertLocations, incomingOnly, severityFilter]);
  const filteredThemes = useMemo(() => {
    const term = themeSearch.trim().toLowerCase();
    if (!term) return publicThemes;
    return publicThemes.filter((theme) => {
      const name = (theme.name ?? '').toString().toLowerCase();
      const author = (theme.authorName ?? '').toString().toLowerCase();
      const handle = (theme.authorHandle ?? '').toString().toLowerCase();
      return name.includes(term) || author.includes(term) || handle.includes(term);
    });
  }, [publicThemes, themeSearch]);
  const filteredContacts = useMemo(() => {
    const term = contactSearch.trim().toLowerCase();
    if (!term) return contacts;
    return contacts.filter((contact) => {
      const values = [
        contact.displayName,
        contact.phoneNumber,
        contact.email,
        ...(contact.additionalPhones || []),
        ...(contact.additionalEmails || [])
      ];
      return values.some((value) =>
        (value ?? '').toString().toLowerCase().includes(term)
      );
    });
  }, [contacts, contactSearch]);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setIsLoggingIn(false);
      setActivePanel(currentUser ? 'home' : 'home');
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (!user) {
      setProfile({ ownerName: '', avatarUrl: '', email: '', phoneNumber: '' });
      setThemePrefs(defaultTheme);
      setRemoteSettings({
        remoteWebAccessEnabled: false,
        autoUpdateContactInfo: true,
        timeFormat: 'AUTO'
      });
      return;
    }
    const userRef = doc(db, "users", user.uid);
    const unsubscribe = onSnapshot(userRef, (snapshot) => {
      const data = snapshot.data() || {};
      setProfile({
        ownerName: data.ownerName ?? user.displayName ?? '',
        avatarUrl: data.avatarUrl ?? '',
        email: data.email ?? user.email ?? '',
        phoneNumber: data.phoneNumber ?? ''
      });
      if (data.themePreferences) {
        setThemePrefs(normalizeTheme(data.themePreferences));
      } else {
        setThemePrefs(defaultTheme);
      }
      setRemoteSettings({
        remoteWebAccessEnabled: data.remoteWebAccessEnabled ?? false,
        autoUpdateContactInfo: data.autoUpdateContactInfo ?? true,
        timeFormat: data.timeFormat ?? 'AUTO'
      });
    });
    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (!user) {
      setContacts([]);
      return;
    }
    const contactsRef = collection(db, "users", user.uid, "trustedContacts");
    const unsubscribe = onSnapshot(contactsRef, (snapshot) => {
      const items = snapshot.docs.map(docSnap => ({
        id: docSnap.id,
        ...docSnap.data()
      }));
      items.sort((a, b) => (a.contactOrder ?? 0) - (b.contactOrder ?? 0));
      setContacts(items);
    });
    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (user) {
      // Listen to threads
      // Assuming structure: users/{uid}/synced_threads/{threadId}
      const threadsRef = collection(db, "users", user.uid, "synced_threads");
      const q = query(threadsRef, orderBy("date", "desc"));
      const unsubscribe = onSnapshot(q, (snapshot) => {
        const threadsData = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        setThreads(threadsData);
      });
      return () => unsubscribe();
    } else {
      setThreads([]);
    }
  }, [user]);

  useEffect(() => {
    const themesRef = collection(db, "themes_public");
    const q = query(themesRef, orderBy("createdAt", "desc"), limit(200));
    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        const items = snapshot.docs.map(docSnap => ({
          id: docSnap.id,
          ...docSnap.data()
        }));
        setPublicThemes(items);
      },
      (error) => {
        console.error("Failed to load theme gallery", error);
        setThemeGalleryStatus(error?.message ?? "Unable to load theme gallery.");
      }
    );
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (user && selectedThread) {
      // Listen to messages
      const messagesRef = collection(db, "users", user.uid, "synced_threads", selectedThread.id, "messages");
      const q = query(messagesRef, orderBy("date", "asc"));
      const unsubscribe = onSnapshot(q, (snapshot) => {
        const messagesData = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data()
        }));
        setMessages(messagesData);
      });
      return () => unsubscribe();
    } else {
      setMessages([]);
    }
  }, [user, selectedThread]);

  useEffect(() => {
    if (selectedThread?.address) {
      setComposeAddress(selectedThread.address);
    }
    setComposeBody('');
    setSendStatus('');
  }, [selectedThread?.address]);

  // Auto-scroll to bottom when messages change
  useEffect(() => {
    if (autoScroll) {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, autoScroll]);

  useEffect(() => {
    if (!user) {
      setAlertLocations([]);
      setAlertStatus('');
      return;
    }
    const alertsRef = collection(db, "users", user.uid, "emergencyLocations");
    const q = query(alertsRef, orderBy("createdAt", "desc"), limit(200));
    const unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        const items = snapshot.docs.map(docSnap => {
          const data = docSnap.data();
          const lat = Number(data.lat);
          const lng = Number(data.lng);
          if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
          return {
            id: docSnap.id,
            lat,
            lng,
            severity: data.severity ?? 'emergency',
            incoming: data.incoming ?? false,
            address: data.sourceName || data.sourcePhone || (data.incoming ? 'Trusted contact' : 'You'),
            body: data.message ?? '',
            date: toMillis(data.createdAt),
            clearedAt: data.clearedAt ?? null
          };
        }).filter(Boolean).filter(item => !item.clearedAt);
        setAlertLocations(items);
        setAlertStatus(items.length ? '' : 'No emergencies recently — that’s good news.');
      },
      (error) => {
        console.error('Failed to load emergency locations', error);
        setAlertStatus(error?.message ?? 'Unable to load emergency locations.');
      }
    );
    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (activePanel !== 'map') return;
    if (!mapsApiKey) {
      setMapStatus('Add VITE_GOOGLE_MAPS_API_KEY to load the map.');
      return;
    }
    setMapStatus('');
    let cancelled = false;
    loadGoogleMaps(mapsApiKey)
      .then(() => {
        if (cancelled) return;
        if (!mapInstanceRef.current && mapRef.current) {
          mapInstanceRef.current = new window.google.maps.Map(mapRef.current, {
            center: defaultMapCenter,
            zoom: 3,
            mapTypeControl: false,
            fullscreenControl: false,
            streetViewControl: false
          });
        }
      })
      .catch((error) => {
        if (cancelled) return;
        setMapStatus(error?.message ?? 'Map failed to load.');
      });
    return () => {
      cancelled = true;
    };
  }, [activePanel, mapsApiKey, defaultMapCenter]);

  useEffect(() => {
    if (activePanel !== 'map') return;
    if (!navigator.geolocation) {
      setGeoStatus('Location services are not available in this browser.');
      return;
    }
    if (userLocation) return;
    setGeoStatus('Locating your position…');
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const next = { lat: position.coords.latitude, lng: position.coords.longitude };
        setUserLocation(next);
        setGeoStatus('Showing your current location.');
      },
      (error) => {
        const message = error?.message ?? 'Unable to access location.';
        setGeoStatus(message);
      },
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 300000 }
    );
  }, [activePanel, userLocation]);

  useEffect(() => {
    if (!mapInstanceRef.current || !window.google?.maps) return;
    mapMarkersRef.current.forEach((marker) => marker.setMap(null));
    mapMarkersRef.current.clear();
    if (mapHomeMarkerRef.current) {
      mapHomeMarkerRef.current.setMap(null);
      mapHomeMarkerRef.current = null;
    }

    if (!filteredAlerts.length) {
      const center = userLocation ?? defaultMapCenter;
      mapInstanceRef.current.setCenter(center);
      mapInstanceRef.current.setZoom(userLocation ? 12 : 3);
      if (userLocation) {
        mapHomeMarkerRef.current = new window.google.maps.Marker({
          position: center,
          map: mapInstanceRef.current,
          title: 'Your location',
          icon: {
            path: window.google.maps.SymbolPath.CIRCLE,
            fillColor: '#3b82f6',
            fillOpacity: 0.9,
            strokeColor: '#0b0e16',
            strokeWeight: 2,
            scale: 7
          }
        });
      }
      return;
    }

    const bounds = new window.google.maps.LatLngBounds();
    filteredAlerts.forEach((alert) => {
      const color = alertBadgeColor[alert.severity] ?? alertBadgeColor.non_urgent;
      const marker = new window.google.maps.Marker({
        position: { lat: alert.lat, lng: alert.lng },
        map: mapInstanceRef.current,
        title: `${alertBadgeCopy[alert.severity] ?? 'Alert'} from ${alert.address}`,
        icon: {
          path: window.google.maps.SymbolPath.CIRCLE,
          fillColor: color,
          fillOpacity: 0.9,
          strokeColor: '#0b0e16',
          strokeWeight: 2,
          scale: 8
        }
      });
      marker.addListener('click', () => {
        if (!mapInfoRef.current) {
          mapInfoRef.current = new window.google.maps.InfoWindow();
        }
        mapInfoRef.current.setContent(
          `<div style="font-family: sans-serif; max-width: 220px;">
            <strong>${alertBadgeCopy[alert.severity] ?? 'Alert'}</strong><br/>
            ${alert.address}<br/>
            <span style="font-size: 12px;">${new Date(alert.date).toLocaleString()}</span>
          </div>`
        );
        mapInfoRef.current.open(mapInstanceRef.current, marker);
      });
      mapMarkersRef.current.set(alert.id, marker);
      bounds.extend({ lat: alert.lat, lng: alert.lng });
    });
    mapInstanceRef.current.fitBounds(bounds);
  }, [filteredAlerts, userLocation, defaultMapCenter]);


  const handleAlertFocus = (alert) => {
    setSelectedAlertId(alert.id);
    if (!mapInstanceRef.current || !window.google?.maps) return;
    const marker = mapMarkersRef.current.get(alert.id);
    if (marker) {
      mapInstanceRef.current.panTo(marker.getPosition());
      mapInstanceRef.current.setZoom(13);
      if (!mapInfoRef.current) {
        mapInfoRef.current = new window.google.maps.InfoWindow();
      }
      mapInfoRef.current.setContent(
        `<div style="font-family: sans-serif; max-width: 220px;">
          <strong>${alertBadgeCopy[alert.severity] ?? 'Alert'}</strong><br/>
          ${alert.address}<br/>
          <span style="font-size: 12px;">${new Date(alert.date).toLocaleString()}</span>
        </div>`
      );
      mapInfoRef.current.open(mapInstanceRef.current, marker);
    } else {
      mapInstanceRef.current.panTo({ lat: alert.lat, lng: alert.lng });
      mapInstanceRef.current.setZoom(13);
    }
  };

  const handleClearAlert = async (alertId) => {
    if (!user) return;
    try {
      await setDoc(
        doc(db, "users", user.uid, "emergencyLocations", alertId),
        { clearedAt: serverTimestamp() },
        { merge: true }
      );
    } catch (error) {
      console.error('Failed to clear alert', error);
      setAlertStatus(error?.message ?? 'Unable to clear alert.');
    }
  };

  const fetchAlertLocations = () => {
    // Firestore onSnapshot handles real-time updates automatically.
    // We just provide visual feedback that the system is connected.
    setAlertStatus('Syncing...');
    setTimeout(() => {
      setAlertStatus(alertLocations.length ? '' : 'No emergency locations yet.');
    }, 800);
  };

  const handleLogin = async () => {
    setIsLoggingIn(true);
    setAuthError('');
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
    } catch (error) {
      console.error("Login failed", error);
      setAuthError(error?.message ?? "Google sign-in failed.");
      setIsLoggingIn(false);
    }
  };

  const handleEmailAuth = async (mode) => {
    const trimmedEmail = email.trim();
    if (!trimmedEmail || !password) {
      setAuthError("Enter email and password.");
      return;
    }
    setIsLoggingIn(true);
    setAuthError('');
    try {
      if (mode === 'signup') {
        await createUserWithEmailAndPassword(auth, trimmedEmail, password);
      } else {
        await signInWithEmailAndPassword(auth, trimmedEmail, password);
      }
    } catch (error) {
      console.error("Email auth failed", error);
      setAuthError(error?.message ?? "Email sign-in failed.");
      setIsLoggingIn(false);
    }
  };

  const handlePasswordReset = async () => {
    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setAuthError("Enter your email to reset your password.");
      return;
    }
    setIsLoggingIn(true);
    setAuthError('');
    try {
      await sendPasswordResetEmail(auth, trimmedEmail);
      setAuthError("Password reset email sent.");
    } catch (error) {
      console.error("Password reset failed", error);
      setAuthError(error?.message ?? "Password reset failed.");
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handlePasswordResetForUser = async () => {
    if (!user?.email) {
      setSettingsStatus("No email address on file.");
      return;
    }
    setSettingsStatus("Sending password reset email...");
    try {
      await sendPasswordResetEmail(auth, user.email);
      setSettingsStatus(`Password reset sent to ${user.email}.`);
    } catch (error) {
      console.error("Password reset failed", error);
      setSettingsStatus(error?.message ?? "Password reset failed.");
    }
  };

  const handleProfileSave = async () => {
    if (!user) return;
    setIsSavingProfile(true);
    setProfileStatus("Saving profile...");
    try {
      const payload = {
        ownerName: profile.ownerName || '',
        avatarUrl: profile.avatarUrl || '',
        phoneNumber: profile.phoneNumber || '',
        email: profile.email || user.email || ''
      };
      if (payload.email) {
        payload.emailLowercase = payload.email.toLowerCase();
      }
      await setDoc(doc(db, "users", user.uid), payload, { merge: true });
      setProfileStatus("Profile updated.");
    } catch (error) {
      console.error("Profile update failed", error);
      setProfileStatus(error?.message ?? "Profile update failed.");
    } finally {
      setIsSavingProfile(false);
    }
  };

  const resetContactForm = () => {
    setContactForm({
      displayName: '',
      phoneNumber: '',
      email: '',
      additionalPhones: '',
      additionalEmails: '',
      escalationTier: 'EMERGENCY',
      includeLocation: true,
      autoCall: false,
      allowRemoteOverride: true,
      allowRemoteSoundChange: false
    });
    setEditingContactId(null);
    setContactStatus('');
  };

  const handleEditContact = (contact) => {
    setContactForm({
      displayName: contact.displayName ?? '',
      phoneNumber: contact.phoneNumber ?? '',
      email: contact.email ?? '',
      additionalPhones: (contact.additionalPhones || []).join(', '),
      additionalEmails: (contact.additionalEmails || []).join(', '),
      escalationTier: contact.escalationTier ?? 'EMERGENCY',
      includeLocation: contact.includeLocation ?? true,
      autoCall: contact.autoCall ?? false,
      allowRemoteOverride: contact.allowRemoteOverride ?? true,
      allowRemoteSoundChange: contact.allowRemoteSoundChange ?? false
    });
    setEditingContactId(contact.id);
    setContactStatus('');
    setActivePanel('pulselink');
  };

  const handleSaveContact = async () => {
    if (!user) return;
    if (!contactForm.displayName.trim()) {
      setContactStatus("Display name is required.");
      return;
    }
    setContactStatus("Saving contact...");
    try {
      const payload = {
        displayName: contactForm.displayName.trim(),
        phoneNumber: contactForm.phoneNumber.trim(),
        email: contactForm.email.trim() || null,
        additionalPhones: parseList(contactForm.additionalPhones),
        additionalEmails: parseList(contactForm.additionalEmails),
        escalationTier: contactForm.escalationTier,
        includeLocation: contactForm.includeLocation,
        autoCall: contactForm.autoCall,
        allowRemoteOverride: contactForm.allowRemoteOverride,
        allowRemoteSoundChange: contactForm.allowRemoteSoundChange,
        contactOrder: editingContactId
          ? contacts.find(c => c.id === editingContactId)?.contactOrder ?? contacts.length
          : contacts.length,
        updatedAt: serverTimestamp()
      };

      const newDocId = buildContactDocId(payload);
      if (editingContactId && editingContactId !== newDocId) {
        await deleteDoc(doc(db, "users", user.uid, "trustedContacts", editingContactId));
      }
      await setDoc(doc(db, "users", user.uid, "trustedContacts", editingContactId || newDocId), payload, { merge: true });
      setContactStatus("Contact saved.");
      resetContactForm();
    } catch (error) {
      console.error("Contact save failed", error);
      setContactStatus(error?.message ?? "Contact save failed.");
    }
  };

  const handleDeleteContact = async (contactId) => {
    if (!user) return;
    setContactStatus("Removing contact...");
    try {
      await deleteDoc(doc(db, "users", user.uid, "trustedContacts", contactId));
      setContactStatus("Contact removed.");
    } catch (error) {
      console.error("Delete contact failed", error);
      setContactStatus(error?.message ?? "Delete failed.");
    }
  };

  const handleApplyPreset = async (presetTheme) => {
    if (!user) return;
    const normalized = normalizeTheme(presetTheme);
    setThemeStatus("Updating theme...");
    try {
      await setDoc(
        doc(db, "users", user.uid),
        { themePreferences: normalized, themeUpdatedAt: serverTimestamp() },
        { merge: true }
      );
      setThemePrefs(normalized);
      setThemeStatus("Theme synced.");
    } catch (error) {
      console.error("Theme update failed", error);
      setThemeStatus(error?.message ?? "Theme update failed.");
    }
  };

  const handleImportPublicTheme = async (themeDoc) => {
    if (!themeDoc?.theme) return;
    await handleApplyPreset(themeDoc.theme);
    setThemeGalleryStatus(`Imported "${themeDoc.name}".`);
  };

  const handlePublishTheme = async () => {
    if (!user) return;
    const name = themePublishForm.name.trim();
    if (!name) {
      setThemePublishStatus("Theme name is required.");
      return;
    }
    setThemePublishStatus("Publishing theme...");
    const backgroundImageUrl = themePublishForm.backgroundImageUrl.trim();
    const normalized = normalizeTheme({
      ...themePrefs,
      backgroundImageUrl: backgroundImageUrl || themePrefs.backgroundImageUrl || null
    });
    const iconOverrides = normalized.iconOverrides ?? {};
    const hasImages = Boolean(normalized.backgroundImageUrl) ||
      Object.values(iconOverrides).some((value) => (value ?? '').toString().trim().length > 0);
    const anonymous = themePublishForm.anonymous;
    const authorName = anonymous
      ? null
      : (themePublishForm.authorName.trim() || profile.ownerName || null);
    const authorHandle = anonymous
      ? null
      : (themePublishForm.authorHandle.trim() || null);
    const payload = {
      name,
      nameLowercase: name.toLowerCase(),
      ownerUid: user.uid,
      anonymous,
      authorName,
      authorHandle,
      hasImages,
      status: hasImages ? "pending" : "approved",
      theme: normalized,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    };
    try {
      const targetCollection = hasImages ? "themes_submissions" : "themes_public";
      await addDoc(collection(db, targetCollection), payload);
      setThemePublishStatus(
        hasImages ? "Submitted for approval (images require review)." : "Theme published."
      );
      setThemePublishForm((prev) => ({
        ...prev,
        name: '',
        authorHandle: '',
        anonymous: false,
        backgroundImageUrl: ''
      }));
    } catch (error) {
      console.error("Theme publish failed", error);
      setThemePublishStatus(error?.message ?? "Theme publish failed.");
    }
  };

  const handleRemoteSettingsSave = async () => {
    if (!user) return;
    setSettingsStatus("Saving settings...");
    try {
      await setDoc(doc(db, "users", user.uid), {
        remoteWebAccessEnabled: remoteSettings.remoteWebAccessEnabled,
        autoUpdateContactInfo: remoteSettings.autoUpdateContactInfo,
        timeFormat: remoteSettings.timeFormat,
        settingsUpdatedAt: serverTimestamp()
      }, { merge: true });
      setSettingsStatus("Settings updated.");
    } catch (error) {
      console.error("Settings update failed", error);
      setSettingsStatus(error?.message ?? "Settings update failed.");
    }
  };

  const handleDeleteAccount = async () => {
    if (!user) return;
    if (!window.confirm("Delete your account and all cloud data? This cannot be undone.")) {
      return;
    }
    setDeleteStatus("Requesting account deletion...");
    try {
      const callable = httpsCallable(functions, "deleteAccount");
      await callable();
      setDeleteStatus("Account deletion requested.");
      await signOut(auth);
    } catch (error) {
      console.error("Delete account failed", error);
      setDeleteStatus(error?.message ?? "Delete account failed.");
    }
  };

  const handleDeleteAccountData = async () => {
    if (!user) return;
    if (!window.confirm("Clear synced messages and trusted contacts from the cloud?")) {
      return;
    }
    setDeleteStatus("Deleting account data...");
    try {
      const batch = writeBatch(db);
      const contactsSnap = await getDocs(collection(db, "users", user.uid, "trustedContacts"));
      contactsSnap.docs.forEach(docSnap => batch.delete(docSnap.ref));

      const outboxSnap = await getDocs(collection(db, "users", user.uid, "outbox"));
      outboxSnap.docs.forEach(docSnap => batch.delete(docSnap.ref));

      const threadsSnap = await getDocs(collection(db, "users", user.uid, "synced_threads"));
      threadsSnap.docs.forEach(docSnap => batch.delete(docSnap.ref));

      await batch.commit();

      for (const thread of threadsSnap.docs) {
        const messagesSnap = await getDocs(collection(db, "users", user.uid, "synced_threads", thread.id, "messages"));
        if (messagesSnap.empty) continue;
        const messageBatch = writeBatch(db);
        messagesSnap.docs.forEach(messageDoc => messageBatch.delete(messageDoc.ref));
        await messageBatch.commit();
      }

      setDeleteStatus("Cloud data cleared.");
    } catch (error) {
      console.error("Delete data failed", error);
      setDeleteStatus(error?.message ?? "Delete data failed.");
    }
  };

  const handleLogout = async () => {
    await signOut(auth);
    setSelectedThread(null);
    setComposeAddress('');
    setComposeBody('');
    setSendStatus('');
    setActivePanel('home');
  };

  const handleSendMessage = async () => {
    if (!user) return;
    const address = composeAddress.trim();
    const body = composeBody.trim();
    if (!address || !body) {
      setSendStatus("Add a phone number and message.");
      return;
    }
    setIsSending(true);
    setSendStatus('');
    try {
      await addDoc(collection(db, "users", user.uid, "outbox"), {
        address,
        body,
        createdAt: serverTimestamp(),
        source: "web"
      });
      setComposeBody('');
      setSendStatus("Queued for sending from your device.");
    } catch (error) {
      console.error("Send failed", error);
      setSendStatus("Send failed. Try again.");
    } finally {
      setIsSending(false);
    }
  };

  if (!user) {
    return (
      <div className="app-shell" style={themeVars}>
        <div className="container login-container">
          <div className="login-card">
            <img src={logo} alt="PulseLink Pro" className="brand-logo" />
            <h1>PulseLink Web</h1>
            <p>Login to access your messages</p>
            <div className="login-form">
              <label className="login-field">
                Email
                <input
                  className="login-input"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  autoComplete="email"
                />
              </label>
              <div className="login-field">
                <label htmlFor="login-password">Password</label>
                <div className="password-input-wrapper">
                  <input
                    id="login-password"
                    className="login-input"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="password"
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="password-toggle-btn"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? "Hide password" : "Show password"}
                  >
                    {showPassword ? (
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                        <line x1="1" y1="1" x2="23" y2="23"></line>
                      </svg>
                    ) : (
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                    )}
                  </button>
                </div>
              </div>
              {authError && <div className="auth-error">{authError}</div>}
              <div className="login-actions">
                <button
                  onClick={() => handleEmailAuth('signin')}
                  disabled={isLoggingIn}
                  aria-busy={isLoggingIn}
                  className="primary-btn"
                >
                  {isLoggingIn ? 'Signing in...' : 'Sign in'}
                </button>
                <button
                  onClick={() => handleEmailAuth('signup')}
                  disabled={isLoggingIn}
                  className="secondary-btn"
                >
                  Create account
                </button>
              </div>
              <button
                type="button"
                className="link-button"
                onClick={handlePasswordReset}
                disabled={isLoggingIn}
              >
                Forgot password?
              </button>
              <div className="login-divider">or</div>
              <button
                onClick={handleLogin}
                disabled={isLoggingIn}
                aria-busy={isLoggingIn}
                className="primary-btn"
              >
                {isLoggingIn ? 'Signing in...' : 'Sign in with Google'}
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="app-shell" style={themeVars}>
      <div className="app-container">
        <div className="sidebar">
          <div className="sidebar-header">
            <div className="sidebar-brand">
              <img src={logo} alt="PulseLink Pro" className="brand-logo small" />
              <div>
                <div className="brand-title">PulseLink Pro</div>
                <div className="brand-subtitle">Web Command Center</div>
              </div>
            </div>
            <div className="sidebar-actions">
              {activePanel === 'beacon' && (
                <button
                  onClick={() => {
                    setActivePanel('beacon');
                    setSelectedThread(null);
                    setComposeAddress('');
                    setComposeBody('');
                    setSendStatus('');
                  }}
                  className="secondary-btn"
                  aria-label="Start new conversation"
                >
                  New
                </button>
              )}
              <button onClick={handleLogout} className="ghost-btn">Logout</button>
            </div>
          </div>
          <div className="sidebar-nav">
            <button
              className={`nav-item ${activePanel === 'home' ? 'active' : ''}`}
              onClick={() => setActivePanel('home')}
              aria-current={activePanel === 'home' ? 'page' : undefined}
            >
              Home
            </button>
            <button
              className={`nav-item ${activePanel === 'beacon' ? 'active' : ''}`}
              onClick={() => setActivePanel('beacon')}
              aria-current={activePanel === 'beacon' ? 'page' : undefined}
            >
              Beacon Inbox
            </button>
            <button
              className={`nav-item ${activePanel === 'map' ? 'active' : ''}`}
              onClick={() => setActivePanel('map')}
            >
              Emergency Map
            </button>
            <button
              className={`nav-item ${activePanel === 'themes' ? 'active' : ''}`}
              onClick={() => setActivePanel('themes')}
              aria-current={activePanel === 'themes' ? 'page' : undefined}
            >
              Themes
            </button>
            <button
              className={`nav-item ${activePanel === 'pulselink' ? 'active' : ''}`}
              onClick={() => setActivePanel('pulselink')}
              aria-current={activePanel === 'pulselink' ? 'page' : undefined}
            >
              PulseLink
            </button>
            <button
              className={`nav-item ${activePanel === 'contacts' ? 'active' : ''}`}
              onClick={() => setActivePanel('contacts')}
              aria-current={activePanel === 'contacts' ? 'page' : undefined}
            >
              Contacts
            </button>
            <button
              className={`nav-item ${activePanel === 'settings' ? 'active' : ''}`}
              onClick={() => setActivePanel('settings')}
              aria-current={activePanel === 'settings' ? 'page' : undefined}
            >
              Settings
            </button>
          </div>
          {activePanel === 'beacon' ? (
            <div className="thread-list">
              {threads.length === 0 ? (
                <div className="sidebar-placeholder">
                  <div className="sidebar-tip muted">No conversations found.</div>
                </div>
              ) : (
                threads.map(thread => (
                  <ThreadItem
                    key={thread.id}
                    thread={thread}
                    isActive={selectedThread?.id === thread.id}
                    onSelect={setSelectedThread}
                    showPreviews={showPreviews}
                  />
                ))
              )}
            </div>
          ) : (
            <div className="sidebar-placeholder">
              <div className="sidebar-tip">Use the tiles on Home to jump into PulseLink or Beacon.</div>
              <div className="sidebar-tip muted">Theme and settings sync to your device.</div>
            </div>
          )}
        </div>
        <div className="main-content">
          {activePanel === 'home' && (
            <div className="home-panel">
              <div className="home-hero">
                <h2>Welcome back</h2>
                <p>Choose what you want to manage on PulseLink Web.</p>
              </div>
              <div className="home-grid">
                <button className="home-card" onClick={() => setActivePanel('pulselink')}>
                  <div className="home-icon pulselink">
                    <img src={logo} alt="PulseLink" />
                  </div>
                  <h3>PulseLink</h3>
                  <p>Update your profile and trusted contacts.</p>
                </button>
                <button className="home-card" onClick={() => setActivePanel('contacts')}>
                  <div className="home-icon pulselink">
                    <img src={logo} alt="PulseLink contacts" />
                  </div>
                  <h3>Contacts</h3>
                  <p>Browse every trusted contact at a glance.</p>
                </button>
                <button className="home-card" onClick={() => setActivePanel('beacon')}>
                  <div className="home-icon beacon">
                    <img src={beaconLogo} alt="Beacon" />
                  </div>
                  <h3>Beacon Inbox</h3>
                  <p>View SMS synced from your phone.</p>
                </button>
                <button className="home-card" onClick={() => setActivePanel('map')}>
                  <div className="home-icon pulselink">
                    <img src={logo} alt="PulseLink map" />
                  </div>
                  <h3>Emergency Map</h3>
                  <p>Track shared locations from PulseLink alerts.</p>
                </button>
                <button className="home-card" onClick={() => setActivePanel('themes')}>
                  <div className="home-icon pulselink">
                    <img src={logo} alt="PulseLink themes" />
                  </div>
                  <h3>Theme Gallery</h3>
                  <p>Browse, import, and publish custom themes.</p>
                </button>
              </div>
            </div>
          )}

          {activePanel === 'pulselink' && (
            <div className="pulselink-panel">
              <div className="panel-header">
                <h3>PulseLink</h3>
                <p>Manage trusted contacts and your public profile.</p>
              </div>
              <div className="pulselink-grid">
                <div className="settings-card">
                  <h4>Public profile</h4>
                  <label className="login-field">
                    Display name
                    <input
                      className="login-input"
                      value={profile.ownerName}
                      onChange={(e) => setProfile((prev) => ({ ...prev, ownerName: e.target.value }))}
                    />
                  </label>
                  <label className="login-field">
                    Email (profile)
                    <input
                      className="login-input"
                      value={profile.email}
                      onChange={(e) => setProfile((prev) => ({ ...prev, email: e.target.value }))}
                    />
                  </label>
                  <label className="login-field">
                    Phone
                    <input
                      className="login-input"
                      value={profile.phoneNumber}
                      onChange={(e) => setProfile((prev) => ({ ...prev, phoneNumber: e.target.value }))}
                    />
                  </label>
                  <label className="login-field">
                    Avatar URL
                    <input
                      className="login-input"
                      value={profile.avatarUrl}
                      onChange={(e) => setProfile((prev) => ({ ...prev, avatarUrl: e.target.value }))}
                    />
                  </label>
                  <button
                    className="primary-btn"
                    type="button"
                    onClick={handleProfileSave}
                    disabled={isSavingProfile}
                  >
                    {isSavingProfile ? 'Saving...' : 'Save profile'}
                  </button>
                  {profileStatus && <div className="settings-status">{profileStatus}</div>}
                </div>
                <div className="settings-card">
                  <h4>Trusted contacts</h4>
                  <div className="contact-list">
                    {contacts.map((contact) => (
                      <div key={contact.id} className="contact-row">
                        <div className="contact-main">
                          <div className="contact-name">{contact.displayName}</div>
                          <div className="contact-meta">
                            {contact.phoneNumber || contact.email || 'No phone or email'}
                          </div>
                        </div>
                        <div className="contact-actions">
                          <button
                            className="secondary-btn"
                            onClick={() => handleEditContact(contact)}
                            aria-label={`Edit ${contact.displayName}`}
                          >
                            Edit
                          </button>
                          <button
                            className="ghost-btn"
                            onClick={() => handleDeleteContact(contact.id)}
                            aria-label={`Remove ${contact.displayName}`}
                          >
                            Remove
                          </button>
                        </div>
                      </div>
                    ))}
                    {contacts.length === 0 && (
                      <div className="settings-note">No trusted contacts yet.</div>
                    )}
                  </div>
                  {contactStatus && <div className="settings-status">{contactStatus}</div>}
                </div>
                <div className="settings-card">
                  <h4>{editingContactId ? 'Edit trusted contact' : 'Add trusted contact'}</h4>
                  <label className="login-field">
                    Name
                    <input
                      className="login-input"
                      value={contactForm.displayName}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, displayName: e.target.value }))}
                    />
                  </label>
                  <label className="login-field">
                    Phone
                    <input
                      className="login-input"
                      value={contactForm.phoneNumber}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, phoneNumber: e.target.value }))}
                    />
                  </label>
                  <label className="login-field">
                    Email
                    <input
                      className="login-input"
                      value={contactForm.email}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, email: e.target.value }))}
                    />
                  </label>
                  <label className="login-field">
                    Extra phones (comma separated)
                    <input
                      className="login-input"
                      value={contactForm.additionalPhones}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, additionalPhones: e.target.value }))}
                    />
                  </label>
                  <label className="login-field">
                    Extra emails (comma separated)
                    <input
                      className="login-input"
                      value={contactForm.additionalEmails}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, additionalEmails: e.target.value }))}
                    />
                  </label>
                  <label className="login-field">
                    Escalation tier
                    <select
                      className="login-input"
                      value={contactForm.escalationTier}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, escalationTier: e.target.value }))}
                    >
                      <option value="EMERGENCY">Emergency</option>
                      <option value="CHECK_IN">Check-in</option>
                    </select>
                  </label>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={contactForm.includeLocation}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, includeLocation: e.target.checked }))}
                    />
                    Share location with this contact
                  </label>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={contactForm.autoCall}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, autoCall: e.target.checked }))}
                    />
                    Auto-call after alert
                  </label>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={contactForm.allowRemoteOverride}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, allowRemoteOverride: e.target.checked }))}
                    />
                    Allow remote overrides
                  </label>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={contactForm.allowRemoteSoundChange}
                      onChange={(e) => setContactForm((prev) => ({ ...prev, allowRemoteSoundChange: e.target.checked }))}
                    />
                    Allow remote sound changes
                  </label>
                  <div className="contact-actions">
                    <button className="primary-btn" onClick={handleSaveContact}>
                      {editingContactId ? 'Update contact' : 'Add contact'}
                    </button>
                    <button className="ghost-btn" onClick={resetContactForm}>
                      Clear
                    </button>
                  </div>
                  {contactStatus && <div className="settings-status">{contactStatus}</div>}
                </div>
              </div>
            </div>
          )}

          {activePanel === 'contacts' && (
            <div className="contacts-panel">
              <div className="panel-header">
                <h3>Contacts</h3>
                <p>Browse all trusted contacts synced from PulseLink.</p>
              </div>
              <div className="contacts-toolbar">
                <div className="contact-count">
                  {filteredContacts.length} contact{filteredContacts.length === 1 ? '' : 's'}
                </div>
                <input
                  className="login-input contact-search"
                  placeholder="Search by name, phone, or email"
                  value={contactSearch}
                  onChange={(e) => setContactSearch(e.target.value)}
                />
              </div>
              <div className="contact-list contact-list--full">
                {filteredContacts.map((contact) => {
                  const extraPhones = Array.isArray(contact.additionalPhones)
                    ? contact.additionalPhones
                    : [];
                  const extraEmails = Array.isArray(contact.additionalEmails)
                    ? contact.additionalEmails
                    : [];
                  const extras = [...extraPhones, ...extraEmails].filter(Boolean).join(' • ');
                  const tierLabel = contact.escalationTier === 'CHECK_IN' ? 'Check-in' : 'Emergency';
                  return (
                    <div key={contact.id} className="contact-row contact-row--stacked">
                      <div className="contact-main">
                        <div className="contact-name">{contact.displayName || 'Unnamed contact'}</div>
                        <div className="contact-meta">
                          {contact.phoneNumber || contact.email || 'No phone or email'}
                        </div>
                        {extras && <div className="contact-extra">{extras}</div>}
                        <div className="contact-tags">
                          <span className="contact-tag">{tierLabel}</span>
                          {contact.includeLocation && <span className="contact-tag">Location</span>}
                          {contact.autoCall && <span className="contact-tag">Auto call</span>}
                          {contact.allowRemoteOverride && <span className="contact-tag">DND override</span>}
                        </div>
                      </div>
                      <div className="contact-actions">
                        <button
                          className="secondary-btn"
                          onClick={() => {
                            handleEditContact(contact);
                            setActivePanel('pulselink');
                          }}
                        >
                          Edit
                        </button>
                      </div>
                    </div>
                  );
                })}
                {filteredContacts.length === 0 && (
                  <div className="settings-note">
                    {contactSearch.trim()
                      ? 'No contacts match that search.'
                      : 'No trusted contacts yet.'}
                  </div>
                )}
              </div>
            </div>
          )}

          {activePanel === 'map' && (
            <div className="map-panel">
              <div className="panel-header">
                <h3>Emergency map</h3>
                <p>Locations parsed from PulseLink alert messages synced to this account.</p>
              </div>
              <div className="map-controls">
                <button
                  className="secondary-btn"
                  onClick={() => window.location.reload()}
                  aria-label="Reload page"
                ></button>
                <button className="ghost-btn" onClick={() => setActivePanel('home')}>
                  Back to Home
                </button>
                <button className="secondary-btn" onClick={fetchAlertLocations}>
                  Refresh
                </button>
                <label className="settings-toggle">
                  <input
                    type="checkbox"
                    checked={incomingOnly}
                    onChange={(e) => setIncomingOnly(e.target.checked)}
                  />
                  Incoming only
                </label>
                <label className="login-field map-filter">
                  Alert type
                  <select
                    className="login-input"
                    value={severityFilter}
                    onChange={(e) => setSeverityFilter(e.target.value)}
                  >
                    <option value="emergency">Emergency</option>
                    <option value="check_in">Check-in</option>
                    <option value="non_urgent">Other</option>
                    <option value="all">All</option>
                  </select>
                </label>
                {(alertStatus || geoStatus) && (
                  <div className="map-status-text">
                    {alertStatus && <div>{alertStatus}</div>}
                    {geoStatus && <div>{geoStatus}</div>}
                  </div>
                )}
              </div>
              <div className="map-grid">
                <div className="map-card">
                  <div className="map-canvas" ref={mapRef} />
                  {!mapsApiKey && (
                    <div className="map-fallback">
                      <div className="map-fallback-title">Maps key required</div>
                      <p>Set VITE_GOOGLE_MAPS_API_KEY in web/.env.local to load the map view.</p>
                    </div>
                  )}
                  {mapStatus && <div className="map-status">{mapStatus}</div>}
                </div>
                <div className="map-list">
                  {filteredAlerts.map((alert) => (
                    <div
                      key={alert.id}
                      className={`map-item ${selectedAlertId === alert.id ? 'active' : ''}`}
                      onClick={() => handleAlertFocus(alert)}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          event.preventDefault();
                          handleAlertFocus(alert);
                        }
                      }}
                    >
                      <div className="map-item-header">
                        <div className="map-item-title">{alert.address}</div>
                        <span
                          className="map-badge"
                          style={{ background: alertBadgeColor[alert.severity] ?? alertBadgeColor.non_urgent }}
                        >
                          {alertBadgeCopy[alert.severity] ?? 'Alert'}
                        </span>
                      </div>
                      <div className="map-item-meta">{new Date(alert.date).toLocaleString()}</div>
                      <div className="map-item-snippet">{buildAlertSnippet(alert.body)}</div>
                      <div className="map-item-actions">
                        <button
                          className="secondary-btn"
                          type="button"
                          onClick={(event) => {
                            event.stopPropagation();
                            handleClearAlert(alert.id);
                          }}
                        >
                          Clear
                        </button>
                      </div>
                    </div>
                  ))}
                  {filteredAlerts.length === 0 && (
                    <div className="map-empty">
                      <div className="map-empty-title">No emergencies recently — that’s good news.</div>
                      <div className="map-empty-sub">
                        {userLocation ? 'Showing your location on the map.' : 'We’ll center the map once your location is available.'}
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {activePanel === 'themes' && (
            <div className="themes-panel">
              <div className="panel-header">
                <h3>Theme Gallery</h3>
                <p>Browse community themes or publish your own. Image-based themes require approval.</p>
              </div>
              <div className="themes-grid">
                <div className="settings-card themes-card">
                  <div className="themes-toolbar">
                    <label className="login-field">
                      Search themes
                      <input
                        className="login-input"
                        value={themeSearch}
                        onChange={(e) => setThemeSearch(e.target.value)}
                        placeholder="Search by name or creator"
                      />
                    </label>
                    <button className="secondary-btn" type="button" onClick={() => setThemeSearch('')}>
                      Clear
                    </button>
                  </div>
                  <div className="theme-gallery-grid">
                    {filteredThemes.map((themeDoc) => {
                      const previewTheme = normalizeTheme(themeDoc.theme || {});
                      const previewStyle = buildThemePreviewStyle(previewTheme);
                      const authorLabel = themeDoc.anonymous
                        ? 'Anonymous'
                        : (themeDoc.authorHandle || themeDoc.authorName || 'Community');
                      return (
                        <div key={themeDoc.id} className="theme-card">
                          <div className="theme-preview" style={previewStyle} />
                          <div className="theme-meta">
                            <div className="theme-name">{themeDoc.name || 'Untitled'}</div>
                            <div className="theme-author">{authorLabel}</div>
                          </div>
                          <button
                            className="primary-btn"
                            type="button"
                            onClick={() => handleImportPublicTheme(themeDoc)}
                          >
                            Import
                          </button>
                        </div>
                      );
                    })}
                    {filteredThemes.length === 0 && (
                      <div className="theme-empty">No themes yet. Be the first to publish!</div>
                    )}
                  </div>
                  {themeGalleryStatus && <div className="settings-status">{themeGalleryStatus}</div>}
                </div>
                <div className="settings-card themes-card">
                  <h4>Publish your theme</h4>
                  <label className="login-field">
                    Theme name
                    <input
                      className="login-input"
                      value={themePublishForm.name}
                      onChange={(e) => setThemePublishForm((prev) => ({ ...prev, name: e.target.value }))}
                      placeholder="e.g., Aurora Drift"
                    />
                  </label>
                  <label className="login-field">
                    Creator name (optional)
                    <input
                      className="login-input"
                      value={themePublishForm.authorName}
                      onChange={(e) => setThemePublishForm((prev) => ({ ...prev, authorName: e.target.value }))}
                      placeholder="Leave blank for profile name"
                    />
                  </label>
                  <label className="login-field">
                    Creator handle (optional)
                    <input
                      className="login-input"
                      value={themePublishForm.authorHandle}
                      onChange={(e) => setThemePublishForm((prev) => ({ ...prev, authorHandle: e.target.value }))}
                      placeholder="@pulseartist"
                    />
                  </label>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={themePublishForm.anonymous}
                      onChange={(e) => setThemePublishForm((prev) => ({ ...prev, anonymous: e.target.checked }))}
                    />
                    Publish anonymously
                  </label>
                  <label className="login-field">
                    Background image URL (optional)
                    <input
                      className="login-input"
                      value={themePublishForm.backgroundImageUrl}
                      onChange={(e) => setThemePublishForm((prev) => ({ ...prev, backgroundImageUrl: e.target.value }))}
                      placeholder="https://..."
                    />
                  </label>
                  <p className="settings-note">
                    Suggested max: 1920x1080 and under 1.5MB. Image themes require approval.
                  </p>
                  <button className="primary-btn" type="button" onClick={handlePublishTheme}>
                    Publish theme
                  </button>
                  {themePublishStatus && <div className="settings-status">{themePublishStatus}</div>}
                </div>
              </div>
            </div>
          )}

          {activePanel === 'settings' && (
            <div className="settings-panel">
              <div className="settings-header">
                <h3>Settings</h3>
                <p>Manage account, theme sync, and shared preferences.</p>
              </div>
              <div className="settings-grid">
                <div className="settings-card">
                  <h4>Account</h4>
                  <div className="settings-row">
                    <span className="settings-label">Signed in as</span>
                    <span className="settings-value">{user.email || 'Unknown'}</span>
                  </div>
                  <div className="settings-row">
                    <span className="settings-label">User ID</span>
                    <span className="settings-value mono">{user.uid}</span>
                  </div>
                  <button className="secondary-btn" type="button" onClick={handlePasswordResetForUser}>
                    Send password reset email
                  </button>
                  {settingsStatus && <div className="settings-status">{settingsStatus}</div>}
                </div>
                <div className="settings-card">
                  <h4>Web preferences</h4>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={showPreviews}
                      onChange={(e) => setShowPreviews(e.target.checked)}
                    />
                    Show message previews
                  </label>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={autoScroll}
                      onChange={(e) => setAutoScroll(e.target.checked)}
                    />
                    Auto-scroll to latest message
                  </label>
                  <p className="settings-note">
                    Preferences apply to this browser only.
                  </p>
                </div>
                <div className="settings-card">
                  <h4>Theme sync</h4>
                  <div className="theme-grid">
                    {themePresets.map((preset) => (
                      <button
                        key={preset.name}
                        className="theme-chip"
                        onClick={() => handleApplyPreset(preset.theme)}
                      >
                        <span className="theme-dot" style={{ background: preset.theme.primaryColor }} />
                        {preset.name}
                      </button>
                    ))}
                  </div>
                  <div className="theme-editor">
                    <label className="login-field">
                      Primary color
                      <input
                        className="login-input"
                        type="color"
                        value={themePrefs.primaryColor}
                        onChange={(e) => setThemePrefs((prev) => ({ ...prev, primaryColor: e.target.value }))}
                      />
                    </label>
                    <label className="login-field">
                      Background
                      <input
                        className="login-input"
                        type="color"
                        value={themePrefs.backgroundColor}
                        onChange={(e) => setThemePrefs((prev) => ({ ...prev, backgroundColor: e.target.value }))}
                      />
                    </label>
                    <label className="login-field">
                      Top bar
                      <input
                        className="login-input"
                        type="color"
                        value={themePrefs.topBarColor}
                        onChange={(e) => setThemePrefs((prev) => ({ ...prev, topBarColor: e.target.value }))}
                      />
                    </label>
                    <label className="login-field">
                      Bubble outgoing
                      <input
                        className="login-input"
                        type="color"
                        value={themePrefs.bubbleOutgoing}
                        onChange={(e) => setThemePrefs((prev) => ({ ...prev, bubbleOutgoing: e.target.value }))}
                      />
                    </label>
                    <label className="login-field">
                      Bubble incoming
                      <input
                        className="login-input"
                        type="color"
                        value={themePrefs.bubbleIncoming}
                        onChange={(e) => setThemePrefs((prev) => ({ ...prev, bubbleIncoming: e.target.value }))}
                      />
                    </label>
                    <label className="login-field theme-wide">
                      Background image URL
                      <input
                        className="login-input"
                        value={themePrefs.backgroundImageUrl ?? ''}
                        onChange={(e) => setThemePrefs((prev) => ({
                          ...prev,
                          backgroundImageUrl: e.target.value
                        }))}
                      />
                    </label>
                  </div>
                  <div className="theme-icon-grid">
                    {iconOverrideKeys.map(({ key, label }) => (
                      <label className="login-field" key={key}>
                        {label} icon URL
                        <input
                          className="login-input"
                          value={themePrefs.iconOverrides?.[key] ?? ''}
                          onChange={(e) => {
                            const value = e.target.value;
                            setThemePrefs((prev) => {
                              const next = { ...(prev.iconOverrides ?? {}) };
                              if (!value.trim()) {
                                delete next[key];
                              } else {
                                next[key] = value.trim();
                              }
                              return { ...prev, iconOverrides: next };
                            });
                          }}
                        />
                      </label>
                    ))}
                  </div>
                  <button className="primary-btn" type="button" onClick={() => handleApplyPreset(themePrefs)}>
                    Save theme
                  </button>
                  {themeStatus && <div className="settings-status">{themeStatus}</div>}
                </div>
                <div className="settings-card">
                  <h4>PulseLink settings</h4>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={remoteSettings.remoteWebAccessEnabled}
                      onChange={(e) => setRemoteSettings((prev) => ({ ...prev, remoteWebAccessEnabled: e.target.checked }))}
                    />
                    Enable remote web access
                  </label>
                  <label className="settings-toggle">
                    <input
                      type="checkbox"
                      checked={remoteSettings.autoUpdateContactInfo}
                      onChange={(e) => setRemoteSettings((prev) => ({ ...prev, autoUpdateContactInfo: e.target.checked }))}
                    />
                    Auto-update contact info
                  </label>
                  <label className="login-field">
                    Time format
                    <select
                      className="login-input"
                      value={remoteSettings.timeFormat}
                      onChange={(e) => setRemoteSettings((prev) => ({ ...prev, timeFormat: e.target.value }))}
                    >
                      <option value="AUTO">Auto</option>
                      <option value="TWELVE_HOUR">12-hour</option>
                      <option value="TWENTY_FOUR_HOUR">24-hour</option>
                    </select>
                  </label>
                  <button className="secondary-btn" type="button" onClick={handleRemoteSettingsSave}>
                    Save PulseLink settings
                  </button>
                </div>
                <div className="settings-card">
                  <h4>Account data</h4>
                  <p className="settings-note">
                    Delete account removes your login and all cloud data. Clear data keeps your login but deletes synced content.
                  </p>
                  <div className="contact-actions">
                    <button className="secondary-btn" type="button" onClick={handleDeleteAccountData}>
                      Clear cloud data
                    </button>
                    <button className="primary-btn" type="button" onClick={handleDeleteAccount}>
                      Delete account
                    </button>
                  </div>
                  {deleteStatus && <div className="settings-status">{deleteStatus}</div>}
                </div>
              </div>
            </div>
          )}

          {activePanel === 'beacon' && (
            <>
              {selectedThread ? (
                <>
                  <div className="chat-header">
                    <h3>{selectedThread.address}</h3>
                  </div>
                  <div className="messages-list">
                    {messages.map(msg => (
                      <MessageItem key={msg.id} msg={msg} showPreviews={showPreviews} />
                    ))}
                    <div ref={messagesEndRef} />
                  </div>
                </>
              ) : (
                <div className="empty-state">
                  <img src={beaconLogo} alt="Beacon" className="empty-logo" />
                  <div>Select a thread or start a new message</div>
                </div>
              )}
              <div className="composer">
                <div className="composer-row">
                  <label className="composer-label" htmlFor="compose-address">To</label>
                  <input
                    id="compose-address"
                    className="composer-input"
                    type="tel"
                    placeholder="Phone number"
                    value={composeAddress}
                    onChange={(e) => setComposeAddress(e.target.value)}
                  />
                </div>
                <div className="composer-row composer-actions">
                  <textarea
                    className="composer-textarea"
                    placeholder="Type a message..."
                    value={composeBody}
                    onChange={(e) => setComposeBody(e.target.value)}
                  />
                  <button
                    onClick={handleSendMessage}
                    disabled={isSending || isLoggingIn}
                    className="primary-btn"
                  >
                    {isSending ? "Sending..." : "Send"}
                  </button>
                </div>
                {sendStatus && <div className="compose-status">{sendStatus}</div>}
                <div className="compose-hint">
                  Messages are sent from your phone when it&apos;s online and signed in.
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
