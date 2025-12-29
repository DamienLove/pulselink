import * as functions from "firebase-functions";

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
    // Return only the access token, not the whole object (though client_credentials usually just returns token, type, expires)
    return {access_token: data.access_token};
  } catch (error) {
    console.error("Spotify token fetch failed", error);
    throw new functions.https.HttpsError("internal", "Failed to fetch Spotify token.");
  }
});
