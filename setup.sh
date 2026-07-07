#!/bin/bash

echo "🚀 Enterprise Document Analyzer - Setup Script"
echo "=============================================="

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

echo "✅ Docker found"

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

echo "✅ Docker Compose found"

# Create .env if not exists
if [ ! -f .env ]; then
    echo "📝 Creating .env file from template..."
    cp .env.example .env
    echo "✅ .env file created. Update with your settings."
else
    echo "✅ .env file exists"
fi

# Create uploads directory
mkdir -p uploads
echo "✅ Uploads directory ready"

# Build images
echo ""
echo "🏗️  Building Docker images..."
docker compose build

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build successful!"
    echo ""
    echo "🎯 Next: Run 'docker compose up' to start services"
    echo ""
    echo "📍 Access points:"
    echo "   Frontend:   http://localhost:3000"
    echo "   Backend:    http://localhost:8080/api"
    echo "   Chroma:     http://localhost:8001"
    echo "   Health:     http://localhost:8080/api/health"
else
    echo "❌ Build failed"
    exit 1
fi
