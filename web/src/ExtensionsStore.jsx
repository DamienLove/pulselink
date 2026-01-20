import { useState, useMemo } from 'react';
import { doc, setDoc, serverTimestamp } from "firebase/firestore";
import logo from './assets/pulselink-pro-logo.png';
import beaconLogo from './assets/beacon-logo.png';
import ringersongLogo from './assets/ringersong-logo.png';
import {
  BoltIcon, StarIcon, CloudSyncIcon, MapIcon, ContactIcon, ThemeIcon,
  EmailIcon, CarCrashIcon, LockIcon, MessageSquareIcon, DeleteSweepIcon, SmartToyIcon,
  ExtensionIcon, SearchIcon, CloseIcon, CheckIcon, HomeIcon
} from './Icons';

const getToastClass = (msg) => {
  if (!msg) return 'toast';
  const lower = msg.toLowerCase();
  if (lower.includes('fail') || lower.includes('error') || lower.includes('missing')) return 'toast error';
  if (lower.includes('success') || lower.includes('saved') || lower.includes('updated') || lower.includes('sent') || lower.includes('published') || lower.includes('imported') || lower.includes('cleared')) return 'toast success';
  return 'toast';
};

const ExtensionsStore = ({
  remoteSettings,
  setRemoteSettings,
  user,
  db,
  isPremiumUser,
  handleQuickSetup,
  devExtensions,
  setDevExtensions,
  handleTestExtension,
  extensionStatus,
  setExtensionStatus
}) => {
  const [activeTab, setActiveTab] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [extensionForm, setExtensionForm] = useState({ name: '', endpoint: '', description: '' });

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

  const categories = [
    {
      id: 'core',
      title: "Core",
      items: [
        { id: 'beaconLauncherEnabled', name: 'Beacon Inbox', desc: 'Separate launcher icon for quick access to your SMS inbox.', icon: beaconLogo, isImg: true },
        { id: 'firebaseMessagingEnabled', name: 'Firebase Relay', desc: 'Faster messaging between PulseLink users.', icon: <CloudSyncIcon /> }
      ]
    },
    {
      id: 'apps',
      title: "PulseLink Apps",
      items: [
        { id: 'ringerSongEnabled', name: 'RingerSong', desc: 'Progressive ringtone streaming & playlist manager.', icon: ringersongLogo, isImg: true },
        { id: 'mapEnabled', name: 'Emergency Map', desc: 'Track shared locations from PulseLink alerts.', icon: <MapIcon /> },
        { id: 'contactsEnabled', name: 'Contacts Manager', desc: 'Browse and manage synced device contacts.', icon: <ContactIcon /> },
        { id: 'themesEnabled', name: 'Theme Gallery', desc: 'Browse, import, and publish custom themes.', icon: <ThemeIcon /> }
      ]
    },
    {
      id: 'safety',
      title: "Safety & Security",
      items: [
        { id: 'emailFallbackEnabled', name: 'Email Backup', desc: 'Forward urgent alerts to email if SMS fails.', icon: <EmailIcon /> },
        { id: 'crashDetectionEnabled', name: 'Crash Detection', desc: 'Detects car crashes and notifies emergency contacts.', icon: <CarCrashIcon />, premium: true },
        { id: 'privateSafeEnabled', name: 'Private Safe', desc: 'Lock and hide sensitive conversations.', icon: <LockIcon /> }
      ]
    },
    {
      id: 'smart',
      title: "Smart Features",
      items: [
        { id: 'smartRepliesEnabled', name: 'Smart Replies', desc: 'One-tap suggestion chips for incoming messages.', icon: <MessageSquareIcon /> },
        { id: 'otpCleanupEnabled', name: 'Smart OTP Cleanup', desc: 'Automatically deletes one-time passwords after 24 hours.', icon: <DeleteSweepIcon /> },
        { id: 'aiSummariesEnabled', name: 'PulseLink AI', desc: 'Smart summaries and urgency detection for your chats.', icon: <SmartToyIcon />, premium: true }
      ]
    },
    {
      id: 'integrations',
      title: "Integrations",
      items: [
        { id: 'remoteWebAccessEnabled', name: 'Remote Web Access', desc: 'Sync messages and contacts to this web portal.', icon: logo, isImg: true, premium: true },
        { id: 'mergedExperienceEnabled', name: 'Unified Home', desc: 'Merge PulseLink and Beacon navigation into a single simplified experience.', icon: <HomeIcon />, premium: true }, // HomeIcon logic needs checking
        { id: 'thirdPartyExtensionsEnabled', name: '3rd Party Extensions', desc: 'Allow community-built plugins (Beta).', icon: <ExtensionIcon />, premium: true },
        { id: 'truecallerEnabled', name: 'Truecaller Caller ID', desc: 'Identify unknown callers and block spam using Truecaller directory.', icon: <SearchIcon /> }
      ]
    }
  ];

  // Flatten items for search/all view
  const allItems = useMemo(() => categories.flatMap(c => c.items.map(item => ({...item, categoryId: c.id}))), []);

  const filteredItems = useMemo(() => {
    let items = activeTab === 'all' ? allItems : categories.find(c => c.id === activeTab)?.items || [];

    if (activeTab === 'developer') {
        // Developer tab logic handled separately in render
        return [];
    }

    if (searchQuery.trim()) {
      const lower = searchQuery.toLowerCase();
      items = items.filter(item =>
        item.name.toLowerCase().includes(lower) ||
        item.desc.toLowerCase().includes(lower)
      );
    }
    return items;
  }, [activeTab, searchQuery, allItems]);

  return (
    <div className="pulselink-panel">
      <div className="panel-header">
        <h3>Extensions Store</h3>
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

      <div className="store-controls" style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap', alignItems: 'center' }}>
        <div className="settings-search-container" style={{ flex: 1, margin: 0 }}>
             <SearchIcon />
             <input
               className="settings-search-input"
               placeholder="Search extensions..."
               value={searchQuery}
               onChange={(e) => setSearchQuery(e.target.value)}
             />
             {searchQuery && (
                <button
                  className="ghost-btn icon-only"
                  onClick={() => setSearchQuery('')}
                  style={{ width: '28px', height: '28px' }}
                >
                  <CloseIcon />
                </button>
             )}
        </div>
      </div>

      <div className="store-tabs" style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 12, marginBottom: 12 }}>
        <button className={`chip ${activeTab === 'all' ? 'active' : ''}`} onClick={() => setActiveTab('all')}>All</button>
        {categories.map(c => (
             <button key={c.id} className={`chip ${activeTab === c.id ? 'active' : ''}`} onClick={() => setActiveTab(c.id)}>{c.title}</button>
        ))}
        <button className={`chip ${activeTab === 'developer' ? 'active' : ''}`} onClick={() => setActiveTab('developer')}>Developer</button>
      </div>

      {activeTab !== 'developer' && (
        <div className="home-grid">
            {filteredItems.map(ext => {
                const isEnabled = remoteSettings[ext.id];
                const isLocked = ext.premium && !isPremiumUser;

                return (
                <div className="home-card extension-card" key={ext.id} style={{ opacity: isLocked ? 0.6 : 1, position: 'relative' }}>
                    <div className="home-icon" style={{
                        background: ext.isImg ? 'transparent' : 'rgba(255, 255, 255, 0.05)',
                        display: 'grid',
                        placeItems: 'center'
                    }}>
                    {ext.isImg ? <img src={ext.icon} alt={ext.name} /> : ext.icon}
                    </div>
                    <div style={{flex: 1}}>
                        <h3 style={{marginTop: 12, marginBottom: 4}}>{ext.name}</h3>
                        <p style={{marginBottom: 16, minHeight: 40, fontSize: '0.9em'}}>{ext.desc}</p>
                    </div>

                    {isLocked ? (
                    <div className="badge badge-premium" style={{background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--muted)', width: '100%', textAlign: 'center'}}>
                        Premium Required
                    </div>
                    ) : (
                    <div style={{display: 'flex', gap: 8}}>
                        <button
                            className={isEnabled ? "secondary-btn" : "primary-btn"}
                            style={{flex: 1}}
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
                    </div>
                    )}
                </div>
                );
            })}
            {filteredItems.length === 0 && (
                <div className="settings-note" style={{gridColumn: '1 / -1', textAlign: 'center', padding: 40}}>
                    No extensions found matching "{searchQuery}".
                </div>
            )}
        </div>
      )}

      {activeTab === 'developer' && (
        <>
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
        </>
      )}
    </div>
  );
};

export default ExtensionsStore;
