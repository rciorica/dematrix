import React, { useState } from 'react';
import './CitationBadge.css';

export const CitationBadge = ({ citation }) => {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="citation-badge">
      <button
        className="citation-trigger"
        onClick={() => setIsExpanded(!isExpanded)}
        title={`Relevance: ${(citation.relevanceScore * 100).toFixed(1)}%`}
      >
        <span className="citation-doc">{citation.documentName}</span>
        {citation.pageNumber && <span className="citation-page">p. {citation.pageNumber}</span>}
        <span className="relevance-score">
          {(citation.relevanceScore * 100).toFixed(0)}%
        </span>
      </button>

      {isExpanded && (
        <div className="citation-details">
          <div className="detail-row">
            <span className="label">Document:</span>
            <span className="value">{citation.documentName}</span>
          </div>
          {citation.pageNumber && (
            <div className="detail-row">
              <span className="label">Page:</span>
              <span className="value">{citation.pageNumber}</span>
            </div>
          )}
          {citation.tableCoordinates && (
            <div className="detail-row">
              <span className="label">Type:</span>
              <span className="value">Table Data</span>
            </div>
          )}
          <div className="detail-row">
            <span className="label">Relevance:</span>
            <span className="value">{(citation.relevanceScore * 100).toFixed(1)}%</span>
          </div>
          <div className="snippet">
            <p>{citation.contentSnippet}</p>
          </div>
        </div>
      )}
    </div>
  );
};
