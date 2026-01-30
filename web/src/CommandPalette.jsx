import { useState, useEffect, useRef, useMemo } from 'react';
import './App.css';

const ACTIONS = [
  { id: 'nav-home', label: 'Go to Home', icon: '🏠', action: (setActivePanel) => setActivePanel('home') },
  { id: 'nav-beacon', label: 'Open Beacon Inbox', icon: '📨', action: (setActivePanel) => setActivePanel('beacon') },
  { id: 'nav-pulselink', label: 'Manage Profile', icon: '👤', action: (setActivePanel) => setActivePanel('pulselink') },
  { id: 'nav-contacts', label: 'View Contacts', icon: '👥', action: (setActivePanel) => setActivePanel('contacts') },
  { id: 'nav-map', label: 'Emergency Map', icon: '🗺️', action: (setActivePanel) => setActivePanel('map') },
  { id: 'nav-themes', label: 'Theme Gallery', icon: '🎨', action: (setActivePanel) => setActivePanel('themes') },
  { id: 'nav-extensions', label: 'Extensions Store', icon: '🧩', action: (setActivePanel) => setActivePanel('extensions') },
  { id: 'nav-settings', label: 'Settings', icon: '⚙️', action: (setActivePanel) => setActivePanel('settings') },
  { id: 'act-logout', label: 'Log Out', icon: '🚪', action: (setActivePanel, actions) => actions.logout() },
  { id: 'act-new-msg', label: 'New Message', icon: '✏️', action: (setActivePanel, actions) => actions.newThread() },
];

export default function CommandPalette({ isOpen, onClose, setActivePanel, actions, deviceContacts = [], trustedContacts = [] }) {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef(null);
  const listRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setSelectedIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  const filteredItems = useMemo(() => {
    if (!query) return ACTIONS;
    const lower = query.toLowerCase();

    // Filter static actions
    const matches = ACTIONS.filter(item =>
      item.label.toLowerCase().includes(lower)
    );

    // Filter contacts if query is long enough
    if (lower.length > 1) {
      const contactMatches = [];
      const seen = new Set();

      const addContact = (c, type) => {
        const name = c.displayName || c.name || 'Unknown';
        const phone = c.phoneNumber || c.phone || '';
        const key = `${name}-${phone}`;
        if (seen.has(key)) return;
        seen.add(key);

        if (name.toLowerCase().includes(lower) || phone.includes(lower)) {
          contactMatches.push({
            id: `contact-${c.id || phone}`,
            label: `Message ${name}`,
            icon: '💬',
            desc: phone,
            action: (setActivePanel, acts) => {
              // Switch to beacon
              setActivePanel('beacon');
              // Trigger new thread flow (in a real app, we'd pre-fill)
              if (acts.newThread) acts.newThread();
              // For now, we rely on the user to type the name again or finding it
              // A better way would be passing a "selectedContact" to App.jsx state
            }
          });
          contactMatches.push({
            id: `call-${c.id || phone}`,
            label: `View ${name}`,
            icon: '👤',
            desc: 'Open in Contacts',
            action: (setActivePanel) => {
              setActivePanel('contacts');
              // We can hack search param into DOM or state if accessible
              // For now just open contacts
            }
          });
        }
      };

      trustedContacts.forEach(c => addContact(c, 'trusted'));
      deviceContacts.forEach(c => addContact(c, 'device'));

      return [...matches, ...contactMatches].slice(0, 15); // Limit results
    }

    return matches;
  }, [query, deviceContacts, trustedContacts]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [filteredItems]);

  const handleKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex(prev => Math.min(prev + 1, filteredItems.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex(prev => Math.max(prev - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (filteredItems[selectedIndex]) {
        handleSelect(filteredItems[selectedIndex]);
      }
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onClose();
    }
  };

  const handleSelect = (item) => {
    item.action(setActivePanel, actions);
    onClose();
  };

  useEffect(() => {
    if (listRef.current && listRef.current.children[selectedIndex]) {
      listRef.current.children[selectedIndex].scrollIntoView({
        block: 'nearest',
      });
    }
  }, [selectedIndex]);

  if (!isOpen) return null;

  return (
    <div
      className="command-palette-overlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label="Command Palette"
    >
      <div className="command-palette-modal" onClick={e => e.stopPropagation()}>
        <div className="command-palette-header">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="search-icon">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            ref={inputRef}
            className="command-palette-input"
            placeholder="Type a command or search contacts..."
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            role="combobox"
            aria-autocomplete="list"
            aria-expanded="true"
            aria-controls="command-list"
            aria-activedescendant={filteredItems[selectedIndex]?.id}
            aria-label="Command input"
          />
          <div className="command-palette-hint">Esc</div>
        </div>
        <div
          className="command-palette-list"
          ref={listRef}
          role="listbox"
          id="command-list"
        >
          {filteredItems.map((item, index) => (
            <div
              key={item.id}
              id={item.id}
              className={`command-palette-item ${index === selectedIndex ? 'selected' : ''}`}
              onClick={() => handleSelect(item)}
              onMouseEnter={() => setSelectedIndex(index)}
              role="option"
              aria-selected={index === selectedIndex}
            >
              <span className="item-icon">{item.icon}</span>
              <div style={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
                <span className="item-label">{item.label}</span>
                {item.desc && <span className="item-desc" style={{ fontSize: '0.75em', opacity: 0.6 }}>{item.desc}</span>}
              </div>
              {index === selectedIndex && <span className="item-enter">↵</span>}
            </div>
          ))}
          {filteredItems.length === 0 && (
            <div className="command-palette-empty">No results found.</div>
          )}
        </div>
        <div className="command-palette-footer">
            <span><strong>↑↓</strong> navigate</span>
            <span><strong>↵</strong> select</span>
            <span><strong>esc</strong> close</span>
        </div>
      </div>
    </div>
  );
}
