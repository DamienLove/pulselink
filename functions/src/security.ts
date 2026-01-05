// Sentinel Helper: Escape HTML characters
// Prevents XSS and HTML injection by escaping dangerous characters

/**
 * Escapes HTML special characters to prevent XSS attacks.
 * This function is safe for use in HTML contexts (element content, attributes).
 *
 * @param unsafe - The string to escape. If null/undefined, returns empty string.
 * @return The escaped string with HTML entities replacing special characters
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
 * @param unsafe - The string to sanitize.
 * @return The sanitized string safe for single-line prompt fields.
 */
export function sanitizeScalar(unsafe: string | null | undefined): string {
  if (!unsafe) return "";
  if (typeof unsafe !== "string") unsafe = String(unsafe);

  // Replace newlines and control characters with a space
  return unsafe.replace(/[\r\n\x00-\x1F\x7F]+/g, " ").trim();
}
