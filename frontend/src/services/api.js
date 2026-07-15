// API Configuration
// Use relative URLs so nginx proxy handles routing
export const API_BASE = '/api';

export const api = {
  analytics: {
    entities: `${API_BASE}/analytics/entities`,
    stats: `${API_BASE}/analytics/stats`,
  },
  settings: `${API_BASE}/settings`,
  documents: {
    list: `${API_BASE}/documents/list`,
    upload: `${API_BASE}/documents/upload`,
  },
  chat: {
    stream: `${API_BASE}/chat/stream`,
    health: `${API_BASE}/chat/health`,
  },
};

export default API_BASE;
