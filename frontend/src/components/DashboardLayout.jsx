import React, { useState, useEffect, useRef } from 'react';
import { DocumentDropzone } from './DocumentDropzone';

export const DashboardLayout = () => {
  const [documents, setDocuments] = useState([]);
  const [selectedDoc, setSelectedDoc] = useState(null);
  const [showUpload, setShowUpload] = useState(false);
  const [page, setPage] = useState('feed');
  const [chatMessages, setChatMessages] = useState([]);
  const [chatInput, setChatInput] = useState('');
  const [chatLoading, setChatLoading] = useState(false);
  const [loadingDocs, setLoadingDocs] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [totalDocs, setTotalDocs] = useState(0);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [entities, setEntities] = useState(null);
  const [stats, setStats] = useState(null);
  const [settings, setSettings] = useState(null);
  const [savingSettings, setSavingSettings] = useState(false);
  const scrollContainerRef = useRef(null);

  // Dark theme colors
  const theme = settings?.darkTheme ? {
    bg: '#1a1a1a',
    bgSecondary: '#2d2d2d',
    bgTertiary: '#3a3a3a',
    text: '#e0e0e0',
    textSecondary: '#b0b0b0',
    border: '#404040',
    accent: '#1a9b71',
    accentLight: '#2ab886',
    error: '#ff6b6b',
    success: '#51cf66'
  } : {
    bg: '#f5f5f5',
    bgSecondary: 'white',
    bgTertiary: '#f0f0f0',
    text: '#1a1a1a',
    textSecondary: '#666',
    border: '#e0e0e0',
    accent: '#1a9b71',
    accentLight: '#2ab886',
    error: '#c62828',
    success: '#1a9b71'
  };

  useEffect(() => {
    loadDocuments(0, true);
    loadAnalytics();
    loadSettings();
  }, []);

  useEffect(() => {
    const container = scrollContainerRef.current;
    if (!container) return;

    const handleScroll = () => {
      if (isLoadingMore || !hasMore) return;
      
      const { scrollTop, scrollHeight, clientHeight } = container;
      if (scrollHeight - scrollTop - clientHeight < 500) {
        loadMore();
      }
    };

    container.addEventListener('scroll', handleScroll);
    return () => container.removeEventListener('scroll', handleScroll);
  }, [hasMore, isLoadingMore, currentPage]);

  const loadAnalytics = async () => {
    try {
      const [entitiesRes, statsRes] = await Promise.all([
        fetch(`${process.env.REACT_APP_API_URL}/analytics/entities`),
        fetch(`${process.env.REACT_APP_API_URL}/analytics/stats`)
      ]);
      
      if (entitiesRes.ok) setEntities(await entitiesRes.json());
      if (statsRes.ok) setStats(await statsRes.json());
    } catch (err) {
      console.error('Error loading analytics:', err);
    }
  };

  const loadSettings = async () => {
    try {
      const res = await fetch(`${process.env.REACT_APP_API_URL}/settings`);
      if (res.ok) {
        const data = await res.json();
        setSettings(data);
      }
    } catch (err) {
      console.error('Error loading settings:', err);
    }
  };

  const handleSaveSettings = async () => {
    if (!settings) return;
    setSavingSettings(true);
    try {
      const res = await fetch(`${process.env.REACT_APP_API_URL}/settings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(settings)
      });
      if (res.ok) {
        alert('Settings saved successfully!');
      } else {
        alert('Error saving settings');
      }
    } catch (err) {
      console.error('Error saving settings:', err);
      alert('Error saving settings: ' + err.message);
    } finally {
      setSavingSettings(false);
    }
  };

  const loadDocuments = async (pageNum, reset = false) => {
    try {
      const response = await fetch(
        `${process.env.REACT_APP_API_URL}/documents/list?page=${pageNum}&size=20`,
        { method: 'GET', headers: { 'Content-Type': 'application/json' } }
      );

      if (!response.ok) {
        setDocuments(reset ? [] : docs => docs);
        setLoadingDocs(false);
        return;
      }

      const data = await response.json();
      if (reset || pageNum === 0) {
        setDocuments(data.documents || []);
      } else {
        setDocuments(prev => [...prev, ...(data.documents || [])]);
      }

      setCurrentPage(pageNum);
      setTotalDocs(data.totalElements || 0);
      setHasMore(data.hasMore || false);
    } catch (err) {
      console.error('Error loading documents:', err);
      if (reset) setDocuments([]);
    } finally {
      setLoadingDocs(false);
      setIsLoadingMore(false);
    }
  };

  const loadMore = async () => {
    if (isLoadingMore || !hasMore) return;
    setIsLoadingMore(true);
    await loadDocuments(currentPage + 1);
  };

  const handleDocumentProcessed = (doc) => {
    setDocuments(prev => [doc, ...prev]);
    setShowUpload(false);
    setTotalDocs(prev => prev + 1);
    loadAnalytics();
  };

  const handleChatSubmit = async () => {
    if (!chatInput.trim() || !selectedDoc) return;
    
    const userMessage = chatInput;
    setChatInput('');
    setChatMessages(prev => [...prev, { type: 'user', text: userMessage }]);
    setChatLoading(true);
    
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL}/chat/stream`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: userMessage,
          conversationId: selectedDoc.documentId,
          contextLimit: 5
        })
      });

      if (!response.ok) throw new Error(`HTTP ${response.status}`);

      const data = await response.json();
      
      if (data.response) {
        setChatMessages(prev => [...prev, { type: 'assistant', text: data.response }]);
      } else if (data.error) {
        setChatMessages(prev => [...prev, { type: 'error', text: 'Error: ' + data.error }]);
      } else {
        setChatMessages(prev => [...prev, { type: 'error', text: 'No response received' }]);
      }
    } catch (err) {
      console.error('Chat error:', err);
      setChatMessages(prev => [...prev, { type: 'error', text: 'Error: ' + err.message }]);
    } finally {
      setChatLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh', background: theme.bg, fontFamily: 'Arial, sans-serif', color: theme.text }}>
      {/* SIDEBAR */}
      <aside style={{ width: '220px', background: theme.bgSecondary, borderRight: `1px solid ${theme.border}`, padding: '20px', display: 'flex', flexDirection: 'column', position: 'relative', zIndex: 100 }}>
        <div style={{ marginBottom: '30px' }}>
          <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
            <span style={{ fontSize: '28px' }}>⚙️</span>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.text }}>DE MATRIX</div>
              <div style={{ fontSize: '11px', color: theme.textSecondary }}>Analysis Engine</div>
            </div>
          </div>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: '8px', flex: 1, pointerEvents: 'auto' }}>
          {[
            { id: 'feed', icon: '📊', label: 'Document Feed' },
            { id: 'entities', icon: '🗺️', label: 'Entity Map' },
            { id: 'agents', icon: '🤖', label: 'Agent Director' },
            { id: 'settings', icon: '⚙️', label: 'Settings' }
          ].map(item => (
            <button 
              key={item.id}
              onClick={() => setPage(item.id)}
              style={{ 
                display: 'flex', 
                gap: '12px', 
                padding: '12px 14px', 
                background: page === item.id ? theme.bgTertiary : 'transparent',
                border: 'none',
                borderRadius: '6px',
                cursor: 'pointer',
                fontSize: '13px',
                color: page === item.id ? theme.text : theme.textSecondary,
                fontWeight: page === item.id ? 600 : 500,
                alignItems: 'center'
              }}
            >
              <span style={{ fontSize: '18px' }}>{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div style={{ paddingTop: '20px', borderTop: `1px solid ${theme.border}` }}>
          <div style={{ display: 'flex', gap: '8px', fontWeight: 700, fontSize: '12px', color: theme.accent }}>
            <span>🧬</span>
            <span>DE MATRIX</span>
          </div>
        </div>
      </aside>

      {/* MAIN */}
      <main style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {page === 'feed' && (
          <>
            <header style={{ background: theme.bgSecondary, borderBottom: `1px solid ${theme.border}`, padding: '24px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h1 style={{ margin: 0, fontSize: '28px', fontWeight: 700, color: theme.text }}>Document Analysis Feed</h1>
                <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: theme.textSecondary }}>
                  {loadingDocs ? 'Loading...' : `Showing ${documents.length} of ${totalDocs} documents`}
                </p>
              </div>
              <button onClick={() => setShowUpload(!showUpload)} style={{ padding: '10px 16px', background: theme.accent, color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 600 }}>
                📤 {showUpload ? 'Cancel' : 'Upload'}
              </button>
            </header>

            {showUpload && (
              <div style={{ background: theme.bgSecondary, padding: '16px 32px', borderBottom: `1px solid ${theme.border}` }}>
                <DocumentDropzone onDocumentProcessed={handleDocumentProcessed} />
              </div>
            )}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: '24px', padding: '24px 32px', flex: 1, overflow: 'hidden' }}>
              <div 
                ref={scrollContainerRef}
                style={{ background: theme.bgSecondary, borderRadius: '8px', padding: '16px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '16px' }}
              >
                {documents.length === 0 && !loadingDocs ? (
                  <div style={{ textAlign: 'center', padding: '60px 40px', color: theme.textSecondary }}>
                    <p>📄 No documents analyzed yet</p>
                    <button onClick={() => setShowUpload(true)} style={{ marginTop: '16px', padding: '8px 16px', background: theme.accent, color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>
                      Upload
                    </button>
                  </div>
                ) : (
                  <>
                    {documents.map((doc) => (
                      <div 
                        key={doc.documentId || doc.id} 
                        onClick={() => setSelectedDoc(doc)} 
                        style={{ background: theme.bgTertiary, border: `1px solid ${theme.border}`, borderRadius: '8px', padding: '20px', cursor: 'pointer', borderLeft: `4px solid ${theme.accent}`, transition: 'all 0.2s', flexShrink: 0 }}
                      >
                        <h3 style={{ margin: '0 0 8px 0', fontSize: '18px', fontWeight: 700, color: theme.text }}>{doc.fileName}</h3>
                        <p style={{ margin: 0, fontSize: '13px', color: theme.textSecondary }}>📊 {doc.chunkCount} chunks • {doc.status}</p>
                      </div>
                    ))}
                    {isLoadingMore && (
                      <div style={{ textAlign: 'center', padding: '16px', color: theme.textSecondary }}>
                        <p>Loading more documents...</p>
                      </div>
                    )}
                  </>
                )}
              </div>

              <div style={{ background: theme.bgSecondary, border: `1px solid ${theme.border}`, borderRadius: '8px', padding: '16px', overflowY: 'auto' }}>
                <h3 style={{ margin: '0 0 16px 0', fontSize: '14px', fontWeight: 600, color: theme.text }}>System Status 🟢</h3>
                <div style={{ fontSize: '12px', color: theme.textSecondary }}>
                  <div style={{ marginBottom: '12px' }}>
                    <div style={{ fontWeight: 600, color: theme.text }}>Documents</div>
                    <div>{documents.length} / {totalDocs} loaded</div>
                  </div>
                  <div style={{ marginBottom: '12px' }}>
                    <div style={{ fontWeight: 600, color: theme.text }}>Status</div>
                    <div>{hasMore ? 'More available' : 'All loaded'}</div>
                  </div>
                  <div>
                    <div style={{ fontWeight: 600, color: theme.text }}>Vector Search</div>
                    <div>Ready</div>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}

        {page === 'entities' && (
          <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: theme.bgSecondary, overflow: 'auto' }}>
            <header style={{ background: theme.bgSecondary, borderBottom: `1px solid ${theme.border}`, padding: '24px 32px' }}>
              <h1 style={{ margin: 0, fontSize: '28px', fontWeight: 700, color: theme.text }}>Entity Map</h1>
              <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: theme.textSecondary }}>Extracted entities from {stats?.totalDocuments || 0} documents</p>
            </header>
            <div style={{ padding: '32px', flex: 1, overflowY: 'auto' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '24px' }}>
                {entities && [
                  { title: 'Persons', icon: '👤', data: entities.persons, color: '#667eea' },
                  { title: 'Organizations', icon: '🏢', data: entities.organizations, color: '#764ba2' },
                  { title: 'Locations', icon: '📍', data: entities.locations, color: '#f093fb' },
                  { title: 'Products', icon: '📦', data: entities.products, color: '#4facfe' },
                  { title: 'Events', icon: '🎯', data: entities.events, color: '#43e97b' },
                  { title: 'Technologies', icon: '💻', data: entities.technologies, color: '#fa709a' }
                ].map((category, idx) => (
                  <div key={idx} style={{ background: theme.bgTertiary, border: `2px solid ${category.color}`, borderRadius: '8px', padding: '16px' }}>
                    <h3 style={{ margin: '0 0 12px 0', fontSize: '16px', fontWeight: 600, color: category.color }}>{category.icon} {category.title}</h3>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                      {category.data && category.data.length > 0 ? (
                        category.data.map((item, i) => (
                          <span key={i} style={{ display: 'inline-block', padding: '4px 10px', background: `${category.color}15`, border: `1px solid ${category.color}40`, borderRadius: '4px', fontSize: '12px', color: category.color }}>
                            {item}
                          </span>
                        ))
                      ) : (
                        <span style={{ fontSize: '12px', color: theme.textSecondary }}>No {category.title.toLowerCase()} found</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {page === 'agents' && (
          <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: theme.bgSecondary, overflow: 'auto' }}>
            <header style={{ background: theme.bgSecondary, borderBottom: `1px solid ${theme.border}`, padding: '24px 32px' }}>
              <h1 style={{ margin: 0, fontSize: '28px', fontWeight: 700, color: theme.text }}>Agent Director</h1>
              <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: theme.textSecondary }}>System performance and processing metrics</p>
            </header>
            <div style={{ padding: '32px', flex: 1 }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', marginBottom: '24px' }}>
                {stats && [
                  { name: 'Document Processor', status: 'active', metric: 'Documents', value: stats.totalDocuments },
                  { name: 'Vector Embedder', status: 'active', metric: 'Embeddings', value: stats.totalChunks },
                  { name: 'Semantic Search', status: 'active', metric: 'Chunks/Doc', value: stats.avgChunksPerDoc },
                  { name: 'RAG Pipeline', status: 'active', metric: 'Size', value: `${stats.totalSizeGB} GB` }
                ].map((agent, idx) => (
                  <div key={idx} style={{ background: theme.bgTertiary, border: `1px solid ${theme.border}`, borderRadius: '8px', padding: '16px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                      <h3 style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: theme.text }}>{agent.name}</h3>
                      <span style={{ display: 'inline-block', width: '8px', height: '8px', borderRadius: '50%', background: agent.status === 'active' ? theme.accent : theme.textSecondary }}></span>
                    </div>
                    <div style={{ fontSize: '12px', color: theme.textSecondary, marginBottom: '8px' }}>Status: <span style={{ fontWeight: 600, color: theme.text }}>{agent.status}</span></div>
                    <div style={{ fontSize: '14px', fontWeight: 700, color: theme.text }}>{agent.metric}: <span style={{ color: theme.accent }}>{agent.value}</span></div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {page === 'settings' && (
          <div style={{ display: 'flex', flexDirection: 'column', height: '100%', background: theme.bgSecondary, overflow: 'auto' }}>
            <header style={{ background: theme.bgSecondary, borderBottom: `1px solid ${theme.border}`, padding: '24px 32px' }}>
              <h1 style={{ margin: 0, fontSize: '28px', fontWeight: 700, color: theme.text }}>Settings</h1>
              <p style={{ margin: '4px 0 0 0', fontSize: '14px', color: theme.textSecondary }}>System configuration and preferences</p>
            </header>
            <div style={{ padding: '32px', flex: 1 }}>
              <div style={{ maxWidth: '600px' }}>
                {settings ? (
                  <>
                    <div style={{ marginBottom: '24px' }}>
                      <h2 style={{ margin: '0 0 16px 0', fontSize: '16px', fontWeight: 600, color: theme.text }}>Theme</h2>
                      <div style={{ background: theme.bgTertiary, border: `1px solid ${theme.border}`, borderRadius: '8px', padding: '16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: theme.text }}>Dark Mode</label>
                        <input 
                          type="checkbox" 
                          checked={settings.darkTheme || false} 
                          onChange={(e) => setSettings({...settings, darkTheme: e.target.checked})}
                          style={{ width: '20px', height: '20px', cursor: 'pointer' }} 
                        />
                      </div>
                    </div>

                    <div style={{ marginBottom: '24px' }}>
                      <h2 style={{ margin: '0 0 16px 0', fontSize: '16px', fontWeight: 600, color: theme.text }}>Processing Settings</h2>
                      <div style={{ background: theme.bgTertiary, border: `1px solid ${theme.border}`, borderRadius: '8px', padding: '16px', marginBottom: '12px' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px', color: theme.text }}>Chunk Size</label>
                        <input 
                          type="number" 
                          value={settings.chunkSize} 
                          onChange={(e) => setSettings({...settings, chunkSize: parseInt(e.target.value) || 0})}
                          style={{ width: '100%', padding: '8px', border: `1px solid ${theme.border}`, borderRadius: '4px', fontSize: '13px', boxSizing: 'border-box', background: theme.bg, color: theme.text }} 
                        />
                      </div>
                      <div style={{ background: theme.bgTertiary, border: `1px solid ${theme.border}`, borderRadius: '8px', padding: '16px' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px', color: theme.text }}>Chunk Overlap</label>
                        <input 
                          type="number" 
                          value={settings.chunkOverlap} 
                          onChange={(e) => setSettings({...settings, chunkOverlap: parseInt(e.target.value) || 0})}
                          style={{ width: '100%', padding: '8px', border: `1px solid ${theme.border}`, borderRadius: '4px', fontSize: '13px', boxSizing: 'border-box', background: theme.bg, color: theme.text }} 
                        />
                      </div>
                    </div>

                    <div style={{ marginBottom: '24px' }}>
                      <h2 style={{ margin: '0 0 16px 0', fontSize: '16px', fontWeight: 600, color: theme.text }}>Search Settings</h2>
                      <div style={{ background: theme.bgTertiary, border: `1px solid ${theme.border}`, borderRadius: '8px', padding: '16px', marginBottom: '12px' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px', color: theme.text }}>Top-K Results</label>
                        <input 
                          type="number" 
                          value={settings.topKResults} 
                          onChange={(e) => setSettings({...settings, topKResults: parseInt(e.target.value) || 0})}
                          style={{ width: '100%', padding: '8px', border: `1px solid ${theme.border}`, borderRadius: '4px', fontSize: '13px', boxSizing: 'border-box', background: theme.bg, color: theme.text }} 
                        />
                      </div>
                      <div style={{ background: theme.bgTertiary, border: `1px solid ${theme.border}`, borderRadius: '8px', padding: '16px' }}>
                        <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px', color: theme.text }}>Similarity Threshold</label>
                        <input 
                          type="number" 
                          min="0" 
                          max="1" 
                          step="0.1" 
                          value={settings.similarityThreshold} 
                          onChange={(e) => setSettings({...settings, similarityThreshold: parseFloat(e.target.value) || 0})}
                          style={{ width: '100%', padding: '8px', border: `1px solid ${theme.border}`, borderRadius: '4px', fontSize: '13px', boxSizing: 'border-box', background: theme.bg, color: theme.text }} 
                        />
                      </div>
                    </div>

                    <button 
                      onClick={handleSaveSettings} 
                      disabled={savingSettings} 
                      style={{ width: '100%', padding: '12px', background: savingSettings ? theme.textSecondary : theme.accent, color: 'white', border: 'none', borderRadius: '6px', cursor: savingSettings ? 'not-allowed' : 'pointer', fontWeight: 600, fontSize: '14px' }}>
                      {savingSettings ? 'Saving...' : 'Save Settings'}
                    </button>
                  </>
                ) : (
                  <div style={{ textAlign: 'center', color: theme.textSecondary, padding: '40px' }}>
                    <p>Loading settings...</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </main>

      {selectedDoc && (
        <div style={{ position: 'fixed', right: 0, top: 0, width: '480px', height: '100vh', background: theme.bgSecondary, borderLeft: `1px solid ${theme.border}`, display: 'flex', flexDirection: 'column', zIndex: 1000, boxShadow: '-2px 0 12px rgba(0,0,0,0.08)' }}>
          <div style={{ padding: '20px', borderBottom: `1px solid ${theme.border}`, display: 'flex', justifyContent: 'space-between' }}>
            <div>
              <h2 style={{ margin: 0, fontSize: '14px', color: theme.accent }}>🧬 DE MATRIX</h2>
              <h3 style={{ margin: '4px 0 0 0', fontSize: '18px', fontWeight: 700, color: theme.text }}>{selectedDoc.fileName}</h3>
              <p style={{ margin: '2px 0 0 0', fontSize: '12px', color: theme.textSecondary }}>{selectedDoc.chunkCount} chunks</p>
            </div>
            <button onClick={() => { setSelectedDoc(null); setChatMessages([]); }} style={{ background: 'none', border: 'none', fontSize: '20px', cursor: 'pointer', color: theme.textSecondary }}>✕</button>
          </div>
          <div style={{ flex: 1, padding: '20px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {chatMessages.length === 0 ? (
              <div style={{ textAlign: 'center', color: theme.textSecondary, marginTop: '40px' }}>
                <p style={{ fontSize: '14px' }}>Ask questions about this document...</p>
              </div>
            ) : (
              chatMessages.map((msg, idx) => (
                <div key={idx} style={{ display: 'flex', justifyContent: msg.type === 'user' ? 'flex-end' : 'flex-start' }}>
                  <div style={{
                    maxWidth: '85%',
                    padding: '12px 16px',
                    borderRadius: '8px',
                    background: msg.type === 'user' ? theme.accent : msg.type === 'error' ? theme.error : theme.bgTertiary,
                    color: msg.type === 'user' ? 'white' : msg.type === 'error' ? (settings?.darkTheme ? '#ff6b6b' : '#c62828') : theme.text,
                    fontSize: '13px',
                    lineHeight: '1.4'
                  }}>
                    {msg.text}
                  </div>
                </div>
              ))
            )}
            {chatLoading && (
              <div style={{ display: 'flex', gap: '4px', padding: '12px 16px' }}>
                <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: theme.accent }}></span>
                <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: theme.accent }}></span>
                <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: theme.accent }}></span>
              </div>
            )}
          </div>
          <div style={{ display: 'flex', gap: '8px', padding: '16px 20px', borderTop: `1px solid ${theme.border}` }}>
            <input 
              type="text" 
              placeholder="Ask about this document..." 
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              onKeyPress={(e) => { if (e.key === 'Enter' && !chatLoading) handleChatSubmit(); }}
              disabled={chatLoading}
              style={{ flex: 1, padding: '10px', border: `1px solid ${theme.border}`, borderRadius: '4px', fontSize: '13px', background: theme.bg, color: theme.text }} 
            />
            <button 
              onClick={handleChatSubmit}
              disabled={chatLoading || !chatInput.trim()}
              style={{ padding: '10px 12px', background: chatLoading || !chatInput.trim() ? theme.textSecondary : theme.accent, color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 600 }}
            >
              {chatLoading ? '⏳' : '→'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
