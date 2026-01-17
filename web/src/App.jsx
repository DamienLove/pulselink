import { useState, useEffect, useMemo, useRef, memo, useCallback } from 'react';
import { auth, db, functions } from './firebase';
import DevTools from './DevTools';
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
import ringersongLogo from './assets/ringersong-logo.png';
import auroraBg from './assets/themes/aurora.svg';
import midnightBg from './assets/themes/midnight_oled.svg';
import sunsetBg from './assets/themes/sunset_fade.svg';
import forestBg from './assets/themes/forest_trail.svg';
import neonBg from './assets/themes/neon_noir.svg';
import goldBg from './assets/themes/gold_standard.svg';
import diamondBg from './assets/themes/diamond_dust.svg';
import obsidianBg from './assets/themes/obsidian_pro.svg';
import titaniumBg from './assets/themes/titanium_flow.svg';
import blueprintBg from './assets/themes/blueprint.svg';
import glitchBg from './assets/themes/glitch_stream.svg';
import oakBg from './assets/themes/legacy_oak.svg';
import eternalBg from './assets/themes/eternal_sky.svg';
import cyberMistBg from './assets/themes/cyber_mist.svg';
import deepOceanBg from './assets/themes/deep_ocean.svg';
import premiumAvatar from './assets/avatars/premium_crown.svg';
import proAvatar from './assets/avatars/pro_spark.svg';
import betaAvatar from './assets/avatars/beta_flask.svg';
import loyalAvatar from './assets/avatars/loyal_star.svg';

// Icons
const HomeIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>;
const MapIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"></polygon><line x1="8" y1="2" x2="8" y2="18"></line><line x1="16" y1="6" x2="16" y2="22"></line></svg>;
const ThemeIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="14.31" y1="8" x2="20.05" y2="17.94"></line><line x1="9.69" y1="8" x2="21.17" y2="8"></line><line x1="7.38" y1="12" x2="13.12" y2="2.06"></line><line x1="9.69" y1="16" x2="3.95" y2="6.06"></line><line x1="14.31" y1="16" x2="2.83" y2="16"></line><line x1="16.62" y1="12" x2="10.88" y2="21.94"></line></svg>;
const ContactIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>;
const SettingsIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>;
const TrashIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>;
const LinkIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path></svg>;
const CopyIcon = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>;
const CheckIcon = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>;
const SmartToyIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h4Z"/><rect x="4" y="10" width="16" height="8" rx="2"/><path d="M9 22v-4"/><path d="M15 22v-4"/><circle cx="8" cy="14" r="1" fill="currentColor"/><circle cx="16" cy="14" r="1" fill="currentColor"/></svg>;
const DeleteSweepIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>;
const EmailIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>;
const CarCrashIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 16H9m10 0h3v-3.15M17 12.89l1.45-1.45M9 16.02L6.68 18.34M4.34 20.68L2 23M9 12V8h6v4"/><rect x="4" y="16" width="10" height="6" rx="2"/><path d="M14 10l-2-3-2 3"/></svg>;
const CloudSyncIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22.61 16.95A5 5 0 0 0 18 10h-1.26a8 8 0 0 0-14.33 6"/><polyline points="1 20 5 20 5 16"/><path d="M1 20a9 9 0 0 0 9 9 9 9 0 0 0 4-10"/><polyline points="23 4 19 4 19 8"/><path d="M23 4a9 9 0 0 0-9-9 9 9 0 0 0-4 10"/></svg>;
const ExtensionIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20.5 11a2.5 2.5 0 0 1 0 5 2.5 2.5 0 0 1-5 0V11h5Z"/><path d="M8 11V6a2.5 2.5 0 0 1 5 0 2.5 2.5 0 0 1 0 5H8Z"/><path d="M11 8h5a2.5 2.5 0 0 1 0 5 2.5 2.5 0 0 1-5 0v-5Z"/><path d="M12 21a9 9 0 0 0 9-9 9 9 0 0 0-9-9 9 9 0 0 0-9 9 9 9 0 0 0 9 9Z"/></svg>;
const BoltIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"></path></svg>;
const StarIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>;
const LockIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>;
const MessageSquareIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>;
const SearchIcon = () => <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>;
const CloseIcon = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>;
const Spinner = ({ className = '', style = {} }) => (
  <svg className={`spinner ${className}`} style={style} viewBox="0 0 50 50" aria-hidden="true">
    <defs>
      <linearGradient id="spinner-grad" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stopColor="currentColor" stopOpacity="0" />
        <stop offset="100%" stopColor="currentColor" stopOpacity="1" />
      </linearGradient>
    </defs>
    <circle cx="25" cy="25" r="20" fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="4" />
    <circle cx="25" cy="25" r="20" fill="none" stroke="url(#spinner-grad)" strokeWidth="4" strokeDasharray="100" strokeDashoffset="80" strokeLinecap="round" />
    <circle cx="25" cy="25" r="14" fill="none" stroke="currentColor" strokeWidth="1" strokeDasharray="40" strokeOpacity="0.3" className="spinner-inner" style={{animationDirection: 'reverse', animationDuration: '2s'}} />
  </svg>
);

const CopyButton = ({ text, label = "Copy" }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  return (
    <button
      className="ghost-btn icon-only"
      type="button"
      onClick={handleCopy}
      aria-label={copied ? "Copied" : label}
      title={label}
      style={{ marginLeft: '8px' }}
    >
      {copied ? <CheckIcon /> : <CopyIcon />}
    </button>
  );
};

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
    aria-label={`Select conversation with ${thread.display_name || thread.address}${showPreviews && thread.snippet ? `, ${thread.snippet}` : ''}`}
  >
    <div className="thread-name">{thread.display_name || thread.address}</div>
    <div className="thread-snippet">{showPreviews ? thread.snippet : '••••••'}</div>
  </button>
), areThreadsEqual);

ThreadItem.displayName = 'ThreadItem';

const ThreadSkeleton = () => (
  <div className="skeleton-thread">
    <div className="skeleton-line" style={{ width: '40%' }}></div>
    <div className="skeleton-line short"></div>
  </div>
);

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
      {msg.imageUrl && (
        <div className="message-image-container">
          <img src={msg.imageUrl} alt="Attachment" className="message-image" loading="lazy" />
        </div>
      )}
      {showPreviews ? msg.body : '••••••'}
    </div>
    <div className="message-time">
      {new Date(msg.date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
    </div>
  </div>
), areMessagesEqual);

MessageItem.displayName = 'MessageItem';

// Bolt: Custom comparator for DeviceContactItem to handle object reference changes
const areDeviceContactsEqual = (prev, next) => {
  const p = prev.contact;
  const n = next.contact;
  if (p === n) return true; // Reference equality
  if (!p || !n) return false; // Null/undefined safety

  // Shallow checks for simple props
  if (p.id !== n.id) return false;
  if (p.displayName !== n.displayName) return false;
  if (p.phoneNumber !== n.phoneNumber) return false;
  if (p.email !== n.email) return false;

  // Deep check for array props (assuming arrays of strings)
  const pPhones = p.additionalPhones || [];
  const nPhones = n.additionalPhones || [];
  if (pPhones.length !== nPhones.length) return false;
  for (let i = 0; i < pPhones.length; i++) {
    if (pPhones[i] !== nPhones[i]) return false;
  }

  const pEmails = p.additionalEmails || [];
  const nEmails = n.additionalEmails || [];
  if (pEmails.length !== nEmails.length) return false;
  for (let i = 0; i < pEmails.length; i++) {
    if (pEmails[i] !== nEmails[i]) return false;
  }

  return true;
};

// Bolt: Optimized DeviceContactItem to prevent re-renders of the large contact list
const DeviceContactItem = memo(({ contact }) => {
  const extraPhones = Array.isArray(contact.additionalPhones)
    ? contact.additionalPhones
    : [];
  const extraEmails = Array.isArray(contact.additionalEmails)
    ? contact.additionalEmails
    : [];
  const extras = [...extraPhones, ...extraEmails].filter(Boolean).join(' • ');
  return (
    <div className="contact-row contact-row--stacked">
      <div className="contact-main">
        <div className="contact-name">{contact.displayName || 'Unnamed contact'}</div>
        <div className="contact-meta">
          {contact.phoneNumber || contact.email || 'No phone or email'}
        </div>
        {extras && <div className="contact-extra">{extras}</div>}
      </div>
    </div>
  );
}, areDeviceContactsEqual);

DeviceContactItem.displayName = 'DeviceContactItem';

// Bolt: Optimized TrustedContactRow to avoid re-rendering list on parent state changes
const TrustedContactRow = memo(({ contact, isConfirmingDelete, onEdit, onDeleteRequest, onDeleteConfirm, onDeleteCancel }) => (
  <div className="contact-row">
    <div className="contact-main">
      <div className="contact-name">{contact.displayName}</div>
      <div className="contact-meta">
        {contact.phoneNumber || contact.email || 'No phone or email'}
      </div>
    </div>
    <div className="contact-actions">
      <button
        className="secondary-btn"
        onClick={() => onEdit(contact)}
        aria-label={`Edit ${contact.displayName}`}
      >
        Edit
      </button>
      {isConfirmingDelete ? (
        <button
          className="secondary-btn"
          onClick={() => onDeleteConfirm(contact.id)}
          aria-label={`Confirm remove ${contact.displayName}`}
          onBlur={onDeleteCancel}
          autoFocus
          onKeyDown={(e) => {
            if (e.key === 'Escape') {
              e.preventDefault();
              onDeleteCancel();
            }
          }}
        >
          Confirm?
        </button>
      ) : (
        <button
          className="ghost-btn"
          onClick={() => onDeleteRequest(contact.id)}
          aria-label={`Remove ${contact.displayName}`}
        >
          Remove
        </button>
      )}
    </div>
  </div>
), (prev, next) => {
  return prev.isConfirmingDelete === next.isConfirmingDelete &&
    prev.contact.id === next.contact.id &&
    prev.contact.displayName === next.contact.displayName &&
    prev.contact.phoneNumber === next.contact.phoneNumber &&
    prev.contact.email === next.contact.email;
});

TrustedContactRow.displayName = 'TrustedContactRow';

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

// Bolt: Optimized MapAlertItem to prevent re-renders of the alert list
// Note: onFocus and onClear are assumed to be stable references (useCallback in parent)
const MapAlertItem = memo(({ alert, isActive, onFocus, onClear }) => {
  const [isClearing, setIsClearing] = useState(false);

  const handleClear = useCallback(async (event) => {
    event.stopPropagation();
    setIsClearing(true);
    try {
      await onClear(alert.id);
      // Successful clear will trigger list update and this component will likely unmount
      // or re-render. If it stays, we keep clearing state true to prevent double clicks.
    } catch (e) {
      console.error(e);
      // Reset state on error so user can retry
      setIsClearing(false);
    }
  }, [alert.id, onClear]);

  return (
    <div
      className={`map-item ${isActive ? 'active' : ''}`}
      onClick={() => onFocus(alert)}
      role="button"
      tabIndex={0}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          onFocus(alert);
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
          onClick={handleClear}
          disabled={isClearing}
          aria-label={`Clear alert from ${alert.address}`}
          aria-busy={isClearing}
        >
          {isClearing ? (
            <>
              <Spinner />
              Clearing...
            </>
          ) : 'Clear'}
        </button>
      </div>
    </div>
  );
}, (prev, next) => {
  return prev.isActive === next.isActive &&
    prev.alert.id === next.alert.id &&
    prev.alert.address === next.alert.address &&
    prev.alert.severity === next.alert.severity &&
    prev.alert.date === next.alert.date &&
    prev.alert.body === next.alert.body;
});
MapAlertItem.displayName = 'MapAlertItem';

// Bolt: Custom comparator for ThemeGalleryItem to handle Firestore object reference changes
const areThemeGalleryItemsEqual = (prev, next) => {
  if (prev.onImport !== next.onImport) return false;
  const p = prev.themeDoc;
  const n = next.themeDoc;
  if (p === n) return true;
  if (p.id !== n.id) return false;

  // Bolt: Check updatedAt if available (handling Firestore Timestamps)
  const getMillis = (t) => {
    if (!t) return 0;
    if (typeof t === 'number') return t;
    if (typeof t.toMillis === 'function') return t.toMillis();
    if (typeof t.seconds === 'number') return t.seconds * 1000;
    return 0;
  };

  const pTime = getMillis(p.updatedAt);
  const nTime = getMillis(n.updatedAt);
  if (pTime > 0 && nTime > 0) {
    return pTime === nTime;
  }

  // Fallback to deep check if timestamps are missing/invalid
  return p.name === n.name &&
         p.authorName === n.authorName &&
         p.authorHandle === n.authorHandle &&
         p.anonymous === n.anonymous &&
         JSON.stringify(p.theme) === JSON.stringify(n.theme);
};

// Bolt: Optimized ThemeGalleryItem to prevent re-renders when list doesn't change
const ThemeGalleryItem = memo(({ themeDoc, onImport }) => {
  const [isImporting, setIsImporting] = useState(false);
  const previewTheme = normalizeTheme(themeDoc.theme || {});
  const previewStyle = buildThemePreviewStyle(previewTheme);
  const authorLabel = themeDoc.anonymous
    ? 'Anonymous'
    : (themeDoc.authorHandle || themeDoc.authorName || 'Community');

  const handleImport = useCallback(async () => {
    setIsImporting(true);
    try {
      await onImport(themeDoc);
    } catch (e) {
      console.error(e);
    } finally {
      setIsImporting(false);
    }
  }, [onImport, themeDoc]);

  return (
    <div className="theme-card">
      <div className="theme-preview" style={previewStyle}>
        <div className="theme-preview-chat">
          <div
            className="theme-bubble incoming"
            style={{
              background: previewTheme.bubbleIncoming,
              color: previewTheme.onBubbleIncoming
            }}
          >
            Hey, you good?
          </div>
          <div
            className="theme-bubble outgoing"
            style={{
              background: previewTheme.bubbleOutgoing,
              color: previewTheme.onBubbleOutgoing
            }}
          >
            Yep, on my way!
          </div>
        </div>
      </div>
      <div className="theme-meta">
        <div className="theme-name">{themeDoc.name || 'Untitled'}</div>
        <div className="theme-author">{authorLabel}</div>
      </div>
      <button
        className="primary-btn"
        type="button"
        onClick={handleImport}
        disabled={isImporting}
        aria-busy={isImporting}
        aria-label={`Import theme ${themeDoc.name || 'Untitled'}`}
      >
        {isImporting ? (
          <>
            <Spinner />
            Importing...
          </>
        ) : 'Import'}
      </button>
    </div>
  );
}, areThemeGalleryItemsEqual);

