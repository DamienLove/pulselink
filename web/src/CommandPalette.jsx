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

export default function CommandPalette({ isOpen, onClose, setActivePanel, actions }) {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef(null);
  const listRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setSelectedIndex(0);
      // Small timeout to allow render before focus
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  const filteredItems = useMemo(() => {
    if (!query) return ACTIONS;
    const lower = query.toLowerCase();
    return ACTIONS.filter(item =>
      item.label.toLowerCase().includes(lower)
    );
  }, [query]);

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

  // Auto-scroll to selected item
  useEffect(() => {
    if (listRef.current && listRef.current.children[selectedIndex]) {
      listRef.current.children[selectedIndex].scrollIntoView({
        block: 'nearest',
      });
    }
  }, [selectedIndex]);

  if (!isOpen) return null;

  return (
    <div className="command-palette-overlay" onClick={onClose}>
      <div className="command-palette-modal" onClick={e => e.stopPropagation()}>
        <div className="command-palette-header">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="search-icon">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            ref={inputRef}
            className="command-palette-input"
            placeholder="Type a command or search..."
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <div className="command-palette-hint">Esc to close</div>
        </div>
        <div className="command-palette-list" ref={listRef}>
          {filteredItems.map((item, index) => (
            <div
              key={item.id}
              className={`command-palette-item ${index === selectedIndex ? 'selected' : ''}`}
              onClick={() => handleSelect(item)}
              onMouseEnter={() => setSelectedIndex(index)}
            >
              <span className="item-icon">{item.icon}</span>
              <span className="item-label">{item.label}</span>
              {index === selectedIndex && <span className="item-enter">↵</span>}
            </div>
          ))}
          {filteredItems.length === 0 && (
            <div className="command-palette-empty">No results found.</div>
          )}
        </div>
      </div>
    </div>
  );
}
