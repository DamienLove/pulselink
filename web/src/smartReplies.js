export const getSmartReplies = (text) => {
  if (!text || typeof text !== 'string') return [];

  const lower = text.toLowerCase().trim();
  const suggestions = [];

  // Greetings
  // Matches start of string followed by greeting word and a word boundary
  if (/^(hi|hello|hey|greetings)\b/.test(lower)) {
    suggestions.push("Hi there!", "Hello!", "Hey!");
  }

  // Status check
  if (/\b(how are you|how's it going|how is it going|what's up)\b/.test(lower)) {
    suggestions.push("I'm good, thanks!", "Doing well, you?", "Can't complain.");
  }

  // Location
  if (/\b(where are you|what's your location|where u at)\b/.test(lower)) {
    suggestions.push("At home", "At work", "On my way", "Just leaving");
  }

  // Timing
  // Restrict 'when' to be more specific or start of string to avoid matching "tell me when..."
  if (/^(what time|when|how long)\b/.test(lower) || /\b(what time|when)\?/.test(lower)) {
    suggestions.push("Soon", "In 5 mins", "Later today", "Not sure yet");
  }

  // Gratitude
  if (/\b(thanks|thank you|thx)\b/.test(lower)) {
    suggestions.push("You're welcome!", "No problem!", "Anytime");
  }

  // Binary / Questions (catch-all for questions if no specific match)
  // Only trigger if we haven't found specific suggestions yet
  if (lower.endsWith('?') && suggestions.length === 0) {
    suggestions.push("Yes", "No", "Maybe", "OK", "Sure");
  }

  // Confirmation/Agreement (if someone says "I'm here" or "Done")
  // Restrict 'done'/'ready' to be full sentences or very short
  if (/^(i'm here|i am here|done|finished|ready)[.!]?$/.test(lower) || /\b(i'm here|i am here)\b/.test(lower)) {
    suggestions.push("Great!", "OK", "Coming", "Thanks");
  }

  // Limit to 3-4 unique suggestions
  return [...new Set(suggestions)].slice(0, 4);
};