ThemeGalleryItem.displayName = 'ThemeGalleryItem';

// Bolt: Optimized ThemePresetItem to prevent re-renders of the preset list
const ThemePresetItem = memo(({ preset, onApply }) => (
  <button
    className="theme-chip"
    onClick={() => onApply(preset.theme)}
    aria-label={`Apply ${preset.name} theme`}
  >
    <div className="theme-chip-title">
      <span className="theme-dot" style={{ background: preset.theme.primaryColor }} />
      <strong>{preset.name}</strong>
    </div>
    <div className="theme-chip-preview">
      <div
        className="theme-bubble incoming"
        style={{
          background: preset.theme.bubbleIncoming,
          color: preset.theme.onBubbleIncoming
        }}
      >
        Sample incoming
      </div>
      <div
        className="theme-bubble outgoing"
        style={{
          background: preset.theme.bubbleOutgoing,
          color: preset.theme.onBubbleOutgoing
        }}
      >
        Sample reply
      </div>
    </div>
  </button>
));
ThemePresetItem.displayName = 'ThemePresetItem';

const areRingerSongsEqual = (prev, next) => {
  return prev.song.id === next.song.id &&
         prev.song.title === next.song.title &&
         prev.song.artist === next.song.artist &&
         prev.song.albumArtUrl === next.song.albumArtUrl &&
         prev.onDelete === next.onDelete;
};

// Bolt: Optimized RingerSongItem with memo to prevent re-rendering the entire playlist
const RingerSongItem = memo(({ song, onDelete }) => {
  const [isDeleting, setIsDeleting] = useState(false);
  const isMounted = useRef(true);

  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  const handleDelete = useCallback(async () => {
    setIsDeleting(true);
    try {
      await onDelete(song.id);
    } catch (error) {
      console.error("Failed to delete song", error);
    } finally {
      if (isMounted.current) {
        setIsDeleting(false);
      }
    }
  }, [song.id, onDelete]);

  return (
    <div className="song-card">
      {song.albumArtUrl ? (
        <img src={song.albumArtUrl} alt="" className="song-art" loading="lazy" />
      ) : (
        <div className="song-art" style={{ display: 'grid', placeItems: 'center' }}>♫</div>
      )}
      <div className="song-info">
        <div className="song-title">{song.title}</div>
        <div className="song-artist">{song.artist}</div>
      </div>
      <button
        className="ghost-btn icon-only"
        onClick={handleDelete}
        disabled={isDeleting}
        title="Remove from playlist"
        aria-label={`Remove ${song.title} from playlist`}
        style={{ width: 32, height: 32, padding: 0, display: 'grid', placeItems: 'center', border: 'none' }}
      >
        {isDeleting ? <Spinner className="no-margin" /> : <TrashIcon />}
      </button>
    </div>
  );
}, areRingerSongsEqual);
RingerSongItem.displayName = 'RingerSongItem';

const areSpotifyResultsEqual = (prev, next) => {
  return prev.isAdding === next.isAdding &&
         prev.track.id === next.track.id &&
         prev.onAdd === next.onAdd;
};

// Bolt: Optimized SpotifyResultItem to prevent re-rendering all results when one is adding
const SpotifyResultItem = memo(({ track, onAdd, isAdding }) => (
  <div className="song-card">
    <img src={track.album?.images[0]?.url} alt="" className="song-art" loading="lazy" />
    <div className="song-info">
      <div className="song-title">{track.name}</div>
      <div className="song-artist">{track.artists.map(a => a.name).join(', ')}</div>
    </div>
    <button
      className="secondary-btn"
      style={{ padding: '6px 12px', fontSize: '0.85em' }}
      onClick={() => onAdd(track)}
      disabled={isAdding}
      aria-busy={isAdding}
      aria-label={`Add ${track.name} to playlist`}
    >
      {isAdding ? "Adding..." : "Add"}
    </button>
  </div>
), areSpotifyResultsEqual);
SpotifyResultItem.displayName = 'SpotifyResultItem';

// Bolt: MessageComposer extracted to prevent App re-renders on typing
const MessageComposer = memo(({ user, db, selectedThread, lineInboxMode, activeLineId, lines, isLoggingIn }) => {
  const [address, setAddress] = useState('');
  const [body, setBody] = useState('');
  const [lineId, setLineId] = useState('');
  const [status, setStatus] = useState('');
  const [isSending, setIsSending] = useState(false);

  useEffect(() => {
    if (selectedThread) {
      setAddress(selectedThread.address || '');
      setLineId(selectedThread.lineId || '');
    } else {
      setAddress('');
      // When clearing (New message), reset lineId to empty to allow user selection or fallback
      setLineId('');
    }
    setBody('');
    setStatus('');
  }, [selectedThread]);

  const handleSendMessage = async () => {
    if (!user) return;
    const cleanAddress = address.trim();
    const cleanBody = body.trim();
    const effectiveLineId = lineInboxMode === 'PER_LINE' ? (lineId || activeLineId || lines[0]?.id || null) : null;

    if (!cleanAddress || !cleanBody) {
      setStatus("Add a phone number and message.");
      return;
    }
    setIsSending(true);
    setStatus('');
    try {
      await addDoc(collection(db, "users", user.uid, "outbox"), {
        address: cleanAddress,
        body: cleanBody,
        createdAt: serverTimestamp(),
        source: "web",
        lineId: effectiveLineId
      });
      setBody('');
      setStatus("Queued for sending from your device.");
    } catch (error) {
      console.error("Send failed", error);
      setStatus("Send failed. Try again.");
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="composer">
      <div className="composer-row">
        <label className="composer-label" htmlFor="compose-address">To</label>
        <input
          id="compose-address"
          className="composer-input"
          type="tel"
          placeholder="Phone number"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
        />
      </div>
      {lineInboxMode === 'PER_LINE' && lines.length > 0 && (
        <div className="composer-row">
          <label className="composer-label" htmlFor="compose-line">Send from</label>
          <select
            id="compose-line"
            className="composer-input"
            value={lineId}
            onChange={(e) => setLineId(e.target.value)}
          >
            <option value="">Primary device</option>
            {lines.map(line => (
              <option key={line.id} value={line.id}>
                {(line.label || line.phoneNumber || line.id.slice(0, 6))}
                {line.primaryDeviceId ? ' • primary' : ''}
              </option>
            ))}
          </select>
        </div>
      )}
      <div className="composer-row composer-actions">
        <div style={{ flex: 1, position: 'relative' }}>
          <textarea
            className="composer-textarea"
            style={{ width: '100%', paddingBottom: '24px' }}
            placeholder="Type a message... (Ctrl+Enter to send)"
            aria-label="Message body"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            onKeyDown={(e) => {
              if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                handleSendMessage();
              }
            }}
          />
          {body.length > 0 && (
            <div style={{
              position: 'absolute',
              bottom: '8px',
              right: '12px',
              fontSize: '0.75em',
              color: 'var(--muted)',
              pointerEvents: 'none',
              fontWeight: 500
            }}>
              {body.length}
            </div>
          )}
        </div>
        <button
          onClick={handleSendMessage}
          disabled={isSending || isLoggingIn}
          className="primary-btn"
          title="Send (Ctrl+Enter)"
        >
          {isSending ? "Sending..." : "Send"}
        </button>
      </div>
      {status && <div className="compose-status" role="status" aria-live="polite">{status}</div>}
      <div className="compose-hint">
        Messages are sent from your phone when it&apos;s online and signed in.
      </div>
    </div>
  );
});

MessageComposer.displayName = 'MessageComposer';

const defaultTheme = {
  primaryColor: "#00F0FF",
  secondaryColor: "#D946EF",
  bubbleOutgoing: "#00F0FF",
  bubbleIncoming: "#161B2E",
  backgroundColor: "#030508",
  iconSizeFactor: 1.0,
  fontStyle: "Default",
  bubbleCornerRadius: 22,
  inboxIconVariant: "Beacon",
  onBubbleOutgoing: "#000000",
  onBubbleIncoming: "#EEF2FB",
  onBackground: "#E0F7FA",
  topBarColor: "#030508",
  onTopBarColor: "#E0F7FA",
  bubbleCornerRadiusTopStart: null,
  bubbleCornerRadiusTopEnd: null,
  bubbleCornerRadiusBottomStart: null,
  bubbleCornerRadiusBottomEnd: null,
  timestampColor: "#9FB3C8",
  dividerColor: "#1E293B",
  appBackgroundGradientStart: null,
  appBackgroundGradientEnd: null,
  fontScale: 1.0,
  backgroundImageUrl: null,
  iconOverrides: {},
  useGlassEffect: true,
  useHolographicGlow: true,
  uiDensity: 'Comfortable'
};

const themePresets = [
  {
    name: "Future Hologram",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 24,
      appBackgroundGradientStart: "#030508",
      appBackgroundGradientEnd: "#1a1033",
      onBackground: "#E0F7FA",
      topBarColor: "#030508",
      onTopBarColor: "#00F0FF",
      bubbleOutgoing: "#00F0FF",
      onBubbleOutgoing: "#000000",
      bubbleIncoming: "#1a1033",
      onBubbleIncoming: "#E0F7FA",
      primaryColor: "#00F0FF",
      secondaryColor: "#D946EF",
      dividerColor: "#1E293B",
      inboxIconVariant: "neon_noir",
      useGlassEffect: true,
      useHolographicGlow: true,
      uiDensity: "Comfortable"
    }
  },
  {
    name: "Neon Cyber",
    theme: {
      fontStyle: "Monospace",
      bubbleCornerRadius: 4,
      backgroundColor: "#000000",
      onBackground: "#00FF41",
      topBarColor: "#000000",
      onTopBarColor: "#00FF41",
      bubbleOutgoing: "#003B00",
      onBubbleOutgoing: "#00FF41",
      bubbleIncoming: "#0D0D0D",
      onBubbleIncoming: "#00FF41",
      primaryColor: "#00FF41",
      secondaryColor: "#008F11",
      dividerColor: "#003B00",
      inboxIconVariant: "midnight_oled",
      useHolographicGlow: true,
      uiDensity: "Compact"
    }
  },
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
      inboxIconVariant: "midnight_oled",
      backgroundImageUrl: midnightBg
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
      bubbleCornerRadiusBottomEnd: 0,
      backgroundImageUrl: sunsetBg
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
      inboxIconVariant: "forest_trail",
      backgroundImageUrl: forestBg
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
      iconSizeFactor: 1.15,
      backgroundImageUrl: auroraBg
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
      backgroundColor: "#030508",
      onBackground: "#E0F7FA",
      topBarColor: "#030508",
      onTopBarColor: "#00F0FF",
      bubbleOutgoing: "#00F0FF",
      onBubbleOutgoing: "#000000",
      bubbleIncoming: "#1F2937",
      onBubbleIncoming: "#E0F7FA",
      primaryColor: "#00F0FF",
      secondaryColor: "#D946EF",
      dividerColor: "#1F2937",
      inboxIconVariant: "midnight_oled",
      backgroundImageUrl: neonBg
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
  },
  {
    name: "Cyber Mist",
    theme: {
      fontStyle: "Monospace",
      bubbleCornerRadius: 18,
      backgroundColor: "#1e1b4b",
      onBackground: "#e879f9",
      topBarColor: "#2e1065",
      onTopBarColor: "#e879f9",
      bubbleOutgoing: "#d946ef",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#4c1d95",
      onBubbleIncoming: "#e879f9",
      primaryColor: "#d946ef",
      secondaryColor: "#06b6d4",
      dividerColor: "#4c1d95",
      inboxIconVariant: "midnight_oled",
      backgroundImageUrl: cyberMistBg
    }
  },
  {
    name: "Deep Ocean",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 24,
      backgroundColor: "#0f172a",
      onBackground: "#e0f2fe",
      topBarColor: "#1e3a8a",
      onTopBarColor: "#e0f2fe",
      bubbleOutgoing: "#0ea5e9",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#172554",
      onBubbleIncoming: "#38bdf8",
      primaryColor: "#0ea5e9",
      secondaryColor: "#38bdf8",
      dividerColor: "#1e3a8a",
      inboxIconVariant: "ocean_deep",
      backgroundImageUrl: deepOceanBg
    }
  }
];

