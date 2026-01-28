// Sentinel Helper: Escape HTML characters
// Prevents XSS and HTML injection by escaping dangerous characters

/**
 * Escapes HTML special characters to prevent XSS attacks.
 * This function is safe for use in HTML contexts (element content, attributes).
 *
 * @param {string | null | undefined} unsafe - The string to escape. If null/undefined, returns empty string.
 * @return {string} The escaped string with HTML entities replacing special characters
 *
 * @example
 * escapeHtml('<script>alert("xss")</script>')
 * // Returns: '&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;'
 */
export function escapeHtml(unsafe: string | null | undefined): string {
  // Type safety: handle null, undefined, and non-string inputs
  if (unsafe === null || unsafe === undefined) {
    return "";
  }

  if (typeof unsafe !== "string") {
    // Convert to string for safety
    unsafe = String(unsafe);
  }

  return unsafe
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
}

/**
 * Sanitizes a scalar string to prevent prompt injection via structure manipulation.
 * Removes newlines and non-printable characters to keep the input on a single line.
 *
 * @param {string | null | undefined} unsafe - The string to sanitize.
 * @return {string} The sanitized string safe for single-line prompt fields.
 */
export function sanitizeScalar(unsafe: string | null | undefined): string {
  if (!unsafe) return "";
  if (typeof unsafe !== "string") unsafe = String(unsafe);

  // Replace newlines and control characters with a space
  // eslint-disable-next-line no-control-regex
  return unsafe.replace(/[\r\n\x00-\x1F\x7F]+/g, " ").trim();
}

/**
 * Validates that a string is a secure URL (http or https).
 *
 * @param {string} url - The string to validate.
 * @return {boolean} True if the string is a valid http/https URL.
 */
export function isValidUrl(url: string): boolean {
  // Sentinel: Prevent Denial of Service (DoS) via excessively long URLs
  if (url.length > 2048) {
    return false;
  }
  try {
    const parsed = new URL(url);
    return parsed.protocol === "http:" || parsed.protocol === "https:";
  } catch {
    return false;
  }
}
