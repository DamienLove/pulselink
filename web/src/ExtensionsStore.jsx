import { useState, useMemo } from 'react';
import { doc, serverTimestamp, setDoc } from "firebase/firestore";
import logo from './assets/pulselink-pro-logo.png';
import beaconLogo from './assets/beacon-logo.png';
import ringersongLogo from './assets/ringersong-logo.png';

// Icons
const BoltIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"></path></svg>;
const StarIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>;
const CloudSyncIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22.61 16.95A5 5 0 0 0 18 10h-1.26a8 8 0 0 0-14.33 6"/><polyline points="1 20 5 20 5 16"/><path d="M1 20a9 9 0 0 0 9 9 9 9 0 0 0 4-10"/><polyline points="23 4 19 4 19 8"/><path d="M23 4a9 9 0 0 0-9-9 9 9 0 0 0-4 10"/></svg>;
const MapIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"></polygon><line x1="8" y1="2" x2="8" y2="18"></line><line x1="16" y1="6" x2="16" y2="22"></line></svg>;
const ContactIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>;
const ThemeIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="14.31" y1="8" x2="20.05" y2="17.94"></line><line x1="9.69" y1="8" x2="21.17" y2="8"></line><line x1="7.38" y1="12" x2="13.12" y2="2.06"></line><line x1="9.69" y1="16" x2="3.95" y2="6.06"></line><line x1="14.31" y1="16" x2="2.83" y2="16"></line><line x1="16.62" y1="12" x2="10.88" y2="21.94"></line></svg>;
const EmailIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>;
const CarCrashIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M14 16H9m10 0h3v-3.15M17 12.89l1.45-1.45M9 16.02L6.68 18.34M4.34 20.68L2 23M9 12V8h6v4"/><rect x="4" y="16" width="10" height="6" rx="2"/><path d="M14 10l-2-3-2 3"/></svg>;
const LockIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>;
const MessageSquareIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>;
const DeleteSweepIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>;
const SmartToyIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h4Z"/><rect x="4" y="10" width="16" height="8" rx="2"/><path d="M9 22v-4"/><path d="M15 22v-4"/><circle cx="8" cy="14" r="1" fill="currentColor"/><circle cx="16" cy="14" r="1" fill="currentColor"/></svg>;
const HomeIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>;
const ExtensionIcon = () => <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20.5 11a2.5 2.5 0 0 1 0 5 2.5 2.5 0 0 1-5 0V11h5Z"/><path d="M8 11V6a2.5 2.5 0 0 1 5 0 2.5 2.5 0 0 1 0 5H8Z"/><path d="M11 8h5a2.5 2.5 0 0 1 0 5 2.5 2.5 0 0 1-5 0v-5Z"/><path d="M12 21a9 9 0 0 0 9-9 9 9 0 0 0-9-9 9 9 0 0 0-9 9 9 9 0 0 0 9 9Z"/></svg>;
const SearchIcon = () => <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>;
const CloseIcon = () => <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>;
const RequiredIndicator = () => (
  <span
    aria-hidden="true"
    style={{ color: 'var(--danger)', marginLeft: '4px' }}
    title="Required field"
  >
    *
  </span>
);

const getToastClass = (msg) => {
  if (!msg) return 'toast';
  const lower = msg.toLowerCase();
  if (lower.includes('fail') || lower.includes('error') || lower.includes('missing')) return 'toast error';
  if (lower.includes('success') || lower.includes('saved') || lower.includes('updated') || lower.includes('sent') || lower.includes('published') || lower.includes('imported') || lower.includes('cleared')) return 'toast success';
  return 'toast';
};

function ExtensionsStore({
    remoteSettings,
    setRemoteSettings,
    isPremiumUser,
    user,
    db,
    devExtensions,
    setDevExtensions,
    extensionForm,
    setExtensionForm,
    extensionStatus,
    setExtensionStatus,
    handleAddExtension,
    handleTestExtension
}) {
    const [searchQuery, setSearchQuery] = useState('');

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
          newSettings.ringerSongEnabled = false;
          newSettings.mapEnabled = true; // Safety core
          newSettings.contactsEnabled = true;
          newSettings.themesEnabled = false;
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
          newSettings.truecallerEnabled = true;
          newSettings.ringerSongEnabled = true;
          newSettings.mapEnabled = true;
          newSettings.contactsEnabled = true;
          newSettings.themesEnabled = true;
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

    const categories = [
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

    const filteredCategories = useMemo(() => {
        if (!searchQuery.trim()) return categories;
        const lowerQuery = searchQuery.toLowerCase();

        return categories.map(cat => ({
            ...cat,
            items: cat.items.filter(item =>
                item.name.toLowerCase().includes(lowerQuery) ||
                item.desc.toLowerCase().includes(lowerQuery)
            )
        })).filter(cat => cat.items.length > 0);
    }, [categories, searchQuery]);

    return (
        <div className="pulselink-panel">
            <div className="panel-header">
            <h3>Features Store</h3>
            <p>Enhance your PulseLink experience with powerful add-ons.</p>
            </div>

            <div className="settings-search-container">
                <div style={{ opacity: 0.5, display: 'flex' }}><SearchIcon /></div>
                <input
                    className="settings-search-input"
                    placeholder="Search features..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
                {searchQuery && (
                    <button
                        className="ghost-btn icon-only"
                        onClick={() => setSearchQuery('')}
                        aria-label="Clear search"
                        title="Clear search"
                        style={{ width: '28px', height: '28px' }}
                    >
                        <CloseIcon />
                    </button>
                )}
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

            {filteredCategories.map((category) => (
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
                            aria-label={`${isEnabled ? "Remove" : "Install"} ${ext.name}`}
                            title={`${isEnabled ? "Remove" : "Install"} ${ext.name}`}
                            onClick={() => {
                            setRemoteSettings(prev => ({ ...prev, [ext.id]: !prev[ext.id] }));
                            const next = { ...remoteSettings, [ext.id]: !isEnabled };
                            setDoc(doc(db, "users", user.uid), {
                                ...next,
                                settingsUpdatedAt: serverTimestamp()
                            }, { merge: true });
                            }}
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
                Name<RequiredIndicator />
                <input className="login-input" value={extensionForm.name} onChange={(e) => setExtensionForm((prev) => ({ ...prev, name: e.target.value }))} required />
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
    );
}

export default ExtensionsStore;
