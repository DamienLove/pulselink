import { useState, useEffect, useRef, useMemo, memo, useCallback } from 'react';
import './App.css';

const ACTIONS = [
  { id: 'nav-home', label: 'Go to Home', icon: '🏠', action: (setActivePanel) => setActivePanel('home') },
  { id: 'nav-inbox', label: 'Open SoText Inbox', icon: '📨', action: (setActivePanel) => setActivePanel('inbox') },
  { id: 'nav-pulselink', label: 'Manage Profile', icon: '👤', action: (setActivePanel) => setActivePanel('pulselink') },
  { id: 'nav-contacts', label: 'View Contacts', icon: '👥', action: (setActivePanel) => setActivePanel('contacts') },
  { id: 'nav-map', label: 'Emergency Map', icon: '🗺️', action: (setActivePanel) => setActivePanel('map') },
  { id: 'nav-themes', label: 'Theme Gallery', icon: '🎨', action: (setActivePanel) => setActivePanel('themes') },
  { id: 'nav-extensions', label: 'Extensions Store', icon: '🧩', action: (setActivePanel) => setActivePanel('extensions') },
  { id: 'nav-settings', label: 'Settings', icon: '⚙️', action: (setActivePanel) => setActivePanel('settings') },
  { id: 'act-logout', label: 'Log Out', icon: '🚪', action: (setActivePanel, actions) => actions.logout() },
  { id: 'act-new-msg', label: 'New Message', icon: '✏️', action: (setActivePanel, actions) => actions.newThread() },
];

const LocationIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M12 21.7c-3.1-2.8-8-7.7-8-12.7C4 5.2 7.6 2 12 2s8 3.2 8 6.9c0 5-4.9 9.9-8 12.7z"/>
    <circle cx="12" cy="8" r="3"/>
  </svg>
);

