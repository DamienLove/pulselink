import { useState, useEffect, useRef } from 'react';
import './App.css';

const TIPS = [
  "Press Ctrl+K to open the Command Palette anywhere.",
  "You can pin important conversations to keep them at the top.",
  "Enable 'Remote Web Access' on your phone to sync messages.",
  "Use the 'Emergency Map' to see where alerts are coming from.",
  "Customize your experience in the Theme Gallery.",
  "RingerSong lets you manage your ringtone playlist from the web.",
  "Trusted Contacts can override your phone's Do Not Disturb.",
  "The 'Future Deep' theme saves battery on OLED screens.",
  "You can drag and drop themes to import them (coming soon).",
  "Use the 'Features' tab to enable experimental extensions."
];

export default function PulseGuide({ activePanel, setActivePanel }) {
  const [isOpen, setIsOpen] = useState(false);
  const [currentTip, setCurrentTip] = useState(TIPS[0]);
  const [isHovered, setIsHovered] = useState(false);

  useEffect(() => {
    // Rotate tips every time the guide is opened
    if (isOpen) {
      setCurrentTip(TIPS[Math.floor(Math.random() * TIPS.length)]);
    }
  }, [isOpen]);

  return (
    <>
      <button
        className={`pulse-guide-fab ${isOpen ? 'open' : ''} ${isHovered ? 'hovered' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
        aria-label="Pulse Guide Assistant"
        title="Pulse Guide"
      >
        <div className="pulse-guide-icon">
          {isOpen ? (
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          ) : (
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h4Z"/>
              <rect x="4" y="10" width="16" height="8" rx="2"/>
              <path d="M9 22v-4"/>
              <path d="M15 22v-4"/>
              <circle cx="8" cy="14" r="1" fill="currentColor"/>
              <circle cx="16" cy="14" r="1" fill="currentColor"/>
            </svg>
          )}
        </div>
        <div className="pulse-guide-ring"></div>
        <div className="pulse-guide-ring-inner"></div>
      </button>

      {isOpen && (
        <div className="pulse-guide-card">
          <div className="guide-header">
            <h3>System Status</h3>
            <div className="status-indicator online">
              <span className="status-dot"></span>
              Online
            </div>
          </div>

          <div className="guide-content">
            <div className="guide-section">
              <h4>Quick Actions</h4>
              <div className="guide-actions">
                <button
                  className="guide-action-btn"
                  onClick={() => { setActivePanel('settings'); setIsOpen(false); }}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                  Settings
                </button>
                <button
                  className="guide-action-btn"
                  onClick={() => { setActivePanel('themes'); setIsOpen(false); }}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="14.31" y1="8" x2="20.05" y2="17.94"></line><line x1="9.69" y1="8" x2="21.17" y2="8"></line><line x1="7.38" y1="12" x2="13.12" y2="2.06"></line><line x1="9.69" y1="16" x2="3.95" y2="6.06"></line><line x1="14.31" y1="16" x2="2.83" y2="16"></line><line x1="16.62" y1="12" x2="10.88" y2="21.94"></line></svg>
                  Themes
                </button>
              </div>
            </div>

            <div className="guide-section">
              <h4>Tip of the moment</h4>
              <p className="guide-tip">{currentTip}</p>
            </div>

            <div className="guide-footer">
              <span className="guide-version">PulseLink Web v12.0</span>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
