import React from 'react';
import './DocumentFeed.css';

export const DocumentFeed = ({ documents, onSelectDoc }) => {
  console.log('DocumentFeed rendering with documents:', documents);
  
  if (!documents || documents.length === 0) {
    console.log('No documents to display');
    return <div style={{ color: '#999' }}>No documents</div>;
  }

  return (
    <div className="document-feed">
      {documents.map((doc, index) => {
        console.log('Rendering card for doc:', doc.fileName, 'index:', index);
        return (
          <div
            key={doc.documentId || index}
            className="feed-card"
            onClick={() => {
              console.log('Card clicked:', doc.fileName);
              onSelectDoc(doc);
            }}
          >
            <div className="card-header">
              <div className="card-meta">
                <span className="time-ago">Recently analyzed</span>
                <span className="detector">AI ANALYZED</span>
              </div>
              <div className="confidence-badge">
                ● 92% Conf
              </div>
            </div>

            <h3 className="card-title">{doc.fileName}</h3>
            <p className="card-description">
              Semantic analysis of {doc.chunkCount} text chunks extracted from document.
              Key entities and relationships identified.
            </p>

            <div className="card-tags">
              <span className="tag">Document Analysis</span>
              <span className="tag">Entity Extraction</span>
            </div>

            <div className="card-footer">
              <span className="chunk-count">📊 {doc.chunkCount} chunks</span>
              <span className="status-badge">{doc.status}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
};