const CommandPalette = memo(({ isOpen, onClose, setActivePanel, actions }) => {
  const { isPremium, deviceContacts, trustedContacts, sendMessage, setStatus, activeLineId, lines, lineInboxMode } = actions;
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef(null);
  const listRef = useRef(null);
  const restoreFocusRef = useRef(null);
  const [pendingCommand, setPendingCommand] = useState(null); // { type: 'sendMessage', details: { contact, message, withLocation } }

  const resolveContact = useCallback((term) => {
    const lowerTerm = term.toLowerCase();
    const allContacts = [...deviceContacts, ...trustedContacts];
    // Prioritize exact phone number match
    let found = allContacts.find(c => c.phoneNumber === term);
    if (found) return found;

    // Then try exact display name match
    found = allContacts.find(c => c.displayName?.toLowerCase() === lowerTerm);
    if (found) return found;

    // Then try partial display name or phone number includes
    found = allContacts.find(c =>
      c.displayName?.toLowerCase().includes(lowerTerm) ||
      c.phoneNumber?.includes(term)
    );
    return found;
  }, [deviceContacts, trustedContacts]);

  const handleExecuteCommand = useCallback(async (commandType, details) => {
    switch (commandType) {
      case 'sendMessage':
        const { recipient, message, withLocation } = details;
        let targetContact = recipient;

        // 1. Resolve Contact
        if (typeof recipient === 'string') {
          targetContact = resolveContact(recipient);
          if (!targetContact) {
            setStatus(`Contact "${recipient}" not found. Please try a phone number or a different name.`);
            setPendingCommand(null); // Clear pending if contact resolution fails
            return false;
          }
        } else if (!recipient) {
           setStatus("Recipient not specified. Please provide a contact name or phone number.");
           setPendingCommand(null);
           return false;
        }


        let locationData = null;
        if (withLocation) {
          if (!isPremium) {
            setStatus("Location sharing is a Premium feature. Please upgrade.");
            setPendingCommand({ type: 'sendMessage', details });
            onClose(); // Close palette to allow user to react to toast
            return false;
          }
          if (!navigator.geolocation) {
            setStatus("Geolocation is not supported by your browser.");
            setPendingCommand(null); // Cannot fulfill command if geolocation not supported
            return false;
          }

          setStatus("Getting your current location...");
          try {
            const position = await new Promise((resolve, reject) => {
              navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 });
            });
            locationData = { latitude: position.coords.latitude, longitude: position.coords.longitude };
            setStatus(`Location acquired.`);
          } catch (error) {
            console.error("Geolocation error:", error);
            setStatus(`Failed to get location: ${error.message}. Please enable location services.`);
            setPendingCommand(null); // Geolocation failed
            return false;
          }
        }

        // 2. Send Message
        const success = await sendMessage({
          address: targetContact.phoneNumber,
          body: message,
          location: locationData,
          lineId: activeLineId || lines[0]?.id || null // Use active line or first line if available
        });

        if (success) {
          setStatus("Message sent!");
          setPendingCommand(null); // Clear any pending command
          setActivePanel('inbox'); // Navigate to inbox to see message
        } else {
          setStatus("Failed to send message.");
          setPendingCommand(null); // Clear pending as send failed
        }
        return success;

      default:
        setStatus(`Unknown command: ${commandType}`);
        setPendingCommand(null);
        return false;
    }
  }, [isPremium, deviceContacts, trustedContacts, sendMessage, setStatus, setActivePanel, resolveContact, onClose, activeLineId, lines]);


  useEffect(() => {
    let timeoutId;
    if (isOpen) {
      restoreFocusRef.current = document.activeElement;
      setQuery('');
      setSelectedIndex(0);
      // setPendingCommand(null); // Don't clear pending command here, allow it to be retried
      // Small timeout to allow render before focus
      timeoutId = setTimeout(() => inputRef.current?.focus(), 50);
    } else {
      restoreFocusRef.current?.focus({ preventScroll: true });
    }
    return () => clearTimeout(timeoutId);
  }, [isOpen]);

  const filteredItems = useMemo(() => {
    let currentActions = [...ACTIONS];

    if (pendingCommand) {
      currentActions.unshift({
        id: 'retry-pending',
        label: `Retry pending command: "${pendingCommand.details.message}" to ${pendingCommand.details.recipient}${pendingCommand.details.withLocation ? ' (with location)' : ''}`,
        icon: '🔄',
        action: async () => {
          const success = await handleExecuteCommand(pendingCommand.type, pendingCommand.details);
          if (success) onClose();
        },
        alwaysShow: true // Ensure this action is always visible
      });
    }

    if (query.trim()) {
      const lowerQuery = query.toLowerCase();

      // NLU for "text [contact] [message] [and give my location]"
      const textCommandMatch = lowerQuery.match(/(?:text|message)\s+([^,]+?)(?:\s+(?:with|and)\s+(?:my|current)\s+location)?(?:,?\s*(.*))?$/i);
      if (textCommandMatch) {
        let recipientTerm = textCommandMatch[1]?.trim();
        let messageBody = textCommandMatch[2]?.trim();
        const hasLocation = lowerQuery.includes('location') && (lowerQuery.includes('with') || lowerQuery.includes('and'));

        if (!messageBody && recipientTerm && !hasLocation && (lowerQuery.includes('text ') || lowerQuery.includes('message '))) {
          // If message is empty but command has "text/message" and a recipient, assume recipient is a contact and the rest is message
          const parts = recipientTerm.split(/\s+/);
          const potentialContactName = parts[0];
          const contact = resolveContact(potentialContactName);
          if (contact) {
            recipientTerm = potentialContactName;
            messageBody = parts.slice(1).join(' ').trim();
          } else {
            // No contact found for first word, assume entire match[1] is message, no recipient
            messageBody = recipientTerm;
            recipientTerm = null;
          }
        }
        
        // Final fallback for messageBody if it's still empty and a "text/message" command was detected
        if (!messageBody && (lowerQuery.startsWith('text ') || lowerQuery.startsWith('message '))) {
          messageBody = "Hey!"; // Default message if none provided
        }

        if (recipientTerm || messageBody) {
          const parsedCommand = {
            command: 'sendMessage',
            recipient: recipientTerm,
            message: messageBody,
            withLocation: hasLocation
          };
          
          currentActions.unshift({
            id: 'nlu-send-text',
            label: `Send message: "${messageBody || '...'}" to ${recipientTerm || '...'}${hasLocation ? ' (with location)' : ''}`,
            icon: hasLocation ? <LocationIcon /> : '📝',
            action: async (currentSetActivePanel, currentActions) => { // Use arguments from handleSelect
              const success = await handleExecuteCommand(parsedCommand.command, parsedCommand);
              // if (success) currentActions.onClose(); // onClose is passed as a prop, not in actions
              if (success) onClose();
            },
            keepOpen: true // Keep open if more info needed or geo permission etc.
          });
        }
      }

      // Filter existing static actions
      currentActions = currentActions.filter(item =>
        item.alwaysShow || item.label.toLowerCase().includes(lowerQuery)
      );
    }

    return currentActions;
  }, [query, pendingCommand, handleExecuteCommand, resolveContact, isPremium, setStatus, onClose, setActivePanel, deviceContacts, trustedContacts]);

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
        filteredItems[selectedIndex].action(setActivePanel, actions);
        if (!filteredItems[selectedIndex].keepOpen) onClose(); // New property to keep palette open
      }
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onClose();
    }
  };

  const handleSelect = (item) => {
    item.action(setActivePanel, actions);
    if (!item.keepOpen) onClose();
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
            placeholder="Type a command or search..."
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
          <div className="command-palette-hint">Esc to close</div>
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
              {typeof item.icon === 'string' ? <span className="item-icon">{item.icon}</span> : <div className="item-icon">{item.icon}</div>}
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
});

CommandPalette.displayName = 'CommandPalette';

export default CommandPalette;

