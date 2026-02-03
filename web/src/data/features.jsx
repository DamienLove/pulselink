import React from 'react';
import logo from '../assets/pulselink-pro-logo.png';
import beaconLogo from '../assets/beacon-logo.png';
import ringersongLogo from '../assets/ringersong-logo.png';
import {
  CloudSyncIcon,
  MapIcon,
  ContactIcon,
  ThemeIcon,
  EmailIcon,
  CarCrashIcon,
  LockIcon,
  MessageSquareIcon,
  DeleteSweepIcon,
  SmartToyIcon,
  HomeIcon,
  ExtensionIcon,
  SearchIcon
} from '../Icons';

export const features = [
  {
    title: "Core",
    items: [
      { id: 'beaconLauncherEnabled', name: 'Beacon Inbox', desc: 'Separate launcher icon for quick access to your SMS inbox.', icon: beaconLogo, isImg: true },
      { id: 'firebaseMessagingEnabled', name: 'Firebase Relay', desc: 'Faster messaging between PulseLink users.', icon: <CloudSyncIcon /> }
    ]
  },
  {
    title: "PulseLink Apps",
    items: [
      { id: 'ringerSongEnabled', name: 'RingerSong', desc: 'Progressive ringtone streaming & playlist manager.', icon: ringersongLogo, isImg: true },
      { id: 'mapEnabled', name: 'Emergency Map', desc: 'Track shared locations from PulseLink alerts.', icon: <MapIcon /> },
      { id: 'contactsEnabled', name: 'Contacts Manager', desc: 'Browse and manage synced device contacts.', icon: <ContactIcon /> },
      { id: 'themesEnabled', name: 'Theme Gallery', desc: 'Browse, import, and publish custom themes.', icon: <ThemeIcon /> }
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
      { id: 'thirdPartyExtensionsEnabled', name: '3rd Party Extensions', desc: 'Allow community-built plugins (Beta).', icon: <ExtensionIcon />, premium: true },
      { id: 'truecallerEnabled', name: 'Truecaller Caller ID', desc: 'Identify unknown callers and block spam using Truecaller directory.', icon: <SearchIcon /> }
    ]
  }
];
