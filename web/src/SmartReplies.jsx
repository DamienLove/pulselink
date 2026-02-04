import { useMemo } from 'react';

const generateSuggestions = (lastMessage) => {
  if (!lastMessage || !lastMessage.body) return [];
  const text = lastMessage.body.toLowerCase();

  // Simple heuristics
  if (text.includes('?')) {
    if (text.includes('where')) return ['At home', 'At work', 'On my way'];
    if (text.includes('when') || text.includes('time')) return ['Soon', 'In 5 mins', 'Tonight'];
    if (text.includes('how')) return ['Good!', 'Okay', 'Not great'];
    return ['Yes', 'No', 'Not sure'];
  }

  if (text.includes('hello') || text.includes('hi ') || text.trim() === 'hi') {
    return ['Hey!', 'Hi there', 'What\'s up?'];
  }

  if (text.includes('thanks') || text.includes('thank you')) {
    return ['You\'re welcome', 'No problem', 'Anytime'];
  }

  // Default
  return ['Ok', 'Sounds good', 'Talk later'];
};

const SmartReplies = ({ messages, onSelect }) => {
  const suggestions = useMemo(() => {
    // Find the last message that is NOT from the current user (type === 1 is received)
    // Assuming messages are sorted oldest to newest (which App.jsx does)
    // We want the very last message in the list, check if it is received
    if (!messages || messages.length === 0) return [];

    const lastMsg = messages[messages.length - 1];
    if (lastMsg.type !== 1) return []; // Only suggest if last was received

    return generateSuggestions(lastMsg);
  }, [messages]);

  if (suggestions.length === 0) return null;

  return (
    <div className="smart-replies fade-in">
      {suggestions.map((text, i) => (
        <button
          key={i}
          className="chip suggestion-chip"
          onClick={() => onSelect(text)}
        >
          {text}
        </button>
      ))}
    </div>
  );
};

export default SmartReplies;
