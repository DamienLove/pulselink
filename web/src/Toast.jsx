import { createContext, useContext, useState, useCallback, memo, useEffect } from 'react';
import './App.css'; // Ensure we have access to styles

const ToastContext = createContext(null);

let idCounter = 0;

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};

const ToastItem = memo(({ id, message, type, duration = 4000, onDismiss }) => {
  const [isExiting, setIsExiting] = useState(false);

  const handleDismiss = useCallback(() => {
    setIsExiting(true);
    // Wait for animation to finish before removing from DOM
    setTimeout(() => {
      onDismiss(id);
    }, 400); // Match CSS animation duration
  }, [id, onDismiss]);

  useEffect(() => {
    if (duration && type !== 'loading') {
      const timer = setTimeout(handleDismiss, duration);
      return () => clearTimeout(timer);
    }
  }, [duration, type, handleDismiss]);

  const iconMap = {
    success: '✓',
    error: '!',
    loading: '⟳',
    info: 'i'
  };

  return (
    <div
      className={`toast-item toast-${type} ${isExiting ? 'toast-exit' : 'toast-enter'}`}
      role="alert"
    >
      <div className={`toast-icon ${type === 'loading' ? 'toast-spinner' : ''}`}>
        {iconMap[type] || 'i'}
      </div>
      <div className="toast-content">{message}</div>
      {type !== 'loading' && (
        <button className="toast-close" onClick={handleDismiss} aria-label="Dismiss">
          ×
        </button>
      )}
    </div>
  );
});

ToastItem.displayName = 'ToastItem';

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const add = useCallback((message, type = 'info', duration = 4000, id = null) => {
    const newId = id || `toast-${++idCounter}`;
    setToasts((prev) => {
      // If updating an existing toast (e.g. loading -> success), replace it
      const exists = prev.find(t => t.id === newId);
      if (exists) {
        return prev.map(t => t.id === newId ? { ...t, message, type, duration } : t);
      }
      return [...prev, { id: newId, message, type, duration }];
    });
    return newId;
  }, []);

  const success = useCallback((msg, options = {}) => add(msg, 'success', options.duration, options.id), [add]);
  const error = useCallback((msg, options = {}) => add(msg, 'error', options.duration, options.id), [add]);
  const info = useCallback((msg, options = {}) => add(msg, 'info', options.duration, options.id), [add]);
  const loading = useCallback((msg, options = {}) => add(msg, 'loading', 0, options.id), [add]); // 0 duration = indefinite

  // Helper to update a toast (e.g. from loading to success)
  const update = useCallback((id, msg, type = 'success', duration = 4000) => {
    add(msg, type, duration, id);
  }, [add]);

  // Dismiss a specific toast programmatically (e.g. remove loading toast)
  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const value = {
    add,
    remove,
    success,
    error,
    info,
    loading,
    update,
    dismiss
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-container">
        {toasts.map((toast) => (
          <ToastItem
            key={toast.id}
            {...toast}
            onDismiss={remove}
          />
        ))}
      </div>
    </ToastContext.Provider>
  );
};
