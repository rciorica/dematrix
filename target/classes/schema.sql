-- Create tables only if they don't exist

CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    file_path_ref VARCHAR(500) NOT NULL,
    title VARCHAR(255) NOT NULL,
    owner_id VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    file_size BIGINT NOT NULL DEFAULT 0,
    file_type VARCHAR(50),
    extracted_text TEXT,
    chunk_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(36) PRIMARY KEY,
    document_id VARCHAR(36) NOT NULL,
    chunk_text TEXT NOT NULL,
    sequence_order INT NOT NULL,
    page_number INT,
    start_offset BIGINT,
    end_offset BIGINT,
    is_table_data BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS embedding_vectors (
    id VARCHAR(36) PRIMARY KEY,
    document_chunk_id VARCHAR(36) NOT NULL,
    vector_data double precision[] NOT NULL,
    embedding_model VARCHAR(255),
    dimension INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (document_chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_owner_id ON documents(owner_id);
CREATE INDEX IF NOT EXISTS idx_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_embedding_vectors_chunk ON embedding_vectors(document_chunk_id);

CREATE TABLE IF NOT EXISTS system_settings (
    id VARCHAR(36) PRIMARY KEY,
    chunk_size INT DEFAULT 1000,
    chunk_overlap INT DEFAULT 100,
    top_k_results INT DEFAULT 5,
    similarity_threshold DOUBLE PRECISION DEFAULT 0.7
);
