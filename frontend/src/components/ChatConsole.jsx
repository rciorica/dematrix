import React, { useState, useEffect, useRef } from 'react';
import { useServerSentEvents } from '../hooks/useServerSentEvents';
import { CitationBadge } from './CitationBadge';
import './ChatConsole.css';

export const ChatConsole = ({ document, onClose }) => {
  const [query, setQuery] = useState('');
  const [conversationId] = useState(`conv-${Date.now()}`);
  const [fullResponse, setFullResponse] = useState('');
  const { streamChatResponse, messages, isConnected, error } = useServerSentEvents();
  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    const chunks = messages
      .filter((msg) => msg.chunk && !msg.isFinished)
      .map((msg) => msg.chunk)
      .join('');
    setFullResponse(chunks);
  }, [messages]);

  const handleSendQuery = async (e) => {
    e.preventDefault();
    if (!query.trim() || isConnected) return;

    setFullResponse('');
    await streamChatResponse(query, conversationId);
    setQuery('');
  };

  const citations = messages.find((msg) => msg.isFinished)?.citations || [];

  return (
    <div className="chat-console">
      <div className="console-header">
        <div className="console-title">
          <h2>🧬 DE MATRIX</h2>
          <h3>{document.fileName}</h3>
          <p className="document-desc">{document.chunkCount} chunks • {document.status}</p>
        </div>
        <button className="close-btn" onClick={onClose}>✕</button>
      </div>

      <div className="console-body">
        <div className="section-header">
          <h4>Analysis & Reasoning</h4>
        </div>

        {fullResponse && (
          <div className="response-section">
            <div className="response-text">
              <p>{fullResponse}</p>
            </div>

            {citations.length > 0 && (
              <div className="citations-section">
                <h5>Extracted Evidence</h5>
                <div className="citations-grid">
                  {citations.map((citation) => (
                    <CitationBadge key={citation.chunkId} citation={citation} />
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {isConnected && (
          <div className="response-section streaming">
            <span className="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </span>
            <p>Analyzing document...</p>
          </div>
        )}

        {error && (
          <div className="response-section error">
            <p>⚠️ Error: {error}</p>
          </div>
        )}

        {!fullResponse && !isConnected && !error && (
          <div className="response-section">
            <p style={{ color: '#999', fontSize: '13px' }}>Ask a question to begin analysis...</p>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      <form onSubmit={handleSendQuery} className="console-input">
        <input
          type="text"
          placeholder="Ask about this document..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          disabled={isConnected}
          autoFocus
        />
        <button type="submit" disabled={isConnected || !query.trim()}>
          {isConnected ? '⏳' : '→'}
        </button>
      </form>
    </div>
  );
};
