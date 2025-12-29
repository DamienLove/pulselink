import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Simple in-memory cache for the token
// Note: This cache is local to the function instance. Firebase Functions may spin up multiple instances.
// For a more robust global cache, we would use Firestore or Redis, but in-memory is a good first step
// to reduce rate limits on hot instances.
interface CachedToken {
  token: string;
  expiresAt: number;
}

let tokenCache: CachedToken | null = null;

/**
 * Fetches a Spotify access token using client credentials.
 * This function should be used instead of exposing client secrets in the frontend.
 *
 * Requires environment variables:
 * - SPOTIFY_CLIENT_ID
 * - SPOTIFY_CLIENT_SECRET
 */
export const getSpotifyAccessToken = functions.https.onCall(async (_data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
  }

  // Rate Limiting: Check if user has made too many requests recently
  // We'll use a Firestore counter for this.
  const uid = context.auth.uid;
  const db = admin.firestore();
  const limitRef = db.collection("users").doc(uid).collection("rate_limits").doc("spotify_token");

  try {
    const limitSnap = await limitRef.get();
    const now = Date.now();
    const oneMinute = 60 * 1000;

    if (limitSnap.exists) {
        const data = limitSnap.data();
        // Reset count if window passed
        if (now - (data?.lastRequest || 0) > oneMinute) {
             await limitRef.set({ count: 1, lastRequest: now });
        } else {
             if ((data?.count || 0) >= 10) { // Limit to 10 requests per minute
                 throw new functions.https.HttpsError("resource-exhausted", "Too many requests. Please try again later.");
             }
             await limitRef.update({ count: admin.firestore.FieldValue.increment(1) });
        }
    } else {
        await limitRef.set({ count: 1, lastRequest: now });
    }
  } catch(e) {
      // If Firestore fails, we log but allow the request to proceed to avoid breaking functionality
      // due to rate limit system failure, unless it was the specific error we threw.
      if (e instanceof functions.https.HttpsError) throw e;
      console.warn("Rate limit check failed", e);
  }


  // Cache Check
  if (tokenCache && tokenCache.expiresAt > Date.now()) {
      return { access_token: tokenCache.token, expires_in: Math.floor((tokenCache.expiresAt - Date.now()) / 1000) };
  }

  const clientId = process.env.SPOTIFY_CLIENT_ID;
  const clientSecret = process.env.SPOTIFY_CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    console.error("Spotify credentials missing in environment variables.");
    throw new functions.https.HttpsError(
        "failed-precondition",
        "Spotify credentials not configured on server.",
    );
  }

  try {
    const authString = Buffer.from(`${clientId}:${clientSecret}`).toString("base64");
    const response = await fetch("https://accounts.spotify.com/api/token", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "Authorization": `Basic ${authString}`,
      },
      body: "grant_type=client_credentials",
    });

    if (!response.ok) {
      console.error("Spotify token fetch failed with status:", response.status);
      throw new Error(`Spotify API error: ${response.statusText}`);
    }

    const data: any = await response.json();

    // Cache the token
    // data.expires_in is usually 3600 seconds. We cache for slightly less to be safe.
    if (data.access_token && data.expires_in) {
        tokenCache = {
            token: data.access_token,
            expiresAt: Date.now() + ((data.expires_in - 60) * 1000)
        };
    }

    return {
        access_token: data.access_token,
        expires_in: data.expires_in
    };
  } catch (error) {
    console.error("Spotify token fetch failed", error);
    throw new functions.https.HttpsError("internal", "Failed to fetch Spotify token.");
  }
});
