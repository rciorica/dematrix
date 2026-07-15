import { useState, useCallback, useRef } from 'react';

export const useServerSentEvents = () => {
  const [messages, setMessages] = useState([]);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState(null);
  const eventSourceRef = useRef(null);

  const handleSseLine = useCallback((line) => {
    const trimmedLine = line.trim();
    if (!trimmedLine.startsWith('data:')) {
      return;
    }

    try {
      const data = JSON.parse(trimmedLine.slice(5).trim());
      setMessages((prev) => [...prev, data]);
    } catch (e) {
      console.error('Failed to parse SSE message:', e);
    }
  }, []);

  const streamChatResponse = useCallback(async (query, conversationId) => {
    setError(null);
    setIsConnected(true);
    setMessages([]);

    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL}/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          query,
          conversationId,
          contextLimit: 5,
        }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const contentType = response.headers.get('content-type') || '';
      if (contentType.includes('application/json')) {
        const data = await response.json();
        setMessages([{
          ...data,
          isFinished: data.isFinished ?? true,
        }]);
        return;
      }

      if (!response.body) {
        throw new Error('Streaming response body is not available');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop();

        for (const line of lines) {
          handleSseLine(line);
        }
      }

      if (buffer.trim()) {
        handleSseLine(buffer);
      }
    } catch (err) {
      setError(err.message);
      console.error('SSE Error:', err);
    } finally {
      setIsConnected(false);
    }
  }, [handleSseLine]);

  const closeConnection = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      setIsConnected(false);
    }
  }, []);

  return {
    streamChatResponse,
    messages,
    isConnected,
    error,
    closeConnection,
  };
};
