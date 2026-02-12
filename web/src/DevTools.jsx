import { useState } from 'react';
import { db, auth } from './firebase';
import { doc, serverTimestamp, writeBatch } from "firebase/firestore";

/**
 * Constants for mock data to avoid magic numbers/strings
 */
const MOCK_DATA = {
  LINE_ID: "line_mock_1",
  PHONE_NUMBER: "+15550199",
  THREAD_ID: "thread_1",
  THREAD_ADDRESS: "+15551234567",
  THREAD_NAME: "Alice Wonderland",
  THREAD_SNIPPET: "Hey, are we still on for dinner?",
  VERIFICATION_THREAD_ID: "thread_2",
  VERIFICATION_ADDRESS: "28400",
  VERIFICATION_NAME: "Bank Alert",
  VERIFICATION_SNIPPET: "Your verification code is 849201. Do not share this code."
};

/**
 * DevTools Component
 *
 * DEBUG/DEVELOPMENT ONLY.
 * Allows developers to inject mock data into the connected Firebase project
 * to simulate premium user state and synced messages without needing an Android device.
 *
 * SECURITY WARNING: This component allows writing to the database.
 * It ensures it only runs in development mode, but should never be exposed in production builds.
 */
const DevTools = ({ isVisible, onClose }) => {
  const [status, setStatus] = useState('');
  const [userId, setUserId] = useState('test_user_123');

  const populateMockData = async () => {
    // SECURITY: Double-check we are in DEV mode
    if (!import.meta.env.DEV) {
      setStatus('Error: Operations restricted to development environment.');
      return;
    }

    try {
      setStatus('Populating...');

      // Use current user if logged in, otherwise use test user ID
      const uid = auth.currentUser ? auth.currentUser.uid : userId;

      const batch = writeBatch(db);

      // 1. Set Premium Status
      const userRef = doc(db, "users", uid);
      batch.set(userRef, {
        subscriptionStatus: 'premium',
        remoteWebAccessEnabled: true,
        ownerName: 'Test User',
        createdAt: serverTimestamp()
      }, { merge: true });

      // 2. Create a Mock Line
      const lineRef = doc(db, "users", uid, "lines", MOCK_DATA.LINE_ID);
      batch.set(lineRef, {
        label: "Pixel 7 Pro",
        phoneNumber: MOCK_DATA.PHONE_NUMBER,
        updatedAt: serverTimestamp()
      });

      // 3. Create Threads (Legacy & Line)
      const threads = [
        {
          id: MOCK_DATA.THREAD_ID,
          address: MOCK_DATA.THREAD_ADDRESS,
          display_name: MOCK_DATA.THREAD_NAME,
          snippet: MOCK_DATA.THREAD_SNIPPET,
          // Using Date.now() for message/thread timestamps to match client usage
          date: Date.now() - 1000 * 60 * 5, // 5 mins ago
          unread: true,
          unreadCount: 2
        },
        {
          id: MOCK_DATA.VERIFICATION_THREAD_ID,
          address: MOCK_DATA.VERIFICATION_ADDRESS,
          display_name: MOCK_DATA.VERIFICATION_NAME,
          snippet: MOCK_DATA.VERIFICATION_SNIPPET,
          date: Date.now() - 1000 * 60 * 60 * 2, // 2 hours ago
          unread: false
        }
      ];

      for (const t of threads) {
        // Legacy path
        const tRef = doc(db, "users", uid, "synced_threads", t.id);
        batch.set(tRef, t);

        // Line path
        const lineTRef = doc(db, "users", uid, "lines", MOCK_DATA.LINE_ID, "threads", t.id);
        batch.set(lineTRef, t);
      }

      await batch.commit();

      // 4. Create Messages for Thread 1
      const msgs = [
        { body: "Hello!", date: Date.now() - 1000 * 60 * 60, type: 2 }, // Sent
        { body: "Hi there!", date: Date.now() - 1000 * 60 * 55, type: 1 }, // Received
        { body: "Are we still on for dinner?", date: Date.now() - 1000 * 60 * 5, type: 1 } // Received
      ];

      const msgBatch = writeBatch(db);
      for (const m of msgs) {
         const msgId = `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
         const mRef = doc(db, "users", uid, "synced_threads", MOCK_DATA.THREAD_ID, "messages", msgId);
         const lineMRef = doc(db, "users", uid, "lines", MOCK_DATA.LINE_ID, "threads", MOCK_DATA.THREAD_ID, "messages", msgId);
         msgBatch.set(mRef, m);
         msgBatch.set(lineMRef, m);
      }
      await msgBatch.commit();

      setStatus('Success! Reload or check SoText Inbox.');
    } catch (e) {
      console.error('Failed to populate mock data:', e);
      setStatus('Error: ' + (e.code || 'Unknown') + ' - ' + e.message);
    }
  };

  if (!isVisible) return null;

  return (
    <div style={{
      position: 'fixed',
      bottom: 20,
      right: 20,
      background: '#111',
      border: '1px solid #333',
      padding: 16,
      borderRadius: 8,
      zIndex: 9999,
      color: '#fff',
      maxWidth: 300
    }}>
      <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 10}}>
        <strong>Dev Tools</strong>
        <button
          onClick={onClose}
          aria-label="Close DevTools"
          style={{background: 'transparent', border: 'none', color: '#999', cursor: 'pointer'}}
        >
          X
        </button>
      </div>

      <div style={{marginBottom: 10}}>
         <label htmlFor="dev-target-user-id" style={{display:'block', fontSize: 12, marginBottom: 4}}>
           Target User ID (if not logged in)
         </label>
         <input
           id="dev-target-user-id"
           value={userId}
           onChange={e => setUserId(e.target.value)}
           style={{width: '100%', background: '#222', border: '1px solid #444', color: '#fff', padding: 4}}
         />
      </div>

      <button
        onClick={populateMockData}
        style={{
          width: '100%',
          background: '#0ea5e9',
          border: 'none',
          padding: '8px',
          color: 'white',
          borderRadius: 4,
          cursor: 'pointer',
          marginBottom: 8
        }}
      >
        Populate Mock Data
      </button>

      {status && <div style={{fontSize: 12, color: '#aaa'}} role="alert">{status}</div>}
    </div>
  );
};

export default DevTools;
