import { useEffect, useState, useCallback } from 'react';

const getToastClass = (msg) => {
  if (!msg) return 'toast';
  const lower = msg.toLowerCase();
  if (lower.includes('fail') || lower.includes('error') || lower.includes('missing')) return 'toast error';
  if (lower.includes('success') || lower.includes('saved') || lower.includes('updated') || lower.includes('sent') || lower.includes('published') || lower.includes('imported') || lower.includes('cleared')) return 'toast success';
  return 'toast';
};

const Toast = ({ message, onClose }) => {
  const [isVisible, setIsVisible] = useState(false);

  const handleDismiss = useCallback(() => {
    setIsVisible(false);
    // Wait for transition to finish before unmounting (calling onClose)
    setTimeout(() => {
      onClose();
    }, 300);
  }, [onClose]);

  useEffect(() => {
    if (message) {
      // Small delay to ensure mount animation plays first if needed,
      // or just set true immediately.
      setIsVisible(true);

      const toastClass = getToastClass(message);
      // Auto-dismiss if not an error
      if (!toastClass.includes('error')) {
        const timer = setTimeout(() => {
          handleDismiss();
        }, 4000);
        return () => clearTimeout(timer);
      }
    }
  }, [message, handleDismiss]);

  if (!message) return null;

  const toastClass = getToastClass(message);

  return (
    <div
        className={toastClass}
        role="status"
        aria-live="polite"
        style={{
            opacity: isVisible ? 1 : 0,
            transform: isVisible ? 'translateY(0)' : 'translateY(20px)',
            transition: 'opacity 0.3s ease, transform 0.3s cubic-bezier(0.16, 1, 0.3, 1)',
            // Override CSS animation if needed, or let it play on mount.
            // If we use transition for enter, we don't need CSS animation.
            // But CSS has pulseGlow too.
            animation: isVisible ? undefined : 'none'
        }}
    >
      <span style={{ flex: 1 }}>{message}</span>
      <button
        onClick={handleDismiss}
        style={{
            background: 'transparent',
            border: 'none',
            color: 'inherit',
            marginLeft: '12px',
            cursor: 'pointer',
            padding: '4px',
            display: 'flex',
            alignItems: 'center',
            opacity: 0.7,
            borderRadius: '50%'
        }}
        aria-label="Dismiss"
        className="toast-dismiss-btn"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    </div>
  );
};

export default Toast;
