import { getSpotifyAccessToken } from './spotify';
import * as functions from 'firebase-functions-test';

// Initialize the test SDK
const test = functions();

describe('getSpotifyAccessToken', () => {
    let oldEnv: NodeJS.ProcessEnv;

    beforeEach(() => {
        oldEnv = process.env;
        process.env = { ...oldEnv, SPOTIFY_CLIENT_ID: 'test_id', SPOTIFY_CLIENT_SECRET: 'test_secret' };
    });

    afterEach(() => {
        process.env = oldEnv;
        test.cleanup();
    });

    it('should throw unauthenticated error if no context is provided', async () => {
        const wrapped = test.wrap(getSpotifyAccessToken);
        await expect(wrapped({}, {})).rejects.toThrow('User must be logged in');
    });

    it('should throw failed-precondition if creds are missing', async () => {
         process.env.SPOTIFY_CLIENT_ID = '';
         const wrapped = test.wrap(getSpotifyAccessToken);
         await expect(wrapped({}, { auth: { uid: 'user123' } })).rejects.toThrow('Spotify credentials not configured');
    });
});
