import { useState, useEffect, useLayoutEffect, useMemo, useRef, memo, useCallback } from 'react';
import { collection, query, orderBy, onSnapshot, addDoc, serverTimestamp, limit } from "firebase/firestore";
import beaconLogo from './assets/beacon-logo.png';

// Shared Intl formatters
const timeFormatter = new Intl.DateTimeFormat(undefined, {
  hour: '2-digit',
  minute: '2-digit'
});

const RequiredIndicator = () => (
  <span
    aria-hidden="true"
    style={{ color: 'var(--danger)', marginLeft: '4px' }}
    title="Required field"
  >
    *
  </span>
);

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

const areMessagesEqual = (prev, next) => {
  return prev.showPreviews === next.showPreviews &&
         prev.msg.id === next.msg.id &&
         prev.msg.body === next.msg.body &&
         prev.msg.date === next.msg.date &&
         prev.msg.type === next.msg.type &&
         prev.msg.status === next.msg.status;
};

const MessageItem = memo(({ msg, showPreviews }) => (
  <div className={`message ${msg.type === 1 ? 'received' : 'sent'} ${msg.status === 'sending' ? 'sending' : ''}`}>
    <div className="message-bubble">
      {msg.imageUrl && (
        <div className="message-image-container">
          <img src={msg.imageUrl} alt="Attachment" className="message-image" loading="lazy" />
        </div>
      )}
      {showPreviews ? msg.body : '••••••'}
    </div>
    <div className="message-time">
      {msg.status === 'sending' ? 'Sending...' : timeFormatter.format(new Date(msg.date))}
    </div>
  </div>
), areMessagesEqual);

MessageItem.displayName = 'MessageItem';

