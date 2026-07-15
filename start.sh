#!/bin/bash
# Enterprise Document Analyzer - Quick Start Script
# This script starts all services for the RAG application

set -e

echo ""
echo "========================================"
echo "Enterprise Document Analyzer - Starting"
echo "========================================"
echo ""

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "ERROR: Docker is not running!"
    echo "Please start Docker and try again."
    exit 1
fi

echo "Checking for required services..."

# Pull latest images
echo ""
echo "[1/5] Pulling latest images..."
docker pull eclipse-temurin:21-jre-alpine
docker pull postgres:16-alpine
docker pull ghcr.io/chroma-core/chroma:latest
docker pull ollama/ollama:latest

# Build backend image
echo ""
echo "[2/5] Building backend image..."
if ! docker build -t enterprise-doc-analyzer:latest . --no-cache; then
    echo "ERROR: Failed to build backend image"
    exit 1
fi

# Start containers
echo ""
echo "[3/5] Starting containers..."
if ! docker compose up -d; then
    echo "ERROR: Failed to start containers"
    exit 1
fi

# Wait for services to be ready
echo ""
echo "[4/5] Waiting for services to be ready (30 seconds)..."
sleep 30

# Check health
echo ""
echo "[5/5] Checking service health..."

docker ps --filter "name=doc-analyzer" --format "table {{.Names}}\t{{.Status}}"

echo ""
echo "========================================"
echo "SUCCESS! Application is running:"
echo "========================================"
echo ""
echo "Frontend:  http://localhost:3000"
echo "Backend:   http://localhost:8080"
echo "Chroma:    http://localhost:8001"
echo "Ollama:    http://localhost:11434"
echo ""
echo "Run 'docker compose logs -f' to see live logs"
echo "Run 'docker compose down -v' to stop and remove everything"
echo ""
