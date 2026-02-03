import './App.css';

const PanelSkeleton = () => {
  return (
    <div className="panel-skeleton" style={{ width: '100%', animation: 'fadeIn 0.5s ease' }}>
      <div className="skeleton-header" style={{ marginBottom: 32 }}>
        <div className="skeleton-line" style={{ width: '200px', height: '32px', marginBottom: '8px' }}></div>
        <div className="skeleton-line" style={{ width: '300px', height: '16px', opacity: 0.5 }}></div>
      </div>

      <div className="skeleton-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px' }}>
        {[1, 2, 3].map((i) => (
          <div key={i} className="settings-card" style={{ height: '200px', justifyContent: 'center' }}>
            <div className="skeleton-line" style={{ width: '60%', height: '24px', marginBottom: '16px' }}></div>
            <div className="skeleton-line" style={{ width: '100%', height: '12px', marginBottom: '8px', opacity: 0.6 }}></div>
            <div className="skeleton-line" style={{ width: '80%', height: '12px', opacity: 0.6 }}></div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default PanelSkeleton;
