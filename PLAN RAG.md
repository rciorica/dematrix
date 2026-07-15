Enterprise Document Analyzer - MVP RAG Implementation Plan
Objective
Build a functional Retrieval-Augmented Generation (RAG) MVP that allows users to:
Upload PDF documents
Process and index document content
Ask natural language questions
Receive grounded answers with citations
The focus is rapid delivery, simplicity, and validation of business value while keeping the existing technology stack unchanged wherever possible.
---
MVP Architecture

React Frontend
        ↓
Spring Boot Backend
        ↓
PostgreSQL (Metadata)
        ↓
Voyage AI (Embeddings)
        ↓
ChromaDB (Vector Search)
        ↓
Ollama Mistral (Answer Generation)

---
Technology Stack
Frontend
React
Existing UI and API contracts remain unchanged
Backend
Spring Boot
Existing REST APIs remain unchanged
Database
PostgreSQL
Used for:
Documents
Chunks
Processing status
Optional chat history
Embeddings
Voyage AI
Model: `voyage-3`
Vector Store
ChromaDB
LLM
Ollama
Mistral
File Storage
Local filesystem
---
Simplified Data Model
PostgreSQL
Documents

id
filename
status
uploaded_at

Chunks

id
document_id
chunk_text
chunk_index

---
ChromaDB
Store:
json
{
  "chunkId": "uuid",
  "embedding": [...],
  "chunkText": "text",
  "metadata": {
    "documentId": "123",
    "documentName": "Policy.pdf",
    "chunkIndex": 15
  }
}

Important MVP Decision
Do not store embeddings in PostgreSQL.
Embeddings should live only in ChromaDB to avoid:
Data duplication
Synchronization issues
Stale vector problems
Increased storage consumption
---
Document Ingestion Flow
Keep ingestion synchronous for the MVP.

Upload PDF
     ↓
Extract Text
     ↓
Chunk Text
     ↓
Generate Voyage Embeddings
     ↓
Store in ChromaDB
     ↓
Ready for Search

Why Synchronous?
For typical MVP document sizes (10–100 pages), synchronous processing is easier to:
Build
Debug
Operate
Demonstrate
Background queues can be introduced later.
---
Chunking Strategy
Use simple fixed-size chunking.
Recommended:

500–1000 characters
100–150 character overlap

OR

700–1200 tokens
20% overlap

Avoid advanced chunking approaches during the MVP phase:
Semantic chunking
LLM chunking
Agent-based chunking
---
Retrieval Strategy
Use a simple retrieval pipeline.

User Question
       ↓
Voyage Embedding
       ↓
ChromaDB Top-5 Search
       ↓
Context Assembly
       ↓
Mistral Generation
       ↓
Answer + Citations

Top-K
Recommended:

Top 5 chunks

This is typically sufficient for an MVP and easy to tune later.
---
Context Expansion
Implement a lightweight context expansion strategy.
Example:

Retrieved:
Chunk 20

Include:
Chunk 19
Chunk 20
Chunk 21

Benefits:
Better context continuity
Improved answer quality
Minimal implementation effort
---
Prompt Strategy
Use a strict grounding prompt.

You are an Enterprise Document Assistant.

Answer only from the provided context.

If the answer is not contained in the context,
say:

"I could not find that information in the uploaded documents."

Provide citations for every answer.

Context:
{retrieved_chunks}

Question:
{user_question}

Goals:
Reduce hallucinations
Improve trustworthiness
Encourage source attribution
---
Citations
Citations are a core MVP feature.
Example response:
json
{
  "answer": "Annual leave entitlement is 25 days.",
  "sources": [
    {
      "document": "EmployeeHandbook.pdf",
      "chunk": 14
    }
  ]
}

Benefits:
Increased user trust
Easier validation
Better auditability
---
Error Handling
Avoid silent fallbacks.
Embeddings Failure
Instead of:

Mock embeddings

Use:

Document Status = FAILED

Example:
json
{
  "status": "FAILED",
  "reason": "Embedding service unavailable"
}

LLM Failure
Return:
json
{
  "status": "SERVICE_UNAVAILABLE",
  "message": "LLM service unavailable"
}

Fail clearly rather than returning misleading results.
---
Logging Requirements
Add structured logs for:

Document upload
Chunk generation
Embedding creation
ChromaDB inserts
Search requests
Retrieved chunks
LLM response latency
Errors and retries

This provides sufficient visibility for an MVP.
---
Features Deferred Until Later
The following are intentionally excluded from the MVP:
Retrieval Enhancements
Hybrid search
Reranking
Metadata filtering
Infrastructure Enhancements
Redis caching
Background queues
Kafka
RabbitMQ
Scalability
Multi-tenant support
Distributed processing
Batch embedding pipeline
Observability
Prometheus
Grafana
Advanced metrics dashboards
These should be introduced only after validating user adoption and business value.
---
MVP Roadmap
Phase 1 – Core RAG
Deliver:

PDF Upload
Text Extraction
Chunking
Voyage Embeddings
ChromaDB Storage
Mistral Answers
Citations

Goal:

Working end-to-end document Q&A system

---
Phase 2 – Quality Improvements
Deliver:

Neighbor chunk expansion
Prompt improvements
Conversation history

Goal:

Improve answer relevance and user experience

---
Phase 3 – Production Readiness
Deliver:

Async ingestion
Retry mechanisms
Health checks
Basic metrics

Goal:

Improve reliability and operational support

---
Phase 4 – Advanced Retrieval
Deliver:

Hybrid search
Reranking
Caching
Multi-tenancy

Goal:

Improve retrieval accuracy and scalability

---
Success Criteria
Functional
User can upload a PDF
Chunks are generated successfully
Embeddings are stored in ChromaDB
Questions return relevant answers
Citations are included in responses
Technical
Backend starts successfully
ChromaDB reachable
Voyage API reachable
Ollama/Mistral reachable
No critical runtime errors
Business
Users can find information faster than manual document review
Answers are grounded in uploaded content
Citations allow validation of responses
---
Recommended MVP Stack Summary

React
   ↓
Spring Boot
   ↓
PostgreSQL
   ↓
Voyage AI Embeddings
   ↓
ChromaDB
   ↓
Ollama Mistral

End-to-End Flow

PDF
 ↓
Chunk
 ↓
Voyage Embeddings
 ↓
ChromaDB
 ↓
Question
 ↓
Top 5 Chunks
 ↓
Prompt Assembly
 ↓
Mistral
 ↓
Answer + Citations

This architecture provides the fastest path to a usable MVP while preserving the existing stack and minimizing implementation complexity.