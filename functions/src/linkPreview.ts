import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as cheerio from "cheerio";
import * as logger from "firebase-functions/logger";

interface LinkPreviewData {
  title?: string;
  description?: string;
  image?: string;
  url: string;
}

export const getLinkPreview = onCall< {url: string} >(async (request) => {
  const {url} = request.data;

  if (!request.auth) {
    throw new HttpsError("unauthenticated", "User must be logged in.");
  }

  if (!url || typeof url !== "string") {
    throw new HttpsError("invalid-argument", "URL is required.");
  }

  // Sentinel: Basic URL validation
  let parsedUrl;
  try {
    parsedUrl = new URL(url);
    if (!['http:', 'https:'].includes(parsedUrl.protocol)) {
        throw new Error('Invalid protocol');
    }
  } catch (e) {
    throw new HttpsError("invalid-argument", "Invalid URL protocol.");
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 5000); // 5s timeout

  try {
    const response = await fetch(url, {
        headers: {
            "User-Agent": "PulseLinkBot/1.0 (+https://pulselink.app)"
        },
        signal: controller.signal as any // Type assertion for compatibility if needed
    });

    // Clear timeout on response headers received
    // Note: If body is huge, we still need to handle reading time, but let's check size first.

    if (!response.ok) {
        clearTimeout(timeout);
        logger.warn(`Failed to fetch ${url}: ${response.status}`);
        return { url };
    }

    const contentLength = response.headers.get("content-length");
    if (contentLength && parseInt(contentLength, 10) > 5 * 1024 * 1024) {
        clearTimeout(timeout);
        logger.warn(`Skipping large file ${url} (${contentLength} bytes)`);
        return { url };
    }

    // Read body (this will still time out if it takes too long due to the signal)
    const html = await response.text();
    clearTimeout(timeout);
    const $ = cheerio.load(html);

    const title = $('meta[property="og:title"]').attr('content') || $('title').text() || '';
    const description = $('meta[property="og:description"]').attr('content') || $('meta[name="description"]').attr('content') || '';
    const image = $('meta[property="og:image"]').attr('content') || '';

    const preview: LinkPreviewData = {
        url,
        title: title.trim(),
        description: description.trim(),
        image
    };

    return preview;
  } catch (error: any) {
    logger.error("Error fetching link preview", error);
    // Return empty preview on error rather than throwing, so UI just shows link
    return { url };
  }
});
