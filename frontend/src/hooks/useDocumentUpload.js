import { useState, useCallback } from 'react';

export const useDocumentUpload = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState(null);

  const uploadDocument = useCallback(async (file, title) => {
    setIsLoading(true);
    setError(null);
    setUploadProgress(0);

    try {
      const formData = new FormData();
      formData.append('file', file);
      if (title) {
        formData.append('title', title);
      }

      // Log the request
      const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
      const uploadUrl = `${apiUrl}/documents/upload`;
      console.log('Uploading to:', uploadUrl);
      console.log('File:', file.name, file.size);

      const xhr = new XMLHttpRequest();

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          const percentComplete = (e.loaded / e.total) * 100;
          setUploadProgress(percentComplete);
        }
      });

      return new Promise((resolve, reject) => {
        xhr.addEventListener('load', () => {
          console.log('Response status:', xhr.status);
          console.log('Response headers:', xhr.getAllResponseHeaders());
          console.log('Response text length:', xhr.responseText.length);
          console.log('Response text (first 500):', xhr.responseText.substring(0, 500));

          try {
            if (xhr.status === 200 || xhr.status === 201) {
              const response = JSON.parse(xhr.responseText);
              console.log('Parsed response:', response);
              resolve(response);
            } else {
              const errorMsg = `Upload failed with status ${xhr.status}: ${xhr.responseText.substring(0, 100)}`;
              console.error(errorMsg);
              reject(new Error(errorMsg));
            }
          } catch (parseError) {
            const errorMsg = `Failed to parse JSON: ${parseError.message}`;
            console.error(errorMsg, xhr.responseText.substring(0, 200));
            reject(new Error(errorMsg));
          }
        });

        xhr.addEventListener('error', () => {
          console.error('XHR Network error');
          reject(new Error('Upload failed - network error'));
        });

        xhr.addEventListener('abort', () => {
          console.log('XHR Aborted');
          reject(new Error('Upload cancelled'));
        });

        xhr.open('POST', uploadUrl);
        console.log('Sending XHR...');
        xhr.send(formData);
      });
    } catch (err) {
      console.error('Upload catch error:', err);
      setError(err.message);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { uploadDocument, isLoading, uploadProgress, error };
};