const MessageComposer = memo(({ user, db, selectedThread, lineInboxMode, activeLineId, lines }) => {
  const [address, setAddress] = useState('');
  const [body, setBody] = useState('');
  const [lineId, setLineId] = useState('');
  const [status, setStatus] = useState('');
  const [isSending, setIsSending] = useState(false);
  const textareaRef = useRef(null);

  useLayoutEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${el.scrollHeight + 2}px`;
  }, [body]);

  useEffect(() => {
    if (selectedThread) {
      setAddress(selectedThread.address || '');
      setLineId(selectedThread.lineId || '');
    } else {
      setAddress('');
      setLineId('');
    }
    setBody('');
    setStatus('');
  }, [selectedThread]);

  const handleSendMessage = async () => {
    if (!user) return;
    const cleanAddress = address.trim();
    const cleanBody = body.trim();
    const effectiveLineId = lineInboxMode === 'PER_LINE' ? (lineId || activeLineId || lines[0]?.id || null) : null;

    if (!cleanAddress || !cleanBody) {
      setStatus("Add a phone number and message.");
      return;
    }

    setBody('');
    setStatus("");

    setIsSending(true);
    try {
      const docRef = await addDoc(collection(db, "users", user.uid, "outbox"), {
        address: cleanAddress,
        body: cleanBody,
        createdAt: serverTimestamp(),
        source: "web",
        lineId: effectiveLineId,
        status: "pending"
      });

      let unsubscribe;
      unsubscribe = onSnapshot(docRef, (docSnap) => {
        if (!docSnap.exists()) {
          setStatus("Sent");
          setTimeout(() => setStatus(''), 3000);
          if (unsubscribe) unsubscribe();
        } else {
          const data = docSnap.data();
          if (data.status === 'failed') {
            setStatus(`Send failed: ${data.error || 'Unknown error'}`);
            if (unsubscribe) unsubscribe();
          }
        }
      });
    } catch (error) {
      console.error("Send failed", error);
      setStatus("Send failed. Try again.");
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="composer">
      <div className="composer-row">
        <label className="composer-label" htmlFor="compose-address">To<RequiredIndicator /></label>
        <input
          id="compose-address"
          className="composer-input"
          type="tel"
          placeholder="Phone number"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
          required
        />
      </div>
      {lineInboxMode === 'PER_LINE' && lines.length > 0 && (
        <div className="composer-row">
          <label className="composer-label" htmlFor="compose-line">Send from</label>
          <select
            id="compose-line"
            className="composer-input"
            value={lineId}
            onChange={(e) => setLineId(e.target.value)}
          >
            <option value="">Primary device</option>
            {lines.map(line => (
              <option key={line.id} value={line.id}>
                {(line.label || line.phoneNumber || line.id.slice(0, 6))}
                {line.primaryDeviceId ? ' • primary' : ''}
              </option>
            ))}
          </select>
        </div>
      )}
      <div className="composer-row composer-actions">
        <div style={{ flex: 1, position: 'relative' }}>
          <textarea
            ref={textareaRef}
            className="composer-textarea"
            style={{ width: '100%', paddingBottom: '24px', maxHeight: '200px', overflowY: 'auto' }}
            placeholder="Type a message... (Ctrl+Enter to send)"
            aria-label="Message body"
            aria-describedby="message-char-count"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            required
            onKeyDown={(e) => {
              if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                handleSendMessage();
              }
            }}
          />
          {body.length > 0 && (
            <div
              id="message-char-count"
              style={{
                position: 'absolute',
                bottom: '8px',
                right: '12px',
                fontSize: '0.75em',
                color: 'var(--muted)',
                pointerEvents: 'none',
                fontWeight: 500
              }}
            >
              {body.length}
            </div>
          )}
        </div>
        <button
          onClick={handleSendMessage}
          disabled={isSending}
          className="primary-btn"
          title="Send (Ctrl+Enter)"
          aria-busy={isSending}
        >
          {isSending ? (
            <>
              <Spinner />
              Sending...
            </>
          ) : "Send"}
        </button>
      </div>
      {status && <div className="compose-status" role="status" aria-live="polite">{status}</div>}
      <div className="compose-hint">
        Messages are sent from your phone when it&apos;s online and signed in.
      </div>
    </div>
  );
});

MessageComposer.displayName = 'MessageComposer';

const LineTabs = memo(({ lines, activeLineId, setActiveLineId }) => (
  <div className="line-tabs line-tabs--main">
    <div className="line-tabs-header">
      <h4>Inbox lines</h4>
      <div className="chip-row">
        <button
          className={`chip ${!activeLineId ? 'active' : ''}`}
          onClick={() => setActiveLineId(null)}
        >
          All
        </button>
        {lines.map((line) => (
          <button
            key={line.id}
            className={`chip ${activeLineId === line.id ? 'active' : ''}`}
            onClick={() => setActiveLineId(line.id)}
            title={line.phoneNumber || 'Line'}
          >
            {line.label || line.phoneNumber || line.id.slice(0, 6)}
          </button>
        ))}
      </div>
    </div>
  </div>
));

LineTabs.displayName = 'LineTabs';

const BeaconPanel = ({
  user,
  db,
  selectedThread,
  lines,
  activeLineId,
  setActiveLineId,
  lineInboxMode,
  showPreviews,
  setActivePanel
}) => {
  const [messages, setMessages] = useState([]);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (new URLSearchParams(window.location.search).get('mock_user') === 'true') return;
    if (user && selectedThread) {
      const basePath = selectedThread.lineId
        ? ["users", user.uid, "lines", selectedThread.lineId, "threads", selectedThread.id, "messages"]
        : ["users", user.uid, "synced_threads", selectedThread.id, "messages"];
      const messagesRef = collection(db, ...basePath);

      const q = query(messagesRef, orderBy("date", "desc"), limit(50));

      const unsubscribe = onSnapshot(q, (snapshot) => {
        const messagesData = snapshot.docs.map(doc => {
          const data = doc.data();
          const date = data.date && typeof data.date.toMillis === 'function' ? data.date.toMillis() : (data.date || 0);
          return {
            id: doc.id,
            ...data,
            date: date,
            status: doc.metadata.hasPendingWrites ? 'sending' : (data.status || 'sent')
          };
        });
        setMessages(messagesData.reverse());
      });
      return () => unsubscribe();
    } else {
      setMessages([]);
    }
  }, [user, selectedThread, db]);

  // Bolt: Expose setMessages for testing
  useEffect(() => {
    window.debugSetMessages = setMessages;
  }, []);

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  return (
    <div className="beacon-layout">
      {lineInboxMode === 'PER_LINE' && lines.length > 0 && (
        <LineTabs lines={lines} activeLineId={activeLineId} setActiveLineId={setActiveLineId} />
      )}

      {!selectedThread ? (
        <div className="empty-state">
          <img src={beaconLogo} alt="Beacon" className="empty-logo" />
          <div>Select a thread or start a new message</div>
          <MessageComposer
            user={user}
            db={db}
            selectedThread={null}
            lineInboxMode={lineInboxMode}
            activeLineId={activeLineId}
            lines={lines}
          />
        </div>
      ) : (
        <>
          <div className="chat-header">
            <div>
              <h3>{selectedThread.address}</h3>
              {lineInboxMode === 'PER_LINE' && selectedThread.lineId && (
                <div className="chat-subtitle">From line {lines.find(l => l.id === selectedThread.lineId)?.label || selectedThread.lineId.slice(0,6)}</div>
              )}
            </div>
          </div>
          <div className="messages-list">
            {messages.map(msg => (
              <MessageItem key={msg.id} msg={msg} showPreviews={showPreviews} />
            ))}
            <div ref={messagesEndRef} style={{ height: 1 }} />
          </div>
          <MessageComposer
            user={user}
            db={db}
            selectedThread={selectedThread}
            lineInboxMode={lineInboxMode}
            activeLineId={activeLineId}
            lines={lines}
          />
        </>
      )}
    </div>
  );
};

export default BeaconPanel;