const specialThemePresets = [
  // Premium Themes
  {
    id: "gold_standard",
    name: "Gold Standard",
    condition: "premium",
    theme: {
      fontStyle: "Serif",
      bubbleCornerRadius: 16,
      backgroundColor: "#332200",
      onBackground: "#FFD700",
      topBarColor: "#4B3621",
      onTopBarColor: "#FFD700",
      bubbleOutgoing: "#FFD700",
      onBubbleOutgoing: "#332200",
      bubbleIncoming: "#B8860B",
      onBubbleIncoming: "#FFFFFF",
      primaryColor: "#FFD700",
      secondaryColor: "#DAA520",
      dividerColor: "#B8860B",
      inboxIconVariant: "shield",
      backgroundImageUrl: goldBg
    }
  },
  {
    id: "diamond_dust",
    name: "Diamond Dust",
    condition: "premium",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 20,
      backgroundColor: "#F0F4F8",
      onBackground: "#263238",
      topBarColor: "#FFFFFF",
      onTopBarColor: "#263238",
      bubbleOutgoing: "#E0F7FA",
      onBubbleOutgoing: "#006064",
      bubbleIncoming: "#FFFFFF",
      onBubbleIncoming: "#263238",
      primaryColor: "#00BCD4",
      secondaryColor: "#0097A7",
      dividerColor: "#B2EBF2",
      inboxIconVariant: "bubble",
      backgroundImageUrl: diamondBg
    }
  },
  // Pro Themes
  {
    id: "obsidian_pro",
    name: "Obsidian Pro",
    condition: "pro",
    theme: {
      fontStyle: "Monospace",
      bubbleCornerRadius: 4,
      backgroundColor: "#000000",
      onBackground: "#E0E0E0",
      topBarColor: "#121212",
      onTopBarColor: "#FFFFFF",
      bubbleOutgoing: "#212121",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#121212",
      onBubbleIncoming: "#BDBDBD",
      primaryColor: "#616161",
      secondaryColor: "#424242",
      dividerColor: "#333333",
      inboxIconVariant: "minimal",
      backgroundImageUrl: obsidianBg
    }
  },
  {
    id: "titanium_flow",
    name: "Titanium Flow",
    condition: "pro",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 24,
      backgroundColor: "#2c3e50",
      onBackground: "#ecf0f1",
      topBarColor: "#34495e",
      onTopBarColor: "#ecf0f1",
      bubbleOutgoing: "#95a5a6",
      onBubbleOutgoing: "#2c3e50",
      bubbleIncoming: "#34495e",
      onBubbleIncoming: "#bdc3c7",
      primaryColor: "#bdc3c7",
      secondaryColor: "#7f8c8d",
      dividerColor: "#7f8c8d",
      inboxIconVariant: "shield",
      backgroundImageUrl: titaniumBg
    }
  },
  // Beta Themes
  {
    id: "blueprint",
    name: "Blueprint",
    condition: "beta",
    theme: {
      fontStyle: "Monospace",
      bubbleCornerRadius: 0,
      backgroundColor: "#002b36",
      onBackground: "#839496",
      topBarColor: "#073642",
      onTopBarColor: "#93a1a1",
      bubbleOutgoing: "#2aa198",
      onBubbleOutgoing: "#002b36",
      bubbleIncoming: "#073642",
      onBubbleIncoming: "#2aa198",
      primaryColor: "#2aa198",
      secondaryColor: "#268bd2",
      dividerColor: "#586e75",
      inboxIconVariant: "beacon",
      backgroundImageUrl: blueprintBg
    }
  },
  {
    id: "glitch_stream",
    name: "Glitch Stream",
    condition: "beta",
    theme: {
      fontStyle: "Monospace",
      bubbleCornerRadius: 8,
      backgroundColor: "#0f0f0f",
      onBackground: "#00ff00",
      topBarColor: "#000000",
      onTopBarColor: "#00ff00",
      bubbleOutgoing: "#003300",
      onBubbleOutgoing: "#00ff00",
      bubbleIncoming: "#001100",
      onBubbleIncoming: "#00cc00",
      primaryColor: "#00ff00",
      secondaryColor: "#ff00ff",
      dividerColor: "#004400",
      inboxIconVariant: "minimal",
      backgroundImageUrl: glitchBg
    }
  },
  // Loyalty Themes
  {
    id: "legacy_oak",
    name: "Legacy Oak",
    condition: "loyal",
    theme: {
      fontStyle: "Serif",
      bubbleCornerRadius: 12,
      backgroundColor: "#3E2723",
      onBackground: "#D7CCC8",
      topBarColor: "#4E342E",
      onTopBarColor: "#D7CCC8",
      bubbleOutgoing: "#5D4037",
      onBubbleOutgoing: "#EFEBE9",
      bubbleIncoming: "#4E342E",
      onBubbleIncoming: "#D7CCC8",
      primaryColor: "#8D6E63",
      secondaryColor: "#A1887F",
      dividerColor: "#5D4037",
      inboxIconVariant: "default_light",
      backgroundImageUrl: oakBg
    }
  },
  {
    id: "eternal_sky",
    name: "Eternal Sky",
    condition: "loyal",
    theme: {
      fontStyle: "Default",
      bubbleCornerRadius: 28,
      backgroundColor: "#000033",
      onBackground: "#E0E0FF",
      topBarColor: "#191970",
      onTopBarColor: "#FFFFFF",
      bubbleOutgoing: "#483D8B",
      onBubbleOutgoing: "#FFFFFF",
      bubbleIncoming: "#000080",
      onBubbleIncoming: "#E0E0FF",
      primaryColor: "#8A2BE2",
      secondaryColor: "#9370DB",
      dividerColor: "#191970",
      inboxIconVariant: "beacon",
      backgroundImageUrl: eternalBg
    }
  }
];

const avatarPresets = [
  { id: "premium_crown", name: "Premium Crown", condition: "premium", src: premiumAvatar },
  { id: "pro_spark", name: "Pro Spark", condition: "pro", src: proAvatar },
  { id: "beta_flask", name: "Beta Flask", condition: "beta", src: betaAvatar },
  { id: "loyal_star", name: "Loyal Star", condition: "loyal", src: loyalAvatar },
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
  iconOverrides: input.iconOverrides ?? defaultTheme.iconOverrides,
  useGlassEffect: input.useGlassEffect ?? defaultTheme.useGlassEffect,
  useHolographicGlow: input.useHolographicGlow ?? defaultTheme.useHolographicGlow,
  uiDensity: input.uiDensity ?? defaultTheme.uiDensity
});

