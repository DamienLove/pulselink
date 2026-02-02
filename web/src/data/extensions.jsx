import React from 'react';
import {
  CloudSyncIcon,
  MapPinIcon,
  ShieldCheckIcon,
  RadioIcon,
  HeartPulseIcon,
  SparklesIcon,
  BoltIcon,
  EmailIcon,
  SmartToyIcon,
  CarCrashIcon,
  LockIcon,
  MessageSquareIcon,
  ContactIcon
} from '../Icons';

export const EXTENSIONS_LIST = [
  {
    category: 'Communication',
    items: [
      {
        id: 'beaconEnabled',
        name: 'Beacon Inbox',
        description: 'Sync SMS messages from your phone. Send and receive texts remotely.',
        icon: <RadioIcon className="w-6 h-6 text-laser-blue" />,
        premium: true,
        settingKey: 'beaconEnabled'
      },
      {
        id: 'firebaseMessagingEnabled',
        name: 'Firebase Relay',
        description: 'Instant cloud messaging for real-time alerts and status updates.',
        icon: <BoltIcon className="w-6 h-6 text-laser-blue" />,
        premium: false,
        settingKey: 'firebaseMessagingEnabled'
      },
      {
        id: 'emailFallbackEnabled',
        name: 'Email Backup',
        description: 'Forward incoming alerts to your email when data is unavailable.',
        icon: <EmailIcon className="w-6 h-6 text-laser-blue" />,
        premium: false,
        settingKey: 'emailFallbackEnabled'
      },
      {
        id: 'smartRepliesEnabled',
        name: 'Smart Replies',
        description: 'AI-generated quick responses for incoming messages.',
        icon: <MessageSquareIcon className="w-6 h-6 text-laser-blue" />,
        premium: false,
        settingKey: 'smartRepliesEnabled'
      },
      {
        id: 'truecallerEnabled',
        name: 'Truecaller Caller ID',
        description: 'Identify unknown callers using Truecaller integration.',
        icon: <ContactIcon className="w-6 h-6 text-laser-blue" />,
        premium: true,
        settingKey: 'truecallerEnabled'
      }
    ]
  },
  {
    category: 'Safety & Emergency',
    items: [
      {
        id: 'trustedContactsEnabled',
        name: 'Trusted Contacts',
        description: 'Manage emergency contacts and share your live location.',
        icon: <ShieldCheckIcon className="w-6 h-6 text-laser-blue" />,
        premium: false,
        settingKey: 'trustedContactsEnabled'
      },
      {
        id: 'crashDetectionEnabled',
        name: 'Crash Detection',
        description: 'Automatically alert contacts if a vehicle collision is detected.',
        icon: <CarCrashIcon className="w-6 h-6 text-laser-blue" />,
        premium: true,
        settingKey: 'crashDetectionEnabled'
      },
      {
        id: 'privateSafeEnabled',
        name: 'Private Safe',
        description: 'Secure storage for sensitive notes and emergency codes.',
        icon: <LockIcon className="w-6 h-6 text-laser-blue" />,
        premium: true,
        settingKey: 'privateSafeEnabled'
      },
      {
        id: 'otpCleanupEnabled',
        name: 'OTP Cleanup',
        description: 'Automatically delete one-time passwords after 24 hours.',
        icon: <SparklesIcon className="w-6 h-6 text-laser-blue" />,
        premium: false,
        settingKey: 'otpCleanupEnabled'
      }
    ]
  },
  {
    category: 'System Intelligence',
    items: [
      {
        id: 'aiSummariesEnabled',
        name: 'AI Summaries',
        description: 'Get daily summaries of your message activity and alerts.',
        icon: <SmartToyIcon className="w-6 h-6 text-laser-blue" />,
        premium: true,
        settingKey: 'aiSummariesEnabled'
      },
      {
        id: 'mergedExperienceEnabled',
        name: 'Unified Navigation',
        description: 'Merge Beacon and PulseLink into a single seamless interface.',
        icon: <HeartPulseIcon className="w-6 h-6 text-laser-blue" />,
        premium: true,
        settingKey: 'mergedExperienceEnabled'
      }
    ]
  }
];
