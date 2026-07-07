import React, { useState, useRef } from 'react';
import { useDocumentUpload } from '../hooks/useDocumentUpload';
import './DocumentDropzone.css';

export const DocumentDropzone = ({ onDocumentProcessed }) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const [documentTitle, setDocumentTitle] = useState('');
  const { uploadDocument, isLoading, uploadProgress, error } = useDocumentUpload();
  const fileInputRef = useRef(null);

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleFileUpload(files[0]);
    }
  };

  const handleFileChange = (e) => {
    const files = e.target.files;
    if (files.length > 0) {
      handleFileUpload(files[0]);
    }
  };

  const handleFileUpload = async (file) => {
    try {
      console.log('Starting upload for file:', file.name);
      const response = await uploadDocument(file, documentTitle);
      console.log('Upload successful:', response);
      onDocumentProcessed(response);
      setDocumentTitle('');
      // Reset file input
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (err) {
      console.error('Upload error:', err);
    }
  };

  const handleClickDropzone = () => {
    console.log('Dropzone clicked, opening file picker');
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  return (
    <div className="dropzone-container">
      <div
        className={`dropzone ${isDragOver ? 'drag-over' : ''} ${isLoading ? 'loading' : ''}`}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={handleClickDropzone}
        role="button"
        tabIndex="0"
      >
        {isLoading ? (
          <div className="upload-progress">
            <div className="spinner"></div>
            <p>Uploading... {Math.round(uploadProgress)}%</p>
          </div>
        ) : (
          <>
            <svg className="upload-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
            <h3>Drop your document here</h3>
            <p>or click to browse</p>
          </>
        )}
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileChange}
          accept=".pdf,.txt"
          className="file-input"
          disabled={isLoading}
          aria-label="Upload document"
        />
      </div>

      <div className="document-title">
        <input
          type="text"
          placeholder="Document title (optional)"
          value={documentTitle}
          onChange={(e) => setDocumentTitle(e.target.value)}
          disabled={isLoading}
        />
      </div>

      {error && <div className="error-message">{error}</div>}
    </div>
  );
};
