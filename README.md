# Enterprise Document Analyzer

Enterprise-grade RAG (Retrieval-Augmented Generation) platform for intelligent document analysis.

## Architecture

```
Backend (Spring Boot 3) ──> PostgreSQL Database
                       └─> Vector Store (Chroma)
                       └> LLM Service (OpenAI/Ollama)

Frontend (React 18)  ──> WebFlux SSE Streaming
```

## Quick Start

### Using Docker Compose

```bash
# Copy environment file
cp .env.example .env

# Build and run
docker compose up --build

# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# API Docs: http://localhost:8080/api/swagger-ui.html
```

### Local Development

#### Backend
```bash
# Prerequisites: Java 21, Maven 3.9+

mvn clean install
mvn spring-boot:run

# Runs on http://localhost:8080
```

#### Frontend
```bash
# Prerequisites: Node 20+

cd frontend
npm install
npm start

# Runs on http://localhost:3000
```

## API Endpoints

### Document Ingestion
- `POST /api/documents/upload` - Upload and process documents

### Chat & RAG
- `POST /api/chat/stream` - Stream chat responses with citations (Server-Sent Events)

### Health
- `GET /api/health` - Service health check

## Features

- **Smart Document Chunking**: Semantic-aware text splitting with overlap
- **Vector Embeddings**: Generate and manage embeddings for document chunks
- **Semantic Search**: Find relevant contexts using cosine similarity
- **RAG Pipeline**: Inject retrieved contexts into LLM prompts
- **Streaming Responses**: Real-time chat with SSE streaming
- **Citation Tracking**: Track and display document sources with relevance scores
- **Table Detection**: Identify and flag table data for special handling

## Technology Stack

**Backend:**
- Spring Boot 3.2 (WebFlux for async)
- Jakarta Persistence (JPA/Hibernate)
- PostgreSQL + H2 (dev)
- Apache PDFBox for PDF parsing
- Custom vector embeddings

**Frontend:**
- React 18
- Modern hooks (useServerSentEvents, useDocumentUpload)
- Responsive CSS Grid layout
- Citation badging with metadata

**Infrastructure:**
- Docker & Docker Compose
- Chroma for vector storage
- PostgreSQL for persistent data

## Configuration

Edit `src/main/resources/application.yml` to customize:

```yaml
app:
  chunking:
    chunk-size: 1000
    chunk-overlap: 100
    min-chunk-size: 100
  
  search:
    top-k: 5
    similarity-threshold: 0.7
  
  llm:
    provider: openai  # or 'mock'
    model: gpt-4-turbo-preview
    temperature: 0.7
```

## Project Structure

```
src/main/java/com/docanalysis/
├── domain/              # JPA entities (Document, DocumentChunk, EmbeddingVector)
├── controller/          # REST endpoints
├── service/             # Business logic (RAG, embedding, search)
├── repository/          # Data access layer
├── dto/                 # Transfer objects
├── config/              # Spring configuration
└── exception/           # Exception handling

frontend/src/
├── components/          # React UI components
├── hooks/              # Custom React hooks
├── services/           # API client services
└── App.jsx             # Main app
```

## Key Services

### DocumentProcessingService
- Orchestrates document upload, text extraction, and chunking
- Generates embeddings for all chunks
- Persists metadata in PostgreSQL

### VectorEmbeddingService
- Generates dense vector representations
- Integrates with embedding models
- Currently uses mock embeddings (replace with OpenAI/Ollama)

### VectorRepositoryService
- Performs cosine similarity searches
- Returns top-K most relevant chunks
- Scores each result

### RAGGenerationService
- Builds augmented prompts with retrieved contexts
- Streams LLM completions to client
- Tracks and serializes citations

## Development Notes

### Adding New LLM Provider
Edit `LLMService.streamCompletion()`:
```java
if ("openai".equals(provider)) {
    return streamFromOpenAI(prompt);
}
```

### Extending Chunking Strategy
Modify `DocumentProcessingService.performSmartChunking()` to add:
- Sentence-level splitting
- Semantic cohesion detection
- Table-aware boundaries

### Custom Embedding Models
Replace mock embeddings in `VectorEmbeddingService.generateMockEmbedding()` with:
- Ollama local endpoint
- Hugging Face transformers
- OpenAI embedding API

## Testing

```bash
# Backend unit tests
mvn test

# Backend integration tests
mvn verify

# Frontend tests
cd frontend && npm test
```

## Deployment

### Production Checklist

- [ ] Use PostgreSQL (not H2)
- [ ] Set `OPENAI_API_KEY` in environment
- [ ] Configure `app.storage.upload-dir` for persistent storage
- [ ] Enable HTTPS on reverse proxy
- [ ] Set up monitoring/logging aggregation
- [ ] Use managed vector database (Pinecone, Weaviate cloud)
- [ ] Implement rate limiting and auth middleware

## License

Proprietary - Enterprise Document Analyzer
