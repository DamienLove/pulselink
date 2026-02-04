import { useState, useEffect } from 'react';
import { httpsCallable } from 'firebase/functions';
import { functions } from './firebase';

const LinkPreview = ({ url }) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active = true;
    const fetchPreview = async () => {
      try {
        const getPreview = httpsCallable(functions, 'getLinkPreview');
        const result = await getPreview({ url });
        if (active) {
            if (result.data.title || result.data.image) {
                setData(result.data);
            } else {
                setError(true); // No rich data found
            }
        }
      } catch (e) {
        console.error("Link preview failed", e);
        if (active) setError(true);
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchPreview();
    return () => { active = false; };
  }, [url]);

  if (error || (!loading && !data)) {
    return (
        <a href={url} target="_blank" rel="noopener noreferrer" className="plain-link">
            {url}
        </a>
    );
  }

  if (loading) {
      return <div className="link-preview-skeleton">
          <div className="skeleton-line" style={{width: '60%'}}/>
      </div>;
  }

  return (
    <a href={url} target="_blank" rel="noopener noreferrer" className="link-preview-card">
      {data.image && (
        <div className="link-preview-image" style={{ backgroundImage: `url(${data.image})` }} />
      )}
      <div className="link-preview-content">
        <div className="link-preview-title">{data.title || url}</div>
        {data.description && <div className="link-preview-desc">{data.description}</div>}
        <div className="link-preview-domain">{new URL(url).hostname}</div>
      </div>
    </a>
  );
};

export default LinkPreview;