const buildThemeVars = (theme) => {
  const active = normalizeTheme(theme);
  const vars = {
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
  if (active.backgroundImageUrl) {
    vars["backgroundImage"] = `url(${active.backgroundImageUrl})`;
    vars["backgroundSize"] = 'cover';
    vars["backgroundPosition"] = 'center';
    vars["backgroundAttachment"] = 'fixed'; // nice parallax effect
  }
  return vars;
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

// Sentinel: Prevent XSS in map info windows
const escapeHtml = (unsafe) => {
  return (unsafe || '')
    .toString()
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
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

// Bolt: Optimized Sidebar to prevent re-renders on high-frequency parent updates (typing)
const Sidebar = memo(({
  activePanel,
  setActivePanel,
  tierLabel,
  handleLogout, // Bolt: Explicitly included for logout functionality
  handleNewThread, // Bolt: Explicitly included for new conversation
  lines, // Bolt: Explicitly included for multi-line support
  activeLineId,
  setActiveLineId,
  lineInboxMode,
  isLoadingThreads,
  isPremium,
  navLogo,
  brandTitle,
  threads,
  selectedThreadId,
  onSelect,
  showPreviews
}) => {
  const [searchQuery, setSearchQuery] = useState('');

  // Bolt: Pre-compute search strings for threads locally to prevent App re-renders
  const searchIndex = useMemo(() => {
    return threads.map(t => {
      const display = (t.display_name || t.address || '').toLowerCase();
      const snippet = (t.snippet || '').toLowerCase();
      return { thread: t, searchString: `${display} ${snippet}` };
    });
  }, [threads]);

  const filteredThreads = useMemo(() => {
    const term = searchQuery.trim().toLowerCase();
    if (!term) return threads;
    return searchIndex
      .filter(({ searchString }) => searchString.includes(term))
      .map(({ thread }) => thread);
  }, [searchIndex, searchQuery, threads]);

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-brand">
          <img src={navLogo || logo} alt="PulseLink Suite" className="brand-logo small" />
          <div>
            <div className="brand-title">{brandTitle || "PulseLink Suite"}</div>
            <div className="brand-subtitle">{tierLabel} Web Access</div>
          </div>
        </div>
        <div className="sidebar-actions">
          {activePanel === 'beacon' && (
            <button
              onClick={handleNewThread}
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
          title="Home"
          aria-label="Home"
          aria-current={activePanel === 'home' ? 'page' : undefined}
        >
          <HomeIcon />
          <span>Home</span>
        </button>
        <button
          className={`nav-item ${activePanel === 'pulselink' ? 'active' : ''}`}
          onClick={() => setActivePanel('pulselink')}
          title="PulseLink"
          aria-label="PulseLink"
          aria-current={activePanel === 'pulselink' ? 'page' : undefined}
        >
          <img src={logo} alt="PulseLink" />
          <span>PulseLink</span>
        </button>
        <button
          className={`nav-item ${activePanel === 'beacon' ? 'active' : ''}`}
          onClick={() => setActivePanel('beacon')}
          title="Beacon"
          aria-label="Beacon"
          aria-current={activePanel === 'beacon' ? 'page' : undefined}
        >
          <img src={beaconLogo} alt="Beacon" />
          <span>Beacon</span>
        </button>
        <button
          className={`nav-item ${activePanel === 'ringersong' ? 'active' : ''}`}
          onClick={() => setActivePanel('ringersong')}
          title="RingerSong"
          aria-label="RingerSong"
          aria-current={activePanel === 'ringersong' ? 'page' : undefined}
        >
          <img src={ringersongLogo} alt="RingerSong" />
          <span>RingerSong</span>
        </button>
        <button
          className={`nav-item ${activePanel === 'map' ? 'active' : ''}`}
          onClick={() => setActivePanel('map')}
          title="Map"
          aria-label="Map"
          aria-current={activePanel === 'map' ? 'page' : undefined}
        >
          <MapIcon />
          <span>Map</span>
        </button>
        <button
          className={`nav-item ${activePanel === 'contacts' ? 'active' : ''}`}
          onClick={() => setActivePanel('contacts')}
          title="Contacts"
          aria-label="Contacts"
          aria-current={activePanel === 'contacts' ? 'page' : undefined}
        >
          <ContactIcon />
          <span>Contacts</span>
        </button>
        <button
          className={`nav-item ${activePanel === 'themes' ? 'active' : ''}`}
          onClick={() => setActivePanel('themes')}
          title="Themes"
          aria-label="Themes"
          aria-current={activePanel === 'themes' ? 'page' : undefined}
        >
          <ThemeIcon />
          <span>Themes</span>
        </button>
        <button
          className={`nav-item ${activePanel === 'extensions' ? 'active' : ''}`}
          onClick={() => setActivePanel('extensions')}
          title="Extensions"
          aria-label="Extensions"
          aria-current={activePanel === 'extensions' ? 'page' : undefined}
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
          <span>Extensions</span>
          <span className="badge-new">NEW</span>
        </button>
        <button
          className={`nav-item ${activePanel === 'settings' ? 'active' : ''}`}
          onClick={() => setActivePanel('settings')}
          title="Settings"
          aria-label="Settings"
          aria-current={activePanel === 'settings' ? 'page' : undefined}
        >
          <SettingsIcon />
          <span>Settings</span>
        </button>
      </div>
      {activePanel === 'beacon' ? (
        <>
          <div className="sidebar-search-container">
            <div className="sidebar-search-wrapper">
              <div className="search-icon-wrapper">
                <SearchIcon />
              </div>
              <input
                className="sidebar-search-input-field"
                placeholder="Search messages..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                aria-label="Search messages"
              />
              {searchQuery && (
                <button
                  className="ghost-btn icon-only sidebar-search-clear-btn"
                  onClick={() => setSearchQuery('')}
                  aria-label="Clear search"
                  title="Clear search"
                >
                  <CloseIcon />
                </button>
              )}
            </div>
          </div>
          <div className="thread-list">
            {lineInboxMode === 'PER_LINE' && lines.length > 0 && (
              <div className="line-tabs" aria-label="Device lines">
                <button
                  className={`chip ${!activeLineId ? 'active' : ''}`}
                  onClick={() => setActiveLineId(null)}
                >
                  All
                </button>
                {lines.map((line) => (
                  <button
                    key={line.id}
                    className={`chip ${activeLineId === line.id ? 'active' : ''}`}
                    onClick={() => setActiveLineId(line.id)}
                    title={line.phoneNumber || 'Line'}
                  >
                    {line.label || line.phoneNumber || line.id.slice(0, 6)}
                  </button>
                ))}
              </div>
            )}
            {isLoadingThreads ? (
               Array.from({ length: 5 }).map((_, i) => <ThreadSkeleton key={i} />)
            ) : filteredThreads.length === 0 ? (
              <div className="sidebar-placeholder">
                <div className="sidebar-tip">
                  <strong>{searchQuery ? "No matches found" : "No conversations found"}</strong>
                </div>
                {!searchQuery && (
                  <div className="sidebar-tip muted">
                    To see your messages here:
                    <ol style={{ paddingLeft: '20px', margin: '8px 0' }}>
                      <li>Open PulseLink on your phone</li>
                      <li>Go to Extensions Store</li>
                      <li>Enable &quot;Remote Web Access&quot;</li>
                    </ol>
                    {!isPremium && (
                      <div className="badge badge-premium" style={{ display: 'inline-block', marginTop: '8px', padding: '2px 8px', borderRadius: '4px', background: 'var(--accent)', color: '#fff', fontSize: '0.8em' }}>
                        Premium Required
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              filteredThreads.map(thread => (
                <ThreadItem
                  key={`${thread.lineId || 'legacy'}_${thread.id}`}
                  thread={thread}
                  isActive={selectedThreadId === thread.id}
                  onSelect={onSelect}
                  showPreviews={showPreviews}
                />
              ))
            )}
          </div>
        </>
      ) : (
        <div className="sidebar-placeholder">
          <div className="sidebar-tip">Use the tiles on Home to jump into PulseLink or Beacon.</div>
          <div className="sidebar-tip muted">Theme and settings sync to your device.</div>
        </div>
      )}
    </div>
  );
}, (prev, next) => {
  // Bolt: Ensure strict equality checks for all props to prevent unnecessary re-renders
  return prev.activePanel === next.activePanel &&
         prev.tierLabel === next.tierLabel &&
         prev.isLoadingThreads === next.isLoadingThreads &&
         prev.threads === next.threads &&
         prev.lines === next.lines &&
         prev.activeLineId === next.activeLineId &&
         prev.lineInboxMode === next.lineInboxMode &&
         prev.isPremium === next.isPremium &&
         prev.selectedThreadId === next.selectedThreadId &&
         prev.onSelect === next.onSelect &&
         prev.showPreviews === next.showPreviews &&
         prev.navLogo === next.navLogo &&
         prev.brandTitle === next.brandTitle;
});

Sidebar.displayName = 'Sidebar';

const getToastClass = (msg) => {
  if (!msg) return 'toast';
  const lower = msg.toLowerCase();
  if (lower.includes('fail') || lower.includes('error') || lower.includes('missing')) return 'toast error';
  if (lower.includes('success') || lower.includes('saved') || lower.includes('updated') || lower.includes('sent') || lower.includes('published') || lower.includes('imported') || lower.includes('cleared')) return 'toast success';
  return 'toast';
};

function App() {
  const webHintStorageKey = 'pulselink.hideWebHint';
  const [user, setUser] = useState(null);
  const [userData, setUserData] = useState(null);
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [isLoadingThreads, setIsLoadingThreads] = useState(true);
  const [legacyThreads, setLegacyThreads] = useState([]);
  const [lineThreads, setLineThreads] = useState({});
  const [lines, setLines] = useState([]);
  const [lineInboxMode, setLineInboxMode] = useState('COMBINED');
  const [activeLineId, setActiveLineId] = useState(null);
  const [selectedThread, setSelectedThread] = useState(null);
  const [messages, setMessages] = useState([]);
  const [profile, setProfile] = useState({
    ownerName: '',
    avatarUrl: '',
    avatarId: '',
    email: '',
    phoneNumber: ''
  });
  const [trustedContacts, setTrustedContacts] = useState([]);
  const [deviceContacts, setDeviceContacts] = useState([]);
  const [contactSearch, setContactSearch] = useState('');
  const [contactListLimit, setContactListLimit] = useState(50);
  const [unlockedAvatars, setUnlockedAvatars] = useState([]);
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
  const [isSavingContact, setIsSavingContact] = useState(false);
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
  const [isPublishingTheme, setIsPublishingTheme] = useState(false);
  const [showWebHint, setShowWebHint] = useState(() => {
    if (typeof window === 'undefined') return true;
    return localStorage.getItem(webHintStorageKey) !== 'true';
  });
  const [remoteSettings, setRemoteSettings] = useState({
    remoteWebAccessEnabled: false,
    autoUpdateContactInfo: true,
    timeFormat: 'AUTO',
    thirdPartyExtensionsEnabled: true,
    beaconLauncherEnabled: false,
    otpCleanupEnabled: false,
    emailFallbackEnabled: false,
    crashDetectionEnabled: false,
    aiSummariesEnabled: false,
    firebaseMessagingEnabled: true,
    mergedExperienceEnabled: false,
    privateSafeEnabled: false,
    smartRepliesEnabled: true
  });
  const [devExtensions, setDevExtensions] = useState(() => {
    const saved = localStorage.getItem('pulselink.devExtensions');
    if (saved) return JSON.parse(saved);
    return [
      {
        id: 'hello-world',
        name: 'Hello World Webhook',
        endpoint: '',
        description: 'Shows a toast when triggered. Great for validating the pipeline.',
        sample: true
      },
      {
        id: 'pulse-check',
        name: 'Pulse Check Logger',
        endpoint: 'https://postman-echo.com/post',
        description: 'Sends a payload with your user + sample message to a test echo endpoint.',
        sample: true
      }
    ];
  });
  const [extensionForm, setExtensionForm] = useState({ name: '', endpoint: '', description: '' });
  const [extensionStatus, setExtensionStatus] = useState('');
  useEffect(() => {
    localStorage.setItem('pulselink.devExtensions', JSON.stringify(devExtensions));
  }, [devExtensions]);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [authError, setAuthError] = useState('');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [activePanel, setActivePanel] = useState('home');

  // Bolt: Reset contact list limit when search or panel changes
  useEffect(() => {
    setContactListLimit(50);
  }, [contactSearch, activePanel]);
  const [alertLocations, setAlertLocations] = useState([]);
  const [alertStatus, setAlertStatus] = useState('');
  const [severityFilter, setSeverityFilter] = useState('emergency');
  const [incomingOnly, setIncomingOnly] = useState(true);
  const [selectedAlertId, setSelectedAlertId] = useState(null);
  const [mapStatus, setMapStatus] = useState('');
  const [geoStatus, setGeoStatus] = useState('');
  const [userLocation, setUserLocation] = useState(null);
  const [settingsStatus, setSettingsStatus] = useState('');
  const [remoteSettingsStatus, setRemoteSettingsStatus] = useState('');
  const [isSavingSettings, setIsSavingSettings] = useState(false);
  const [syncDiagnostics, setSyncDiagnostics] = useState(null);
  const [syncRequestStatus, setSyncRequestStatus] = useState('');
  const [deleteStatus, setDeleteStatus] = useState('');
  const [deleteAction, setDeleteAction] = useState(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState(null);
  const [showPreviews, setShowPreviews] = useState(true);
  const [autoScroll, setAutoScroll] = useState(true);
  const [spotifyToken, setSpotifyToken] = useState(null);
  const [spotifySearch, setSpotifySearch] = useState('');
  const [isSearchingSpotify, setIsSearchingSpotify] = useState(false);
  const [spotifyResults, setSpotifyResults] = useState([]);
  const [ringerPlaylist, setRingerPlaylist] = useState([]);
  const [addingTrackId, setAddingTrackId] = useState(null);
  const [showDevTools, setShowDevTools] = useState(false);
  const [settingsSearch, setSettingsSearch] = useState('');

  const subscriptionStatus = userData?.subscriptionStatus;
  const isPremiumUser = useMemo(() => {
    return subscriptionStatus === "premium" ||
      subscriptionStatus === "pro" ||
      userData?.premiumUnlocked === true ||
      userData?.proUnlocked === true ||
      userData?.hasPremiumHistory === true ||
      userData?.hasProHistory === true;
  }, [subscriptionStatus, userData?.premiumUnlocked, userData?.proUnlocked, userData?.hasPremiumHistory, userData?.hasProHistory]);

  const isProUser = useMemo(() => {
    return isPremiumUser ||
      subscriptionStatus === "pro" ||
      userData?.proUnlocked === true ||
      userData?.hasProHistory === true;
  }, [isPremiumUser, subscriptionStatus, userData?.proUnlocked, userData?.hasProHistory]);

  // Toggle DevTools with Ctrl+Shift+D
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.ctrlKey && e.shiftKey && e.key === 'D') {
        setShowDevTools(prev => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  useEffect(() => {
    if (!user) {
      setRingerPlaylist([]);
      return;
    }
    const playlistRef = collection(db, "users", user.uid, "ringer_playlist");
    const q = query(playlistRef, orderBy("addedAt", "desc"));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const items = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setRingerPlaylist(items);
    });
    return () => unsubscribe();
  }, [user]);

  const handleDeleteRingerSong = useCallback(async (songId) => {
    if (!user) return;
    try {
      await deleteDoc(doc(db, "users", user.uid, "ringer_playlist", songId));
    } catch (e) {
      console.error("Failed to delete song", e);
    }
  }, [user]);

  const getSpotifyToken = async () => {
    // Sentinel: Use Cloud Function to fetch token securely, avoiding exposed secrets
    const callable = httpsCallable(functions, 'getSpotifyAccessToken');
    const result = await callable();
    const token = result.data.access_token;
    if (!token) throw new Error("Failed to retrieve access token");
    setSpotifyToken(token);
    return token;
  };

  const handleSpotifySearch = async () => {
    if (!spotifySearch.trim()) return;
    setIsSearchingSpotify(true);
    setSettingsStatus("Searching...");
    try {
        let token = spotifyToken;
        if (!token) {
            token = await getSpotifyToken();
        }
        
        const response = await fetch(`https://api.spotify.com/v1/search?q=${encodeURIComponent(spotifySearch)}&type=track&limit=5`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        
        if (response.status === 401) {
            // Token expired, retry once
            token = await getSpotifyToken();
            const retry = await fetch(`https://api.spotify.com/v1/search?q=${encodeURIComponent(spotifySearch)}&type=track&limit=5`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const data = await retry.json();
            setSpotifyResults(data.tracks?.items || []);
        } else {
            const data = await response.json();
            setSpotifyResults(data.tracks?.items || []);
        }
        setSettingsStatus("");
    } catch (e) {
        setSettingsStatus("Search failed: " + e.message);
    } finally {
        setIsSearchingSpotify(false);
    }
  };

  const handlePushSpotifyTrack = useCallback(async (track) => {
      const target = user?.uid;
      if(!target) {
          setSettingsStatus("Please sign in to push tracks.");
          return;
      }
      try {
          setAddingTrackId(track.id);
          setSettingsStatus(`Adding "${track.name}"...`);
          const trackData = {
              spotifyId: track.id,
              uri: track.uri,
              title: track.name,
              artist: track.artists?.map(a => a.name).join(', ') || "Unknown Artist",
              durationMs: track.duration_ms || 0,
              albumArtUrl: track.album?.images?.[0]?.url || null,
              addedAt: serverTimestamp()
          };
          
          // Use addDoc to let Firestore generate the ID, or use track.id as doc ID to prevent duplicates
          // The Android app uses add(), so we should probably mimic that or just use setDoc with track.id
          // Using setDoc with track.id prevents duplicates better.
          await setDoc(doc(db, "users", target, "ringer_playlist", track.id), trackData);
          
          setSettingsStatus("Added to playlist!");
          // Clear search results after adding? Maybe not, user might want to add multiple.
      } catch(e) {
          setSettingsStatus("Error adding: " + e.message);
      } finally {
          setAddingTrackId(null);
      }
  }, [user]);

  const messagesEndRef = useRef(null);
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const mapMarkersRef = useRef(new Map());
  const mapInfoRef = useRef(null);
  const mapHomeMarkerRef = useRef(null);
  const mapsApiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
  const defaultMapCenter = useMemo(() => ({ lat: 39.5, lng: -98.35 }), []);
  const themeVars = useMemo(() => buildThemeVars(themePrefs), [themePrefs]);

  // Fix for undefined function causing crash/lint error
  // Removed duplicate declaration
  const filteredAlerts = useMemo(() => {
    return alertLocations.filter((alert) => {
      if (incomingOnly && !alert.incoming) return false;
      if (severityFilter === 'emergency') return alert.severity === 'emergency';
      if (severityFilter === 'check_in') return alert.severity === 'check_in';
      if (severityFilter === 'non_urgent') return alert.severity === 'non_urgent';
      return true;
    });
  }, [alertLocations, incomingOnly, severityFilter]);
  // Bolt: Pre-compute search strings for themes to avoid redundant lowercasing during typing
  const themeSearchIndex = useMemo(() => {
    return publicThemes.map(theme => {
      const name = (theme.name ?? '').toString().toLowerCase();
      const author = (theme.authorName ?? '').toString().toLowerCase();
      const handle = (theme.authorHandle ?? '').toString().toLowerCase();
      return { theme, searchString: `${name} ${author} ${handle}` };
    });
  }, [publicThemes]);

  const filteredThemes = useMemo(() => {
    const term = themeSearch.trim().toLowerCase();
    if (!term) return publicThemes;
    return themeSearchIndex
      .filter(({ searchString }) => searchString.includes(term))
      .map(({ theme }) => theme);
  }, [themeSearchIndex, themeSearch, publicThemes]);
  // Bolt: Pre-compute search strings for contacts to avoid expensive string operations on every keystroke
  const contactSearchIndex = useMemo(() => {
    return deviceContacts.map(contact => {
      const parts = [
        contact.displayName,
        contact.phoneNumber,
        contact.email,
        ...(Array.isArray(contact.additionalPhones) ? contact.additionalPhones : []),
        ...(Array.isArray(contact.additionalEmails) ? contact.additionalEmails : [])
      ];

      const searchString = parts
        .filter(part => part !== null && part !== undefined)
        .map(part => String(part).toLowerCase())
        .join(' ');

      return { contact, searchString };
    });
  }, [deviceContacts]);

  const filteredDeviceContacts = useMemo(() => {
    const term = contactSearch.trim().toLowerCase();
    if (!term) return deviceContacts;

    // Bolt: Use the pre-computed index for O(N) simple string inclusion check
    return contactSearchIndex
      .filter(({ searchString }) => searchString.includes(term))
      .map(({ contact }) => contact);
  }, [contactSearchIndex, contactSearch, deviceContacts]);

  // Bolt: Memoize list elements to avoid re-creating them on every render
  const messageListElements = useMemo(() => (
    messages.map(msg => (
      <MessageItem key={msg.id} msg={msg} showPreviews={showPreviews} />
    ))
  ), [messages, showPreviews]);

  // Bolt: Pagination for contact list to improve performance
  const contactListElements = useMemo(() => (
    filteredDeviceContacts.slice(0, contactListLimit).map((contact) => (
      <DeviceContactItem key={contact.id} contact={contact} />
    ))
  ), [filteredDeviceContacts, contactListLimit]);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setIsLoggingIn(false);
      if (currentUser) {
        setActivePanel('home');
      }
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
      setLineInboxMode('COMBINED');
      setActiveLineId(null);
      setLines([]);
      setLegacyThreads([]);
      setLineThreads({});
      return;
    }
    const userRef = doc(db, "users", user.uid);
    const unsubscribe = onSnapshot(userRef, (snapshot) => {
      const data = snapshot.data() || {};
      setUserData(data);
      const isPremium = data.subscriptionStatus === 'premium' || data.premiumUnlocked === true || data.hasPremiumHistory;
      const isPro = isPremium || data.subscriptionStatus === 'pro' || data.proUnlocked === true || data.hasProHistory;
      const isBeta = data.isBetaTester === true;
      const tenureDays = data.createdAt ? (Date.now() - toMillis(data.createdAt)) / (1000 * 60 * 60 * 24) : 0;
      setProfile({
        ownerName: data.ownerName ?? user.displayName ?? '',
        avatarUrl: data.avatarUrl ?? '',
        avatarId: data.avatarId ?? '',
        email: data.email ?? user.email ?? '',
        phoneNumber: data.phoneNumber ?? ''
      });
      if (data.themePreferences) {
        setThemePrefs(normalizeTheme(data.themePreferences));
      } else {
        setThemePrefs(defaultTheme);
      }
      setRemoteSettings({
        remoteWebAccessEnabled: data.remoteWebAccessEnabled ?? isPremium,
        autoUpdateContactInfo: data.autoUpdateContactInfo ?? true,
        timeFormat: data.timeFormat ?? 'AUTO',
        thirdPartyExtensionsEnabled: data.thirdPartyExtensionsEnabled ?? true,
        beaconLauncherEnabled: data.beaconLauncherEnabled ?? false,
        otpCleanupEnabled: data.otpCleanupEnabled ?? false,
        emailFallbackEnabled: data.emailFallbackEnabled ?? false,
        crashDetectionEnabled: data.crashDetectionEnabled ?? false,
        aiSummariesEnabled: data.aiSummariesEnabled ?? false,
        firebaseMessagingEnabled: data.firebaseMessagingEnabled ?? true,
        mergedExperienceEnabled: data.mergedExperienceEnabled ?? false,
        privateSafeEnabled: data.privateSafeEnabled ?? false,
        smartRepliesEnabled: data.smartRepliesEnabled ?? true
      });
      if (data.lineInboxMode) setLineInboxMode(data.lineInboxMode);
      if (data.activeLineId) setActiveLineId(data.activeLineId);

      // Check for theme and avatar unlocks
      const currentUnlockedIds = data.unlockedThemeIds || [];
      const currentUnlockedAvatars = data.unlockedAvatarIds || [];
      const newUnlocks = [];
      const newAvatarUnlocks = [];
      // Mock status checks if fields don't exist yet, effectively unlocking for testing if user has flags
      // In production, these flags would be set by payment/backend logic
      const isLoyal = tenureDays > 365;

      specialThemePresets.forEach(preset => {
        if (currentUnlockedIds.includes(preset.id)) return;
        
        let unlocked = false;
        if (preset.condition === 'premium' && isPremium) unlocked = true;
        if (preset.condition === 'pro' && isPro) unlocked = true;
        if (preset.condition === 'beta' && isBeta) unlocked = true;
        if (preset.condition === 'loyal' && isLoyal) unlocked = true;

        if (unlocked) {
          newUnlocks.push(preset.id);
        }
      });

      avatarPresets.forEach(preset => {
        if (currentUnlockedAvatars.includes(preset.id)) return;
        
        let unlocked = false;
        if (preset.condition === 'premium' && isPremium) unlocked = true;
        if (preset.condition === 'pro' && isPro) unlocked = true;
        if (preset.condition === 'beta' && isBeta) unlocked = true;
        if (preset.condition === 'loyal' && isLoyal) unlocked = true;

        if (unlocked) {
          newAvatarUnlocks.push(preset.id);
        }
      });

      const updates = {};
      if (newUnlocks.length > 0) {
        updates.unlockedThemeIds = [...currentUnlockedIds, ...newUnlocks];
      }

      if (newAvatarUnlocks.length > 0) {
        updates.unlockedAvatarIds = [...currentUnlockedAvatars, ...newAvatarUnlocks];
      } else {
        setUnlockedAvatars(avatarPresets.filter(p => currentUnlockedAvatars.includes(p.id)));
      }

      if (Object.keys(updates).length > 0) {
        setDoc(doc(db, "users", user.uid), updates, { merge: true });
      }
    });
    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (!user || !isPremiumUser || !userData) return;
    if (userData.remoteWebAccessEnabled === undefined) {
      setDoc(doc(db, "users", user.uid), {
        remoteWebAccessEnabled: true
      }, { merge: true }).catch((err) => {
        console.error("Failed to auto-enable remote web access", err);
      });
    }
  }, [user, userData, isPremiumUser]);

  useEffect(() => {
    if (!user) {
      setSyncDiagnostics(null);
      return;
    }
    const diagRef = doc(db, "users", user.uid, "syncDiagnostics", "latest");
    const unsubscribe = onSnapshot(diagRef, (snapshot) => {
      setSyncDiagnostics(snapshot.exists() ? snapshot.data() : null);
    });
    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (!user) {
      setTrustedContacts([]);
      setDeviceContacts([]);
      return;
    }
    const trustedRef = collection(db, "users", user.uid, "trustedContacts");
    const unsubscribe = onSnapshot(trustedRef, (snapshot) => {
      const items = snapshot.docs.map(docSnap => ({
        id: docSnap.id,
        ...docSnap.data()
      }));
      items.sort((a, b) => (a.contactOrder ?? 0) - (b.contactOrder ?? 0));
      setTrustedContacts(items);
    });
    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (!user) {
      setDeviceContacts([]);
      return;
    }
    const deviceRef = collection(db, "users", user.uid, "deviceContacts");
    const unsubscribe = onSnapshot(deviceRef, (snapshot) => {
      const items = snapshot.docs.map(docSnap => ({
        id: docSnap.id,
        ...docSnap.data()
      }));
      items.sort((a, b) => (a.displayName ?? '').localeCompare(b.displayName ?? ''));
      setDeviceContacts(items);
    });
    return () => unsubscribe();
  }, [user]);

  useEffect(() => {
    if (!user) {
      setLegacyThreads([]);
      setLines([]);
      setLineThreads({});
      setIsLoadingThreads(false);
      return;
    }
    setIsLoadingThreads(true);
    // Legacy single-line threads (synced_threads)
    const legacyRef = collection(db, "users", user.uid, "synced_threads");
    const legacyQuery = query(legacyRef, orderBy("date", "desc"));
    const unsubscribeLegacy = onSnapshot(legacyQuery, (snapshot) => {
      const threadsData = snapshot.docs.map(doc => ({ id: doc.id, lineId: null, ...doc.data() }));
      setLegacyThreads(threadsData);
      // Only set loading to false if we have at least legacy threads or lines have also loaded.
      // But for simplicity, we can set it to false here as we have *some* data.
      // A better approach would be to wait for both, but onSnapshot is async.
      // Let's assume lines load quickly or we just wait for the first data update.
      setIsLoadingThreads(false);
    });

    // Multi-device: lines/{lineId}/threads
    const linesRef = collection(db, "users", user.uid, "lines");
    const threadUnsubs = new Map();

    const attachLine = (lineId) => {
      if (threadUnsubs.has(lineId)) return;
      const lineThreadsRef = collection(db, "users", user.uid, "lines", lineId, "threads");
      const lineQuery = query(lineThreadsRef, orderBy("date", "desc"));
      const unsub = onSnapshot(lineQuery, (snapshot) => {
        const items = snapshot.docs.map(doc => ({ id: doc.id, lineId, ...doc.data() }));
        setLineThreads((prev) => ({ ...prev, [lineId]: items }));
      });
      threadUnsubs.set(lineId, unsub);
    };

    const detachAll = () => {
      threadUnsubs.forEach((u) => u());
      threadUnsubs.clear();
    };

    const unsubscribeLines = onSnapshot(linesRef, (snapshot) => {
      const lineItems = snapshot.docs
        .map(doc => ({ id: doc.id, ...doc.data() }))
        .filter(line => line.disabled !== true);
      setLines(lineItems);

      // Attach listeners for new/active lines
      lineItems.forEach(line => attachLine(line.id));

      // Detach listeners for removed or disabled lines
      const activeIds = new Set(lineItems.map(l => l.id));
      // Safe iteration: collect IDs to remove first
      const idsToRemove = Array.from(threadUnsubs.keys()).filter(id => !activeIds.has(id));

      if (idsToRemove.length > 0) {
        idsToRemove.forEach(id => {
          const unsub = threadUnsubs.get(id);
          if (unsub) unsub();
          threadUnsubs.delete(id);
        });

        setLineThreads(prev => {
          const next = { ...prev };
          idsToRemove.forEach(id => delete next[id]);
          return next;
        });
      }

      setIsLoadingThreads(false);
    });

    return () => {
      unsubscribeLegacy();
      unsubscribeLines();
      detachAll();
    };
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
      // Listen to messages for the selected thread
      const basePath = selectedThread.lineId
        ? ["users", user.uid, "lines", selectedThread.lineId, "threads", selectedThread.id, "messages"]
        : ["users", user.uid, "synced_threads", selectedThread.id, "messages"];  
      const messagesRef = collection(db, ...basePath);
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
        if (error?.code === 'permission-denied') {
          setAlertStatus('Missing permissions to read emergency locations. Sign out/in or check Firebase rules for your account.');
        } else {
          setAlertStatus(error?.message ?? 'Unable to load emergency locations.');
        }
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

  // Bolt: Optimized map marker reconciliation to prevent full re-render/flicker on every update
  useEffect(() => {
    if (!mapInstanceRef.current || !window.google?.maps) return;

    // 1. Handle Home Marker (User Location) when no alerts
    if (!filteredAlerts.length) {
      // Clear all alert markers since we are in "Home Mode"
      if (mapMarkersRef.current.size > 0) {
        mapMarkersRef.current.forEach((marker) => marker.setMap(null));
        mapMarkersRef.current.clear();
      }

      const center = userLocation ?? defaultMapCenter;
      mapInstanceRef.current.setCenter(center);
      mapInstanceRef.current.setZoom(userLocation ? 12 : 3);

      if (userLocation) {
        if (!mapHomeMarkerRef.current) {
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
        } else {
          mapHomeMarkerRef.current.setPosition(center);
          mapHomeMarkerRef.current.setMap(mapInstanceRef.current);
        }
      } else {
        if (mapHomeMarkerRef.current) mapHomeMarkerRef.current.setMap(null);
      }
      return;
    }

    // We have alerts, so hide Home marker
    if (mapHomeMarkerRef.current) {
      mapHomeMarkerRef.current.setMap(null);
    }

    // 2. Reconcile Alert Markers (Diffing)
    const bounds = new window.google.maps.LatLngBounds();
    const activeIds = new Set();
    let markersChanged = false;

    filteredAlerts.forEach((alert) => {
      activeIds.add(alert.id);
      bounds.extend({ lat: alert.lat, lng: alert.lng });

      let marker = mapMarkersRef.current.get(alert.id);

      if (!marker) {
        // Create new marker
        markersChanged = true;
        const color = alertBadgeColor[alert.severity] ?? alertBadgeColor.non_urgent;
        marker = new window.google.maps.Marker({
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
        mapMarkersRef.current.set(alert.id, marker);
      } else {
        // Ensure marker is on map (in case it was hidden previously, though unlikely here)
        if (marker.getMap() !== mapInstanceRef.current) {
          marker.setMap(mapInstanceRef.current);
        }
        // Could update position here if alerts move, but assuming static for performance
      }

      // Always update listener to capture fresh closure variables (alert data)
      // This is cheaper than recreating the marker
      window.google.maps.event.clearListeners(marker, 'click');
      marker.addListener('click', () => {
        if (!mapInfoRef.current) {
          mapInfoRef.current = new window.google.maps.InfoWindow();
        }
        // Sentinel: Escape user input to prevent XSS in InfoWindow
        const safeType = escapeHtml(alertBadgeCopy[alert.severity] ?? 'Alert');
        const safeAddress = escapeHtml(alert.address);
        const safeDate = escapeHtml(new Date(alert.date).toLocaleString());

        mapInfoRef.current.setContent(
          `<div style="font-family: sans-serif; max-width: 220px;">
            <strong>${safeType}</strong><br/>
            ${safeAddress}<br/>
            <span style="font-size: 12px;">${safeDate}</span>
          </div>`
        );
        mapInfoRef.current.open(mapInstanceRef.current, marker);
      });
    });

    // Remove stale markers
    const idsToRemove = [];
    mapMarkersRef.current.forEach((_, id) => {
      if (!activeIds.has(id)) {
        idsToRemove.push(id);
      }
    });

    if (idsToRemove.length > 0) {
      markersChanged = true;
      idsToRemove.forEach((id) => {
        const marker = mapMarkersRef.current.get(id);
        marker.setMap(null);
        mapMarkersRef.current.delete(id);
      });
    }

    // Only fit bounds if markers changed (added/removed) or first load to avoid disrupting user panning
    // Use getBounds() check to detect first load
    if (markersChanged || !mapInstanceRef.current.getBounds()) {
      mapInstanceRef.current.fitBounds(bounds);
    }
  }, [filteredAlerts, userLocation, defaultMapCenter]);

  // Bolt: Wrap handlers in useCallback to ensure stable references for React.memo
  const handleAlertFocus = useCallback((alert) => {
    setSelectedAlertId(alert.id);
    if (!mapInstanceRef.current || !window.google?.maps) return;
    const marker = mapMarkersRef.current.get(alert.id);
    if (marker) {
      mapInstanceRef.current.panTo(marker.getPosition());
      mapInstanceRef.current.setZoom(13);
      if (!mapInfoRef.current) {
        mapInfoRef.current = new window.google.maps.InfoWindow();
      }
      // Sentinel: Escape user input to prevent XSS in InfoWindow
      const safeType = escapeHtml(alertBadgeCopy[alert.severity] ?? 'Alert');
      const safeAddress = escapeHtml(alert.address);
      const safeDate = escapeHtml(new Date(alert.date).toLocaleString());

      mapInfoRef.current.setContent(
        `<div style="font-family: sans-serif; max-width: 220px;">
          <strong>${safeType}</strong><br/>
          ${safeAddress}<br/>
          <span style="font-size: 12px;">${safeDate}</span>
        </div>`
      );
      mapInfoRef.current.open(mapInstanceRef.current, marker);
    } else {
      mapInstanceRef.current.panTo({ lat: alert.lat, lng: alert.lng });
      mapInstanceRef.current.setZoom(13);
    }
  }, []);

  const handleClearAlert = useCallback(async (alertId) => {
    if (!user) return; // user is in closure, but user.uid might change. Actually, user ref might change.
    // To be safe, add user as dependency.
    try {
      await setDoc(
        doc(db, "users", user.uid, "emergencyLocations", alertId),
        { clearedAt: serverTimestamp() },
        { merge: true }
      );
    } catch (error) {
      console.error('Failed to clear alert', error);
      const message = error?.message ?? 'Unable to clear alert.';
      setAlertStatus(message);
      // Re-throw so child component can handle state
      throw new Error(message);
    }
  }, [user]);

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
        avatarId: profile.avatarId || null,
        phoneNumber: profile.phoneNumber || '',
        email: profile.email || user.email || ''
      };
      if (payload.email) {
        payload.emailLowercase = payload.email.toLowerCase();
      }
      await setDoc(doc(db, "users", user.uid), payload, { merge: true });
      
      // Sync to public profile
      await setDoc(doc(db, "public_profiles", user.uid), {
        ownerName: payload.ownerName,
        avatarUrl: payload.avatarUrl,
        avatarId: payload.avatarId,
        themePreferences: themePrefs, // Include current theme
        updatedAt: serverTimestamp()
      }, { merge: true });

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

  const handleEditContact = useCallback((contact) => {
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
  }, []);

  const handleSaveContact = async () => {
    if (!user) return;
    if (!contactForm.displayName.trim()) {
      setContactStatus("Display name is required.");
      return;
    }
    setIsSavingContact(true);
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
          ? trustedContacts.find(c => c.id === editingContactId)?.contactOrder ?? trustedContacts.length
          : trustedContacts.length,
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
    } finally {
      setIsSavingContact(false);
    }
  };

  const handleDeleteContact = useCallback(async (contactId) => {
    if (!user) return;
    setContactStatus("Removing contact...");
    try {
      await deleteDoc(doc(db, "users", user.uid, "trustedContacts", contactId));
      setContactStatus("Contact removed.");
    } catch (error) {
      console.error("Delete contact failed", error);
      setContactStatus(error?.message ?? "Delete failed.");
    }
  }, [user]);

  // Stable handlers for TrustedContactRow
  const handleDeleteConfirm = useCallback((id) => {
    handleDeleteContact(id);
    setConfirmDeleteId(null);
  }, [handleDeleteContact]);

  const handleDeleteCancel = useCallback(() => {
    setConfirmDeleteId(null);
  }, []);

  const handleApplyPreset = useCallback(async (presetTheme) => {
    if (!user) return;
    const normalized = normalizeTheme(presetTheme);
    setThemeStatus("Updating theme...");
    try {
      await setDoc(
        doc(db, "users", user.uid),
        { themePreferences: normalized, themeUpdatedAt: serverTimestamp() },
        { merge: true }
      );

      // Sync theme to public profile
      await setDoc(doc(db, "public_profiles", user.uid), {
        themePreferences: normalized,
        updatedAt: serverTimestamp()
      }, { merge: true });

      setThemePrefs(normalized);
      setThemeStatus("Theme synced.");
    } catch (error) {
      console.error("Theme update failed", error);
      setThemeStatus(error?.message ?? "Theme update failed.");
    }
  }, [user]);

  const handleImportPublicTheme = useCallback(async (themeDoc) => {
    if (!themeDoc?.theme) return;
    await handleApplyPreset(themeDoc.theme);
    setThemeGalleryStatus(`Imported "${themeDoc.name}".`);
  }, [handleApplyPreset]);

  const handlePublishTheme = async () => {
    if (!user) return;
    const name = themePublishForm.name.trim();
    if (!name) {
      setThemePublishStatus("Theme name is required.");
      return;
    }
    setIsPublishingTheme(true);
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
    } finally {
      setIsPublishingTheme(false);
    }
  };

  const handleRemoteSettingsSave = async () => {
    if (!user) return;
    setIsSavingSettings(true);
    setRemoteSettingsStatus("Saving settings...");
    try {
      await setDoc(doc(db, "users", user.uid), {
        remoteWebAccessEnabled: remoteSettings.remoteWebAccessEnabled,
        autoUpdateContactInfo: remoteSettings.autoUpdateContactInfo,
        timeFormat: remoteSettings.timeFormat,
        thirdPartyExtensionsEnabled: remoteSettings.thirdPartyExtensionsEnabled,
        beaconLauncherEnabled: remoteSettings.beaconLauncherEnabled,
        otpCleanupEnabled: remoteSettings.otpCleanupEnabled,
        emailFallbackEnabled: remoteSettings.emailFallbackEnabled,
        crashDetectionEnabled: remoteSettings.crashDetectionEnabled,
        aiSummariesEnabled: remoteSettings.aiSummariesEnabled,
        firebaseMessagingEnabled: remoteSettings.firebaseMessagingEnabled,
        mergedExperienceEnabled: remoteSettings.mergedExperienceEnabled,
        privateSafeEnabled: remoteSettings.privateSafeEnabled,
        smartRepliesEnabled: remoteSettings.smartRepliesEnabled,
        settingsUpdatedAt: serverTimestamp()
      }, { merge: true });
      setRemoteSettingsStatus("Settings updated.");
    } catch (error) {
      console.error("Settings update failed", error);
      setRemoteSettingsStatus(error?.message ?? "Settings update failed.");
    } finally {
      setIsSavingSettings(false);
    }
  };

  const requestPhoneSync = async () => {
    if (!user) return;
    setSyncRequestStatus("Requesting sync...");
    try {
      await setDoc(
        doc(db, "users", user.uid),
        {
          syncRequestedAt: serverTimestamp(),
          syncRequestedBy: "web",
          remoteWebAccessEnabled: true,
          settingsUpdatedAt: serverTimestamp()
        },
        { merge: true }
      );
      setSyncRequestStatus("Sync requested. Open PulseLink on your phone and keep it online.");
    } catch (error) {
      console.error("Sync request failed", error);
      setSyncRequestStatus(error?.message ?? "Unable to request sync.");
    }
  };

  const handleQuickSetup = async (mode) => {
    if (!user) return;
    const isEssentials = mode === 'essentials';
    const isPower = mode === 'power';

    // Logic mirroring Android
    const newSettings = { ...remoteSettings };

    if (isEssentials) {
      newSettings.beaconLauncherEnabled = true;
      newSettings.firebaseMessagingEnabled = true;
      newSettings.emailFallbackEnabled = true;
      newSettings.otpCleanupEnabled = true;
      newSettings.aiSummariesEnabled = false;
      newSettings.remoteWebAccessEnabled = false;
      newSettings.crashDetectionEnabled = false;
      newSettings.mergedExperienceEnabled = false;
      newSettings.privateSafeEnabled = false;
      newSettings.smartRepliesEnabled = true;
    } else if (isPower) {
      newSettings.beaconLauncherEnabled = true;
      newSettings.firebaseMessagingEnabled = true;
      newSettings.emailFallbackEnabled = true;
      newSettings.otpCleanupEnabled = true;
      newSettings.aiSummariesEnabled = isPremiumUser; // Check premium
      newSettings.remoteWebAccessEnabled = isPremiumUser;
      newSettings.crashDetectionEnabled = isPremiumUser;
      newSettings.mergedExperienceEnabled = true;
      newSettings.thirdPartyExtensionsEnabled = true;
      newSettings.privateSafeEnabled = true;
      newSettings.smartRepliesEnabled = true;
    }

    setRemoteSettings(newSettings);
    // Auto-save
    try {
      await setDoc(doc(db, "users", user.uid), {
        ...newSettings,
        settingsUpdatedAt: serverTimestamp()
      }, { merge: true });
    } catch (e) {
      console.error("Quick setup failed", e);
    }
  };

  const handleAddExtension = (e) => {
    e?.preventDefault();
    if (!extensionForm.name.trim()) {
      setExtensionStatus('Name is required.');
      return;
    }
    const id = `${Date.now()}`;
    setDevExtensions((prev) => [
      ...prev,
      {
        id,
        name: extensionForm.name.trim(),
        endpoint: extensionForm.endpoint.trim(),
        description: extensionForm.description.trim(),
        sample: false
      }
    ]);
    setExtensionForm({ name: '', endpoint: '', description: '' });
    setExtensionStatus('Saved extension to your device.');
  };

  const handleTestExtension = async (ext) => {
    if (!remoteSettings.thirdPartyExtensionsEnabled) {
      setExtensionStatus('Enable third-party extensions in Settings first.');
      return;
    }
    setExtensionStatus(`Testing ${ext.name}...`);
    if (!ext.endpoint) {
      setExtensionStatus(`${ext.name} is active locally (no endpoint needed).`);
      return;
    }
    try {
      const resp = await fetch(ext.endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          event: 'ping',
          user: user?.uid ?? 'anonymous',
          sampleMessage: messages[0]?.body ?? 'hello from PulseLink',
          timestamp: Date.now()
        })
      });
      const text = await resp.text();
      setExtensionStatus(`Response from ${ext.name}: ${text.slice(0, 180)}`);
    } catch (err) {
      setExtensionStatus(`Couldn't reach ${ext.name}: ${err.message}`);
    }
  };

  const handleDeleteAccount = async () => {
    if (!user) return;
    if (!window.confirm("Delete your account and all cloud data? This cannot be undone.")) {
      return;
    }
    setDeleteAction('account');
    setDeleteStatus("Requesting account deletion...");
    try {
      const callable = httpsCallable(functions, "deleteAccount");
      await callable();
      setDeleteStatus("Account deletion requested.");
      await signOut(auth);
    } catch (error) {
      console.error("Delete account failed", error);
      setDeleteStatus(error?.message ?? "Delete account failed.");
    } finally {
      setDeleteAction(null);
    }
  };

  const handleDeleteAccountData = async () => {
    if (!user) return;
    if (!window.confirm("Clear synced messages, device contacts, and trusted contacts from the cloud?")) {
      return;
    }
    setDeleteAction('data');
    setDeleteStatus("Deleting account data...");
    try {
      const batch = writeBatch(db);
      const trustedSnap = await getDocs(collection(db, "users", user.uid, "trustedContacts"));
      trustedSnap.docs.forEach(docSnap => batch.delete(docSnap.ref));

      const deviceSnap = await getDocs(collection(db, "users", user.uid, "deviceContacts"));
      deviceSnap.docs.forEach(docSnap => batch.delete(docSnap.ref));

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
    } finally {
      setDeleteAction(null);
    }
  };

  const handleLogout = useCallback(async () => {
    await signOut(auth);
    setSelectedThread(null);
    setActivePanel('home');
  }, []);

  // Bolt: Stable handler for new thread button
  const handleNewThread = useCallback(() => {
    setActivePanel('beacon');
    setSelectedThread(null);
  }, []);

  // Bolt: Stable handler to prevent ghost content when switching threads
  const handleThreadSelect = useCallback((thread) => {
    setMessages([]); // Clear previous messages immediately
    setSelectedThread(thread);
    if (thread?.lineId) {
      setActiveLineId((prev) => prev ?? thread.lineId);
    }
  }, []);

  const combinedThreads = useMemo(() => {
    if (lineInboxMode === 'PER_LINE') return [];
    const lineFlattened = Object.values(lineThreads).flat();

    // Create a Set of addresses present in the new line threads to filter out legacy duplicates
    const lineAddresses = new Set(lineFlattened.map(t => t.address).filter(Boolean));
    const uniqueLegacy = legacyThreads.filter(t => !t.address || !lineAddresses.has(t.address));

    const all = [...uniqueLegacy, ...lineFlattened];
    return all.sort((a, b) => (b.date ?? 0) - (a.date ?? 0));
  }, [legacyThreads, lineThreads, lineInboxMode]);

  const activeLineThreads = useMemo(() => {
    if (lineInboxMode === 'COMBINED') return combinedThreads;
    const chosenLine = activeLineId || lines[0]?.id || null;
    const current = chosenLine ? lineThreads[chosenLine] || [] : [];
    return [...current].sort((a, b) => (b.date ?? 0) - (a.date ?? 0));
  }, [lineInboxMode, activeLineId, lines, lineThreads, combinedThreads]);


  const isPremium = isPremiumUser;
  const tierLabel = isPremiumUser ? 'Premium' : (isProUser ? 'Pro' : 'Free');
  const hasBeaconData = isPremiumUser || lines.length > 0 || legacyThreads.length > 0;

  const navLogo = useMemo(() => {
     if (remoteSettings.mergedExperienceEnabled) {
         return beaconLogo;
     }
     return logo;
  }, [remoteSettings.mergedExperienceEnabled]);

  if (!user) {
    return (
      <div className={`app-shell ${themePrefs.useGlassEffect ? 'glass-mode' : ''} ${themePrefs.useHolographicGlow ? 'holographic-mode' : ''}`} style={themeVars}>
        <div className="noise-overlay" />
        {import.meta.env.DEV && <DevTools isVisible={showDevTools} onClose={() => setShowDevTools(false)} />}
        <a href="#main-content" className="skip-link">Skip to main content</a>
        <div className="container login-container" id="main-content">
          <div className="login-card neon-border">
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
              {authError && <div className="auth-error" role="alert">{authError}</div>}
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
    <div className={`app-shell ${themePrefs.useGlassEffect ? 'glass-mode' : ''} ${themePrefs.useHolographicGlow ? 'holographic-mode' : ''}`} style={themeVars}>
      <div className="noise-overlay" />
      {import.meta.env.DEV && <DevTools isVisible={showDevTools} onClose={() => setShowDevTools(false)} />}
      <a href="#main-content" className="skip-link">Skip to main content</a>
      <div className="app-container">
        <Sidebar
          activePanel={activePanel}
          setActivePanel={setActivePanel}
          tierLabel={tierLabel}
          handleLogout={handleLogout}
          handleNewThread={handleNewThread}
          lines={lines}
          activeLineId={activeLineId}
          setActiveLineId={setActiveLineId}
          lineInboxMode={lineInboxMode}
          isLoadingThreads={isLoadingThreads}
          isPremium={isPremium}
          remoteSettings={remoteSettings}
          navLogo={navLogo}
          brandTitle={remoteSettings.mergedExperienceEnabled ? "PulseLink Unified" : "PulseLink Suite"}
          threads={activeLineThreads}
          selectedThreadId={selectedThread?.id}
          onSelect={handleThreadSelect}
          showPreviews={showPreviews}
        />
        <div className="main-content" id="main-content">
          {activePanel === 'home' && (
            <div className="home-panel">
              <div className="home-hero">
                <h2>Welcome back</h2>
                <p>Choose what you want to manage on PulseLink Web.</p>
              </div>
              {/* Web app info tooltip - fixes #236: Users need to know about web app availability */}
              {/* QA TEST: Visit web app home screen after login */}
              {/* EXPECTED: Blue info banner should be visible explaining web access */}
              {/* EXPECTED: Banner should display icon, bold heading, and feature description */}
              {showWebHint && (
                <div className="web-app-hint">
                  <button
                    className="hint-dismiss"
                    type="button"
                    aria-label="Dismiss web access notice"
                    onClick={() => {
                      setShowWebHint(false);
                      localStorage.setItem(webHintStorageKey, 'true');
                    }}
                  >
                    x
                  </button>
                  <div className="hint-icon">??</div>
                  <div className="hint-content">
                    <strong>Access PulseLink Web anytime:</strong> Visit pulselink.damiennichols.com (or app.damiennichols.com / pulselink-24899.web.app) from any browser to manage contacts, view synced messages, customize themes, and track emergency locations. All settings sync automatically with your mobile app.
                  </div>
                </div>
              )}
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
                  <p>Browse all device contacts synced from your phone.</p>
                </button>
                <button className="home-card" onClick={() => setActivePanel('beacon')}>
                  <div className="home-icon beacon">
                    <img src={beaconLogo} alt="Beacon" />
                  </div>
                  <h3>Beacon Inbox</h3>
                  <p>View SMS synced from your phone.</p>
                </button>
                <button className="home-card" onClick={() => setActivePanel('ringersong')}>
                  <div className="home-icon ringersong">
                    <img src={ringersongLogo} alt="RingerSong" />
                  </div>
                  <h3>RingerSong</h3>
                  <p>Manage ringtone progressions and streaming.</p>
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
                <button
                  className="home-card"
                  onClick={() => setActivePanel('extensions')}
                  disabled={!remoteSettings.thirdPartyExtensionsEnabled}
                  title={remoteSettings.thirdPartyExtensionsEnabled ? "Manage extensions" : "Enable 3rd-party extensions in Settings"}
                >
                  <div className="home-icon pulselink">
                    <img src={logo} alt="Extensions" />
                  </div>
                  <h3>Extensions</h3>
                  <p>{remoteSettings.thirdPartyExtensionsEnabled ? "Attach 3rd-party add-ons (coming soon)" : "Enable 3rd-party extensions to start."}</p>
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
                  <div className="profile-header-row">
                    <div className="profile-avatar-preview">
                      {profile.avatarId ? (
                        <img
                          src={avatarPresets.find(p => p.id === profile.avatarId)?.src}
                          alt="Avatar"
                          className="profile-avatar-img"
                        />
                      ) : (
                        profile.avatarUrl ? (
                          <img src={profile.avatarUrl} alt="Avatar" className="profile-avatar-img" />
                        ) : (
                          <div className="profile-avatar-placeholder">?</div>
                        )
                      )}
                    </div>
                    {unlockedAvatars.length > 0 && (
                      <div className="avatar-selector">
                        {unlockedAvatars.map(av => (
                          <button
                            key={av.id}
                            onClick={() => setProfile(prev => ({ ...prev, avatarId: av.id }))}
                            className={`avatar-option-btn ${profile.avatarId === av.id ? 'active' : ''}`}
                            title={av.name}
                          >
                            <img src={av.src} alt={av.name} className="avatar-option-img" />
                          </button>
                        ))}
                        <button
                          onClick={() => setProfile(prev => ({ ...prev, avatarId: '' }))}
                          className={`avatar-option-btn ${!profile.avatarId ? 'active' : ''}`}
                          title="Use Custom URL"
                          aria-label="Use custom avatar URL"
                        >
                          <LinkIcon />
                        </button>
                      </div>
                    )}
                  </div>
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
                    aria-busy={isSavingProfile}
                  >
                    {isSavingProfile ? (
                      <>
                        <Spinner />
                        Saving...
                      </>
                    ) : 'Save profile'}
                  </button>
                    {profileStatus && <div className={getToastClass(profileStatus)} role="status" aria-live="polite">{profileStatus}</div>}
                </div>
                <div className="settings-card">
                  <h4>Trusted contacts</h4>
                  <div className="contact-list">
                    {trustedContacts.map((contact) => (
                      <TrustedContactRow
                        key={contact.id}
                        contact={contact}
                        isConfirmingDelete={confirmDeleteId === contact.id}
                        onEdit={handleEditContact}
                        onDeleteRequest={setConfirmDeleteId}
                        onDeleteConfirm={handleDeleteConfirm}
                        onDeleteCancel={handleDeleteCancel}
                      />
                    ))}
                    {trustedContacts.length === 0 && (
                      <div className="settings-note">No trusted contacts yet.</div>
                    )}
                  </div>
                  {contactStatus && <div className={getToastClass(contactStatus)} role="status" aria-live="polite">{contactStatus}</div>}
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
                    <button
                      className="primary-btn"
                      onClick={handleSaveContact}
                      disabled={isSavingContact}
                      aria-busy={isSavingContact}
                    >
                      {isSavingContact ? (
                        <>
                          <Spinner />
                          {editingContactId ? 'Updating...' : 'Saving...'}
                        </>
                      ) : (editingContactId ? 'Update contact' : 'Add contact')}
                    </button>
                    <button className="ghost-btn" onClick={resetContactForm} disabled={isSavingContact}>
                      Clear
                    </button>
                  </div>
                  {contactStatus && <div className={getToastClass(contactStatus)} role="status" aria-live="polite">{contactStatus}</div>}
                </div>
              </div>
            </div>
          )}

          {activePanel === 'contacts' && (
            <div className="contacts-panel">
              <div className="panel-header">
                <h3>Contacts</h3>
                <p>Browse all device contacts synced from your phone.</p>
              </div>
              <div className="contacts-toolbar">
                <div className="contact-count">
                  {filteredDeviceContacts.length} contact{filteredDeviceContacts.length === 1 ? '' : 's'}
                </div>
                <div className="sidebar-actions" style={{ flex: 1 }}>
                  <input
                    className="login-input contact-search"
                    placeholder="Search by name, phone, or email"
                    aria-label="Search contacts"
                    value={contactSearch}
                    onChange={(e) => setContactSearch(e.target.value)}
                    style={{ flex: 1 }}
                  />
                  {contactSearch && (
                    <button
                      className="secondary-btn"
                      onClick={() => setContactSearch('')}
                      aria-label="Clear search"
                      title="Clear search"
                    >
                      Clear
                    </button>
                  )}
                </div>
              </div>
              <div className="contact-list contact-list--full">
                {contactListElements}
                {filteredDeviceContacts.length > contactListLimit && (
                  <button
                    className="secondary-btn"
                    style={{ marginTop: '16px', width: '100%' }}
                    onClick={() => setContactListLimit(prev => prev + 50)}
                  >
                    Show more contacts
                  </button>
                )}
                {filteredDeviceContacts.length === 0 && (
                  <div className="settings-note">
                    {contactSearch.trim()
                      ? 'No contacts match that search.'
                      : 'No device contacts synced yet.'}
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
              {!user && (
                <div className="settings-card" style={{ marginBottom: 20 }}>
                  <h4>Sign in to view alerts</h4>
                  <p className="settings-note">Emergency locations are secured per account. Please sign in to load your map.</p>
                </div>
              )}
              {user && (
              <>
              <div className="map-controls">
                <button
                  className="secondary-btn"
                  onClick={() => window.location.reload()}
                  aria-label="Reload page"
                >
                  Reload
                </button>
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
                  {mapStatus && <div className={getToastClass(mapStatus)} role="status" aria-live="polite">{mapStatus}</div>}
                </div>
                <div className="map-list">
                  {filteredAlerts.map((alert) => (
                    <MapAlertItem
                      key={alert.id}
                      alert={alert}
                      isActive={selectedAlertId === alert.id}
                      onFocus={handleAlertFocus}
                      onClear={handleClearAlert}
                    />
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
              </>
              )}
            </div>
          )}

          {activePanel === 'ringersong' && (
            <div className="pulselink-panel">
              <div className="ringersong-header">
                <div
                  className="ringersong-logo-container"
                  style={{
                      maskImage: `url(${ringersongLogo})`,
                      WebkitMaskImage: `url(${ringersongLogo})`
                  }}
                />
                <div>
                    <h3>RingerSong</h3>
                    <p style={{color: 'var(--muted)'}}>Progressive ringtone streaming & playlist manager.</p>
                </div>
              </div>

              <div className="pulselink-grid">
                <div className="settings-card">
                    <div className="card-header-row" style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20}}>
                        <h4>Current Playlist</h4>
                        <span className="badge" style={{background: 'var(--accent)', color: '#fff', padding: '2px 8px', borderRadius: 12, fontSize: '0.8em'}}>{ringerPlaylist.length} songs</span>
                    </div>
                    
                    {ringerPlaylist.length === 0 ? (
                        <div className="empty-state">
                            <p className="muted">Your playlist is empty. Add songs to start streaming.</p>
                        </div>
                    ) : (
                        <div className="song-grid">
                            {ringerPlaylist.map(song => (
                                <RingerSongItem
                                    key={song.id}
                                    song={song}
                                    onDelete={handleDeleteRingerSong}
                                />
                            ))}
                        </div>
                    )}
                </div>
              
                <div className="settings-card">
                    <h4>Add Music</h4>
                    <div className="search-container">
                        <div className="search-input-wrapper">
                            <input
                                className="login-input"
                                placeholder="Search Spotify for songs..."
                                aria-label="Search Spotify for songs"
                                value={spotifySearch}
                                onChange={(e) => setSpotifySearch(e.target.value)}
                                onKeyDown={(e) => e.key === 'Enter' && handleSpotifySearch()}
                            />
                            <button
                                className="primary-btn"
                                onClick={handleSpotifySearch}
                                disabled={isSearchingSpotify}
                                aria-busy={isSearchingSpotify}
                            >
                                {isSearchingSpotify ? "Searching..." : "Search"}
                            </button>
                        </div>
                    </div>

                    {spotifyResults.length > 0 && (
                        <div className="song-grid">
                            {spotifyResults.map(track => (
                                <SpotifyResultItem
                                    key={track.id}
                                    track={track}
                                    onAdd={handlePushSpotifyTrack}
                                    isAdding={addingTrackId === track.id}
                                />
                            ))}
                        </div>
                    )}
                    
                    {settingsStatus && <div className={getToastClass(settingsStatus)} style={{marginTop: 12}} role="status" aria-live="polite">{settingsStatus}</div>}
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
                        aria-label="Search themes"
                      />
                    </label>
                    <button className="secondary-btn" type="button" onClick={() => setThemeSearch('')}>
                      Clear
                    </button>
                  </div>
                  <div className="theme-gallery-grid">
                    {filteredThemes.map((themeDoc) => (
                      <ThemeGalleryItem
                        key={themeDoc.id}
                        themeDoc={themeDoc}
                        onImport={handleImportPublicTheme}
                      />
                    ))}
                    {filteredThemes.length === 0 && (
                      <div className="theme-empty">
                        No themes found.
                      </div>
                    )}
                  </div>
                  {themeGalleryStatus && <div className={getToastClass(themeGalleryStatus)} role="status" aria-live="polite">{themeGalleryStatus}</div>}
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
                  <button
                    className="primary-btn"
                    type="button"
                    onClick={handlePublishTheme}
                    disabled={isPublishingTheme}
                    aria-busy={isPublishingTheme}
                  >
                    {isPublishingTheme ? (
                      <>
                        <Spinner />
                        Publishing...
                      </>
                    ) : 'Publish theme'}
                  </button>
                  {themePublishStatus && <div className={getToastClass(themePublishStatus)} role="status" aria-live="polite">{themePublishStatus}</div>}
                </div>
                <div className="settings-card themes-card">
                  <h4>Quick presets</h4>
                  <div className="theme-grid">
                    {themePresets.map((preset) => (
                      <ThemePresetItem
                        key={preset.name}
                        preset={preset}
                        onApply={handleApplyPreset}
                      />
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
                  {themeStatus && <div className={getToastClass(themeStatus)} role="status" aria-live="polite">{themeStatus}</div>}
                </div>
              </div>
            </div>
          )}

          {activePanel === 'extensions' && (
            <div className="pulselink-panel">
              <div className="panel-header">
                <h3>Extensions</h3>
                <p>Enhance your PulseLink experience with powerful add-ons.</p>
              </div>

              <div className="settings-card" style={{marginBottom: 20}}>
                <h4>Quick Setup</h4>
                <div className="settings-row" style={{alignItems: 'stretch', gap: 16}}>
                  <button className="home-card" style={{margin: 0, flex: 1, textAlign: 'left', alignItems: 'flex-start'}} onClick={() => handleQuickSetup('essentials')}>
                    <div className="home-icon" style={{width: 40, height: 40, background: 'rgba(34, 211, 238, 0.1)', color: 'var(--accent)'}}>
                      <BoltIcon />
                    </div>
                    <h4 style={{marginTop: 8}}>Essentials</h4>
                    <p style={{fontSize: '0.9em', color: 'var(--muted)', margin: 0}}>Just the basics: Beacon, Relay, Email Backup, and OTP Cleanup.</p>
                  </button>
                  <button className="home-card" style={{margin: 0, flex: 1, textAlign: 'left', alignItems: 'flex-start'}} onClick={() => handleQuickSetup('power')}>
                    <div className="home-icon" style={{width: 40, height: 40, background: 'rgba(34, 211, 238, 0.1)', color: 'var(--accent)'}}>
                      <StarIcon />
                    </div>
                    <h4 style={{marginTop: 8}}>Power User</h4>
                    <p style={{fontSize: '0.9em', color: 'var(--muted)', margin: 0}}>Everything enabled: AI, Crash Detection, Web Access, and more.</p>
                  </button>
                </div>
              </div>

              {[
                {
                  title: "Core",
                  items: [
                    { id: 'beaconLauncherEnabled', name: 'Beacon Inbox', desc: 'Separate launcher icon for quick access to your SMS inbox.', icon: beaconLogo, isImg: true },
                    { id: 'firebaseMessagingEnabled', name: 'Firebase Relay', desc: 'Faster messaging between PulseLink users.', icon: <CloudSyncIcon /> }
                  ]
                },
                {
                  title: "Safety & Security",
                  items: [
                    { id: 'emailFallbackEnabled', name: 'Email Backup', desc: 'Forward urgent alerts to email if SMS fails.', icon: <EmailIcon /> },
                    { id: 'crashDetectionEnabled', name: 'Crash Detection', desc: 'Detects car crashes and notifies emergency contacts.', icon: <CarCrashIcon />, premium: true },
                    { id: 'privateSafeEnabled', name: 'Private Safe', desc: 'Lock and hide sensitive conversations.', icon: <LockIcon /> }
                  ]
                },
                {
                  title: "Smart Features",
                  items: [
                    { id: 'smartRepliesEnabled', name: 'Smart Replies', desc: 'One-tap suggestion chips for incoming messages.', icon: <MessageSquareIcon /> },
                    { id: 'otpCleanupEnabled', name: 'Smart OTP Cleanup', desc: 'Automatically deletes one-time passwords after 24 hours.', icon: <DeleteSweepIcon /> },
                    { id: 'aiSummariesEnabled', name: 'PulseLink AI', desc: 'Smart summaries and urgency detection for your chats.', icon: <SmartToyIcon />, premium: true }
                  ]
                },
                {
                  title: "Integrations",
                  items: [
                    { id: 'remoteWebAccessEnabled', name: 'Remote Web Access', desc: 'Sync messages and contacts to this web portal.', icon: logo, isImg: true, premium: true },
                    { id: 'mergedExperienceEnabled', name: 'Unified Home', desc: 'Merge PulseLink and Beacon navigation into a single simplified experience.', icon: <HomeIcon />, premium: true },
                    { id: 'thirdPartyExtensionsEnabled', name: '3rd Party Extensions', desc: 'Allow community-built plugins (Beta).', icon: <ExtensionIcon />, premium: true }
                  ]
                }
              ].map((category) => (
                <div key={category.title} className="extension-category" style={{marginBottom: 32}}>
                  <h4 style={{marginBottom: 16, color: 'var(--ink)'}}>{category.title}</h4>
                  <div className="home-grid">
                    {category.items.map(ext => {
                      const isEnabled = remoteSettings[ext.id];
                      const isLocked = ext.premium && !isPremiumUser;

                      return (
                        <div className="home-card" key={ext.id} style={{ opacity: isLocked ? 0.6 : 1, position: 'relative' }}>
                          <div className="home-icon" style={{
                             background: ext.isImg ? 'transparent' : 'rgba(255, 255, 255, 0.05)',
                             display: 'grid',
                             placeItems: 'center'
                          }}>
                            {ext.isImg ? <img src={ext.icon} alt={ext.name} /> : ext.icon}
                          </div>
                          <h3 style={{marginTop: 12, marginBottom: 4}}>{ext.name}</h3>
                          <p style={{marginBottom: 16, minHeight: 40}}>{ext.desc}</p>

                          {isLocked ? (
                            <div className="badge badge-premium" style={{background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--muted)'}}>
                              Premium Required
                            </div>
                          ) : (
                            <button
                              className={isEnabled ? "secondary-btn" : "primary-btn"}
                              style={{width: '100%'}}
                              onClick={() => {
                                setRemoteSettings(prev => ({ ...prev, [ext.id]: !prev[ext.id] }));
                                const next = { ...remoteSettings, [ext.id]: !isEnabled };
                                setDoc(doc(db, "users", user.uid), {
                                  ...next,
                                  settingsUpdatedAt: serverTimestamp()
                                }, { merge: true });
                              }}
                              aria-label={isEnabled ? `Remove ${ext.name}` : `Install ${ext.name}`}
                            >
                              {isEnabled ? "Remove" : "Install"}
                            </button>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}

              <div className="settings-card">
                <h4>Developer sandbox</h4>
                <p className="settings-note">Add webhook-style extensions that run against your own account. Stored locally so you can iterate safely.</p>
                {!remoteSettings.thirdPartyExtensionsEnabled && (
                  <div className="settings-warning">Extensions stay dormant until you enable 3rd-party access in Settings.</div>
                )}
                <div className="extensions-list" style={{display: 'grid', gap: 12}}>
                  {devExtensions.map((ext) => (
                    <div key={ext.id} className="home-card" style={{margin: 0}}>
                      <h4>{ext.name}</h4>
                      <p className="settings-note">{ext.description || ext.endpoint || 'No description provided.'}</p>
                      {ext.endpoint && <code className="mono" style={{fontSize: 12}}>{ext.endpoint}</code>}
                      <div className="settings-row" style={{justifyContent: 'flex-start', gap: 8, marginTop: 8}}>
                        <button className="secondary-btn" type="button" onClick={() => handleTestExtension(ext)}>Test</button>
                        <button className="ghost-btn" type="button" onClick={() => setDevExtensions((prev) => prev.filter((d) => d.id !== ext.id))}>Remove</button>
                      </div>
                    </div>
                  ))}
                  {devExtensions.length === 0 && <p className="settings-note">No custom extensions yet.</p>}
                </div>
                <form className="login-form" style={{marginTop: 12}} onSubmit={handleAddExtension}>
                  <label className="login-field">
                    Name
                    <input className="login-input" value={extensionForm.name} onChange={(e) => setExtensionForm((prev) => ({ ...prev, name: e.target.value }))} />
                  </label>
                  <label className="login-field">
                    Webhook URL (https://…)
                    <input className="login-input" value={extensionForm.endpoint} onChange={(e) => setExtensionForm((prev) => ({ ...prev, endpoint: e.target.value }))} />
                  </label>
                  <label className="login-field">
                    Description
                    <textarea className="login-input" rows={2} value={extensionForm.description} onChange={(e) => setExtensionForm((prev) => ({ ...prev, description: e.target.value }))} />
                  </label>
                  <button type="submit" className="primary-btn">Save extension</button>
                </form>
                {extensionStatus && <div className={getToastClass(extensionStatus)} role="status">{extensionStatus}</div>}
              </div>
              <div className="settings-card">
                <h4>Submit to gallery</h4>
                <p className="settings-note">Share your extension with other testers.</p>
                <div className="settings-row" style={{gap: 8, flexWrap: 'wrap'}}>
                  <a className="secondary-btn" href="https://github.com/DamienLove/pulselink/blob/Suite-Beta/docs/extensions-dev.md" target="_blank" rel="noreferrer">Read dev guide</a>
                  <a className="ghost-btn" href="mailto:extensions@pulselink.app?subject=PulseLink%20Extension%20Submission">Email us your zip</a>
                </div>
              </div>
            </div>
          )}

          {activePanel === 'settings' && (
            <div className="settings-panel">
              <div className="settings-header">
                <h3>Settings</h3>
                <p>Manage account details and shared preferences.</p>
              </div>

              <div className="settings-search-container">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{opacity: 0.5}}>
                  <circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input
                  className="settings-search-input"
                  placeholder="Search settings..."
                  aria-label="Search settings"
                  value={settingsSearch}
                  onChange={(e) => setSettingsSearch(e.target.value)}
                />
                {settingsSearch && (
                  <button
                    className="ghost-btn icon-only"
                    onClick={() => setSettingsSearch('')}
                    aria-label="Clear search"
                    title="Clear search"
                    style={{ width: '28px', height: '28px' }}
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="18" y1="6" x2="6" y2="18"></line>
                      <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                  </button>
                )}
              </div>

              <div className="settings-grid">
                {(() => {
                  const term = settingsSearch.toLowerCase().trim();
                  const show = (keywords) => {
                    if (!term) return true;
                    return keywords.some(k => k.includes(term));
                  };

                  return (
                    <>
                      {show(['account', 'email', 'user id', 'password', 'reset', 'sign out', 'logout', 'profile']) && (
                        <div className="settings-card">
                          <h4>Account</h4>
                          <div className="settings-row">
                            <span className="settings-label">Signed in as</span>
                            <span className="settings-value">{user.email || 'Unknown'}</span>
                          </div>
                          <div className="settings-row">
                            <span className="settings-label">User ID</span>
                            <span className="settings-value mono">
                              {user.uid}
                              <CopyButton text={user.uid} label="Copy User ID" />
                            </span>
                          </div>
                          <button className="secondary-btn" type="button" onClick={handlePasswordResetForUser}>
                            Send password reset email
                          </button>
                          {settingsStatus && <div className={getToastClass(settingsStatus)} role="status" aria-live="polite">{settingsStatus}</div>}
                        </div>
                      )}

                      {show(['web', 'previews', 'scroll', 'auto-scroll', 'message previews', 'browser']) && (
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
                      )}

                      {show(['pulselink', 'remote', 'web access', 'contact info', 'extensions', '3rd party', 'time format', 'sync']) && (
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
                          <label className="settings-toggle">
                            <input
                              type="checkbox"
                              checked={remoteSettings.thirdPartyExtensionsEnabled}
                              onChange={(e) => setRemoteSettings((prev) => ({ ...prev, thirdPartyExtensionsEnabled: e.target.checked }))}
                            />
                            Enable 3rd-party extensions (beta)
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
                          <button
                            className="secondary-btn"
                            type="button"
                            onClick={handleRemoteSettingsSave}
                            disabled={isSavingSettings}
                            aria-busy={isSavingSettings}
                          >
                            {isSavingSettings ? (
                              <>
                                <Spinner />
                                Saving...
                              </>
                          ) : 'Save PulseLink settings'}
                          </button>
                          <div className="settings-row">
                            <span className="settings-label">Web sync</span>
                            <span className="settings-value">
                              {syncDiagnostics
                                ? `${new Date(toMillis(syncDiagnostics.timestamp)).toLocaleString()} • ${syncDiagnostics.status}`
                                : 'No sync data yet'}
                            </span>
                          </div>
                          {syncDiagnostics && (
                            <p className="settings-note">
                              Threads: {syncDiagnostics.threadCount ?? 0} · Messages: {syncDiagnostics.messageCount ?? 0} · READ_SMS: {syncDiagnostics.hasReadSms ? 'yes' : 'no'} · App: {syncDiagnostics.appVersion ?? 'unknown'}
                            </p>
                          )}
                          <button
                            className="secondary-btn"
                            type="button"
                            onClick={requestPhoneSync}
                          >
                            Request phone sync
                          </button>
                          {remoteSettingsStatus && <div className={getToastClass(remoteSettingsStatus)} role="status" aria-live="polite">{remoteSettingsStatus}</div>}
                          {syncRequestStatus && <div className={getToastClass(syncRequestStatus)} role="status" aria-live="polite">{syncRequestStatus}</div>}
                        </div>
                      )}

                      {show(['data', 'delete', 'clear', 'cloud', 'account data', 'remove', 'privacy']) && (
                        <div className="settings-card">
                          <h4>Account data</h4>
                          <p className="settings-note">
                            Delete account removes your login and all cloud data. Clear data keeps your login but deletes synced content.
                          </p>
                          <div className="contact-actions">
                            <button
                              className="secondary-btn"
                              type="button"
                              onClick={handleDeleteAccountData}
                              disabled={!!deleteAction}
                            >
                              {deleteAction === 'data' ? "Clearing..." : "Clear cloud data"}
                            </button>
                            <button
                              className="primary-btn"
                              type="button"
                              onClick={handleDeleteAccount}
                              disabled={!!deleteAction}
                            >
                              {deleteAction === 'account' ? "Deleting..." : "Delete account"}
                            </button>
                          </div>
                          {deleteStatus && <div className={getToastClass(deleteStatus)} role="status" aria-live="polite">{deleteStatus}</div>}
                        </div>
                      )}
                    </>
                  );
                })()}
              </div>
            </div>
          )}

          {activePanel === 'beacon' && (
            hasBeaconData ? (
              <>
      {lineInboxMode === 'PER_LINE' && lines.length > 0 && (
        <div className="line-tabs line-tabs--main">
          <div className="line-tabs-header">
            <h4>Inbox lines</h4>
            <div className="chip-row">
              <button
                className={`chip ${!activeLineId ? 'active' : ''}`}
                onClick={() => setActiveLineId(null)}
              >
                All
              </button>
              {lines.map((line) => (
                <button
                  key={line.id}
                  className={`chip ${activeLineId === line.id ? 'active' : ''}`}
                  onClick={() => setActiveLineId(line.id)}
                  title={line.phoneNumber || 'Line'}
                >
                  {line.label || line.phoneNumber || line.id.slice(0, 6)}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
                {selectedThread ? (
                  <>
                    <div className="chat-header">
                      <div>
                        <h3>{selectedThread.address}</h3>
                        {lineInboxMode === 'PER_LINE' && selectedThread.lineId && (
                          <div className="chat-subtitle">From line {lines.find(l => l.id === selectedThread.lineId)?.label || selectedThread.lineId.slice(0,6)}</div>
                        )}
                      </div>
                    </div>
                    <div className="messages-list">
                      {messageListElements}
                      <div ref={messagesEndRef} />
                    </div>
                  </>
                ) : (
                  <div className="empty-state">
                    <img src={beaconLogo} alt="Beacon" className="empty-logo" />
                    <div>Select a thread or start a new message</div>
                  </div>
                )}
                <MessageComposer
                  user={user}
                  db={db}
                  selectedThread={selectedThread}
                  lineInboxMode={lineInboxMode}
                  activeLineId={activeLineId}
                  lines={lines}
                  isLoggingIn={isLoggingIn}
                />
              </>
            ) : (
              <div className="empty-state">
                <img src={beaconLogo} alt="Beacon" className="empty-logo" style={{ marginBottom: '24px', opacity: 1, filter: 'none' }} />
                <h2 style={{ marginBottom: '16px' }}>Beacon Inbox</h2>
                <div className="badge badge-premium" style={{ background: 'var(--accent)', color: '#fff', marginBottom: '24px' }}>
                  Premium Feature
                </div>
                <p style={{ maxWidth: '400px', textAlign: 'center', lineHeight: '1.6', color: 'var(--muted)' }}>
                  Upgrade to PulseLink Premium to access your SMS messages directly from the web.
                  Send and receive texts even when your phone is in the other room.
                </p>
                <button
                  className="primary-btn"
                  style={{ marginTop: '32px' }}
                  onClick={() => setActivePanel('settings')}
                >
                  Go to Settings
                </button>
              </div>
            )
          )}
        </div>
      </div>
    </div>
  );
}

export default App;

