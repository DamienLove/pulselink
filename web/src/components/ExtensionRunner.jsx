import { useRef, useEffect, useState } from 'react';
import { Spinner } from '../App'; // Assuming Spinner is exported or we redefine it.

// Message Type Constants
const EXTENSION_MSG = {
    PING: 'EXTENSION_PING',
    PONG: 'HOST_PONG',
    GET_USER: 'GET_USER',
    USER_DATA: 'USER_DATA',
    SCORE_UPDATE: 'EXTENSION_SCORE_UPDATE'
};

const ExtensionRunner = ({ extension, user, profile, onClose }) => {
    const iframeRef = useRef(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const handler = (event) => {
            // Security: Validate Origin
            // For this implementation, we allow extensions from the same origin (local samples)
            // AND the specific extension URL origin if different.

            if (!extension.url) return;

            try {
                const extensionOrigin = new URL(extension.url).origin;
                // If the event origin doesn't match the extension's origin, ignore it.
                // Note: For 'null' origin (sandboxed iframes without allow-same-origin),
                // origin validation is tricky. We might rely on the source window check.

                // Strict check:
                if (event.origin !== extensionOrigin && event.origin !== window.location.origin) {
                     // In production, log this violation
                     // console.warn("Blocked message from unknown origin:", event.origin);
                     return;
                }
            } catch (e) {
                // Invalid URL in extension, ignore
                return;
            }

            if (event.source !== iframeRef.current?.contentWindow) return;

            const { type, payload } = event.data || {};

            // Bridge Logic
            if (type === EXTENSION_MSG.PING) {
                console.log("Extension Ping:", payload);
                iframeRef.current.contentWindow.postMessage({
                    type: EXTENSION_MSG.PONG,
                    payload: { serverTime: Date.now() }
                }, event.origin); // Reply only to the sender origin
            }

            if (type === EXTENSION_MSG.GET_USER) {
                // Privacy: Only send basic info
                iframeRef.current.contentWindow.postMessage({
                    type: EXTENSION_MSG.USER_DATA,
                    payload: {
                        uid: user?.uid,
                        displayName: profile?.ownerName
                    }
                }, event.origin);
            }

            if (type === EXTENSION_MSG.SCORE_UPDATE) {
                console.log("Score update:", payload);
                // In a real app, we might save this to Firestore
            }
        };

        window.addEventListener('message', handler);
        return () => window.removeEventListener('message', handler);
    }, [extension, user, profile]);

    const handleLoad = () => {
        setIsLoading(false);
    };

    const handleError = () => {
        setIsLoading(false);
        setError('Failed to load extension.');
    };

    return (
        <div className="extension-runner-container" style={{
            position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
            background: 'var(--bg)', zIndex: 100, display: 'flex', flexDirection: 'column'
        }}>
            <div className="panel-header" style={{ padding: '10px 20px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
                    <button className="ghost-btn icon-only" onClick={onClose} aria-label="Close Extension">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
                    </button>
                    <h3>{extension.name}</h3>
                </div>
                <div className="badge">Running</div>
            </div>

            <div style={{flex: 1, position: 'relative'}}>
                {isLoading && (
                    <div style={{
                        position: 'absolute', inset: 0, display: 'flex',
                        alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 10
                    }}>
                        <Spinner />
                        <span>Loading extension...</span>
                    </div>
                )}
                {error && (
                    <div style={{
                        position: 'absolute', inset: 0, display: 'flex',
                        alignItems: 'center', justifyContent: 'center', color: 'var(--danger)'
                    }}>
                        {error}
                    </div>
                )}
                <iframe
                    ref={iframeRef}
                    src={extension.url}
                    style={{ border: 'none', width: '100%', height: '100%', opacity: isLoading ? 0 : 1 }}
                    title={extension.name}
                    // Security: Removed allow-same-origin
                    sandbox="allow-scripts allow-forms allow-popups"
                    onLoad={handleLoad}
                    onError={handleError}
                />
            </div>
        </div>
    );
};

export default ExtensionRunner;
