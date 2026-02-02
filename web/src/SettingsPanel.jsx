import React, { useState } from 'react';

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
      {copied ? (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
      ) : (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
      )}
    </button>
  );
};

const SettingsPanel = ({
  user,
  settingsSearch,
  setSettingsSearch,
  showPreviews,
  setShowPreviews,
  autoScroll,
  setAutoScroll,
  showScanlines,
  setShowScanlines,
  showNoise,
  setShowNoise,
  glassIntensity,
  setGlassIntensity,
  remoteSettings,
  setRemoteSettings,
  syncDiagnostics,
  relayDiagnostics,
  requestPhoneSync,
  handleTestRelay,
  remoteSettingsStatus,
  isSavingSettings,
  handleRemoteSettingsSave,
  syncRequestStatus,
  deleteAction,
  setDeleteAction,
  deleteStatus,
  handleDeleteAccountData,
  handleDeleteAccount,
  handlePasswordResetForUser,
  settingsStatus,
  getToastClass,
  dateTimeFormatter,
  toMillis
}) => {
  const [activeTab, setActiveTab] = useState('general');

  const term = settingsSearch.toLowerCase().trim();
  const show = (keywords) => {
    if (!term) return true;
    return keywords.some(k => k.includes(term));
  };

  return (
    <div className="settings-panel">
      <div className="settings-header">
        <h3>Settings</h3>
        <p>Manage account details and shared preferences.</p>
      </div>

      <div className="settings-tabs" style={{ display: 'flex', gap: '8px', marginBottom: '24px', borderBottom: '1px solid var(--border)' }}>
        <button
          className={`ghost-btn ${activeTab === 'general' ? 'active' : ''}`}
          onClick={() => setActiveTab('general')}
          style={{ borderBottom: activeTab === 'general' ? '2px solid var(--accent)' : '2px solid transparent', borderRadius: '4px 4px 0 0' }}
        >
          General
        </button>
        <button
          className={`ghost-btn ${activeTab === 'pulselink' ? 'active' : ''}`}
          onClick={() => setActiveTab('pulselink')}
          style={{ borderBottom: activeTab === 'pulselink' ? '2px solid var(--accent)' : '2px solid transparent', borderRadius: '4px 4px 0 0' }}
        >
          PulseLink
        </button>
        <button
          className={`ghost-btn ${activeTab === 'data' ? 'active' : ''}`}
          onClick={() => setActiveTab('data')}
          style={{ borderBottom: activeTab === 'data' ? '2px solid var(--accent)' : '2px solid transparent', borderRadius: '4px 4px 0 0' }}
        >
          Data & Privacy
        </button>
      </div>

      <div className="settings-search-container">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{opacity: 0.5}}>
          <circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
        <input
          className="settings-search-input"
          placeholder="Search settings"
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
        {/* General Tab */}
        {(activeTab === 'general' || term) && (
          <>
            {show(['account', 'email', 'user id', 'password', 'reset', 'sign out', 'logout', 'profile']) && (
              <div className="settings-card">
                <h4>Account</h4>
                <div className="settings-row">
                  <span className="settings-label">Signed in as</span>
                  <span className="settings-value">{user?.email || 'Unknown'}</span>
                </div>
                <div className="settings-row">
                  <span className="settings-label">User ID</span>
                  <span className="settings-value mono">
                    {user?.uid}
                    <CopyButton text={user?.uid} label="Copy User ID" />
                  </span>
                </div>
                <button className="secondary-btn" type="button" onClick={handlePasswordResetForUser}>
                  Send password reset email
                </button>
                {settingsStatus && <div className={getToastClass(settingsStatus)} role="status" aria-live="polite">{settingsStatus}</div>}
              </div>
            )}

            {show(['web', 'previews', 'scroll', 'auto-scroll', 'message previews', 'browser', 'scanlines', 'noise', 'grain']) && (
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
                <div className="divider" style={{height: 1, background: 'var(--border)', margin: '12px 0'}} />
                <h5 style={{margin: '0 0 8px 0', fontSize: '0.95em'}}>Visual Effects</h5>
                <label className="settings-toggle">
                  <input
                    type="checkbox"
                    checked={showScanlines}
                    onChange={(e) => setShowScanlines(e.target.checked)}
                  />
                  Show CRT Scanlines
                </label>
                <label className="settings-toggle">
                  <input
                    type="checkbox"
                    checked={showNoise}
                    onChange={(e) => setShowNoise(e.target.checked)}
                  />
                  Show Film Grain
                </label>
                <p className="settings-note">
                  Preferences apply to this browser only.
                </p>
              </div>
            )}
          </>
        )}

        {/* PulseLink Tab */}
        {(activeTab === 'pulselink' || term) && (
          <>
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
                      ? `${dateTimeFormatter.format(new Date(toMillis(syncDiagnostics.timestamp)))} • ${syncDiagnostics.status}`
                      : 'No sync data yet'}
                  </span>
                </div>
                {syncDiagnostics && (
                  <p className="settings-note">
                    Threads: {syncDiagnostics.threadCount ?? 0} · Messages: {syncDiagnostics.messageCount ?? 0} · READ_SMS: {syncDiagnostics.hasReadSms ? 'yes' : 'no'} · App: {syncDiagnostics.appVersion ?? 'unknown'}
                  </p>
                )}
                <div className="settings-row">
                  <span className="settings-label">Relay status</span>
                  <span className="settings-value">
                    {relayDiagnostics
                      ? `${dateTimeFormatter.format(new Date(toMillis(relayDiagnostics.timestamp)))} • ${relayDiagnostics.status}`
                      : 'No relay data yet'}
                  </span>
                </div>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    className="secondary-btn"
                    type="button"
                    onClick={requestPhoneSync}
                    style={{ flex: 1 }}
                  >
                    Request phone sync
                  </button>
                  <button
                    className="secondary-btn"
                    type="button"
                    onClick={handleTestRelay}
                    style={{ flex: 1 }}
                  >
                    Test relay
                  </button>
                </div>
                {remoteSettingsStatus && <div className={getToastClass(remoteSettingsStatus)} role="status" aria-live="polite">{remoteSettingsStatus}</div>}
                {syncRequestStatus && <div className={getToastClass(syncRequestStatus)} role="status" aria-live="polite">{syncRequestStatus}</div>}
              </div>
            )}
          </>
        )}

        {/* Data Tab */}
        {(activeTab === 'data' || term) && (
          <>
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
        )}
      </div>
    </div>
  );
};

export default SettingsPanel;
