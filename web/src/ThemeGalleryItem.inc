// Bolt: Optimized ThemeGalleryItem to prevent re-renders when list doesn't change
const ThemeGalleryItem = memo(({ themeDoc, onImport }) => {
  const previewTheme = normalizeTheme(themeDoc.theme || {});
  const previewStyle = buildThemePreviewStyle(previewTheme);
  const authorLabel = themeDoc.anonymous
    ? 'Anonymous'
    : (themeDoc.authorHandle || themeDoc.authorName || 'Community');

  return (
    <div className="theme-card">
      <div className="theme-preview" style={previewStyle}>
        <div className="theme-preview-chat">
          <div
            className="theme-bubble incoming"
            style={{
              background: previewTheme.bubbleIncoming,
              color: previewTheme.onBubbleIncoming
            }}
          >
            Hey, you good?
          </div>
          <div
            className="theme-bubble outgoing"
            style={{
              background: previewTheme.bubbleOutgoing,
              color: previewTheme.onBubbleOutgoing
            }}
          >
            Yep, on my way!
          </div>
        </div>
      </div>
      <div className="theme-meta">
        <div className="theme-name">{themeDoc.name || 'Untitled'}</div>
        <div className="theme-author">{authorLabel}</div>
      </div>
      <button
        className="primary-btn"
        type="button"
        onClick={() => onImport(themeDoc)}
        aria-label={`Import theme ${themeDoc.name || 'Untitled'}`}
      >
        Import
      </button>
    </div>
  );
}, (prev, next) => {
  // Shallow comparison of themeDoc is sufficient as Firestore updates return new objects
  // but stable references for unchanged documents in the list
  return prev.themeDoc === next.themeDoc && prev.onImport === next.onImport;
});

ThemeGalleryItem.displayName = 'ThemeGalleryItem';
