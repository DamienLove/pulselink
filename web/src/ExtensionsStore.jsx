import React from 'react';
import { doc, setDoc, serverTimestamp } from "firebase/firestore";
import { db } from './firebase';
import { BoltIcon, StarIcon } from './Icons';
import { features } from './data/features';

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

const ExtensionsStore = ({
  remoteSettings,
  setRemoteSettings,
  user,
  isPremiumUser,
  handleQuickSetup,
  handleAddExtension,
  extensionForm,
  setExtensionForm,
  extensionStatus,
  devExtensions,
  setDevExtensions,
  handleTestExtension
}) => {
  return (
    <div className="pulselink-panel">
      <div className="panel-header">
        <h3>Features</h3>
        <p>Enhance your PulseLink experience with powerful add-ons.</p>
      </div>

      <div className="settings-card" style={{ marginBottom: 20 }}>
        <h4>Quick Setup</h4>
        <div className="settings-row" style={{ alignItems: 'stretch', gap: 16 }}>
          <button className="home-card" style={{ margin: 0, flex: 1, textAlign: 'left', alignItems: 'flex-start' }} onClick={() => handleQuickSetup('essentials')}>
            <div className="home-icon" style={{ width: 40, height: 40, background: 'rgba(34, 211, 238, 0.1)', color: 'var(--accent)' }}>
              <BoltIcon />
            </div>
            <h4 style={{ marginTop: 8 }}>Essentials</h4>
            <p style={{ fontSize: '0.9em', color: 'var(--muted)', margin: 0 }}>Just the basics: Beacon, Relay, Email Backup, and OTP Cleanup.</p>
          </button>
          <button className="home-card" style={{ margin: 0, flex: 1, textAlign: 'left', alignItems: 'flex-start' }} onClick={() => handleQuickSetup('power')}>
            <div className="home-icon" style={{ width: 40, height: 40, background: 'rgba(34, 211, 238, 0.1)', color: 'var(--accent)' }}>
              <StarIcon />
            </div>
            <h4 style={{ marginTop: 8 }}>Power User</h4>
            <p style={{ fontSize: '0.9em', color: 'var(--muted)', margin: 0 }}>Everything enabled: AI, Crash Detection, Web Access, and more.</p>
          </button>
        </div>
      </div>

      {features.map((category) => (
        <div key={category.title} className="extension-category" style={{ marginBottom: 32 }}>
          <h4 style={{ marginBottom: 16, color: 'var(--ink)' }}>{category.title}</h4>
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
                  <h3 style={{ marginTop: 12, marginBottom: 4 }}>{ext.name}</h3>
                  <p style={{ marginBottom: 16, minHeight: 40 }}>{ext.desc}</p>

                  {isLocked ? (
                    <div className="badge badge-premium" style={{ background: 'var(--surface)', border: '1px solid var(--border)', color: 'var(--muted)' }}>
                      Premium Required
                    </div>
                  ) : (
                    <button
                      className={isEnabled ? "secondary-btn" : "primary-btn"}
                      style={{ width: '100%' }}
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
        <div className="extensions-list" style={{ display: 'grid', gap: 12 }}>
          {devExtensions.map((ext) => (
            <div key={ext.id} className="home-card" style={{ margin: 0 }}>
              <h4>{ext.name}</h4>
              <p className="settings-note">{ext.description || ext.endpoint || 'No description provided.'}</p>
              {ext.endpoint && <code className="mono" style={{ fontSize: 12 }}>{ext.endpoint}</code>}
              <div className="settings-row" style={{ justifyContent: 'flex-start', gap: 8, marginTop: 8 }}>
                <button className="secondary-btn" type="button" onClick={() => handleTestExtension(ext)}>Test</button>
                <button className="ghost-btn" type="button" onClick={() => setDevExtensions((prev) => prev.filter((d) => d.id !== ext.id))}>Remove</button>
              </div>
            </div>
          ))}
          {devExtensions.length === 0 && <p className="settings-note">No custom extensions yet.</p>}
        </div>
        <form className="login-form" style={{ marginTop: 12 }} onSubmit={handleAddExtension}>
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
        <div className="settings-row" style={{ gap: 8, flexWrap: 'wrap' }}>
          <a className="secondary-btn" href="https://github.com/DamienLove/pulselink/blob/Suite-Beta/docs/extensions-dev.md" target="_blank" rel="noreferrer">Read dev guide</a>
          <a className="ghost-btn" href="mailto:extensions@pulselink.app?subject=PulseLink%20Extension%20Submission">Email us your zip</a>
        </div>
      </div>
    </div>
  );
};

export default ExtensionsStore;
