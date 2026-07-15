@echo off
REM Enterprise Document Analyzer - Quick Start Script
REM This script starts all services for the RAG application

echo.
echo ========================================
echo Enterprise Document Analyzer - Starting
echo ========================================
echo.

REM Check if Docker is running
docker ps >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo Checking for required services...

REM Pull latest images
echo.
echo [1/5] Pulling latest images...
docker pull eclipse-temurin:21-jre-alpine
docker pull postgres:16-alpine
docker pull ghcr.io/chroma-core/chroma:latest
docker pull ollama/ollama:latest

REM Build backend image
echo.
echo [2/5] Building backend image...
docker build -t enterprise-doc-analyzer:latest . --no-cache
if errorlevel 1 (
    echo ERROR: Failed to build backend image
    pause
    exit /b 1
)

REM Start containers
echo.
echo [3/5] Starting containers...
docker compose up -d
if errorlevel 1 (
    echo ERROR: Failed to start containers
    pause
    exit /b 1
)

REM Wait for services to be ready
echo.
echo [4/5] Waiting for services to be ready (30 seconds)...
timeout /t 30 /nobreak

REM Check health
echo.
echo [5/5] Checking service health...

:check_health
docker ps --filter "name=doc-analyzer" --format "table {{.Names}}\t{{.Status}}" >services.tmp
if errorlevel 1 goto health_error

echo.
echo Service Status:
type services.tmp
del services.tmp

echo.
echo ========================================
echo SUCCESS! Application is running:
echo ========================================
echo.
echo Frontend:  http://localhost:3000
echo Backend:   http://localhost:8080
echo Chroma:    http://localhost:8001
echo Ollama:    http://localhost:11434
echo.
echo Run "docker compose logs -f" to see live logs
echo Run "docker compose down -v" to stop and remove everything
echo.
pause
exit /b 0

:health_error
echo.
echo WARNING: Could not fully verify services
echo.
echo Frontend:  http://localhost:3000
echo Backend:   http://localhost:8080
echo Chroma:    http://localhost:8001
echo Ollama:    http://localhost:11434
echo.
echo Check logs with: docker compose logs
echo.
pause
