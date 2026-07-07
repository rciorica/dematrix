@echo off
REM Enterprise Document Analyzer - Setup Script

echo.
echo 🚀 Enterprise Document Analyzer - Setup Script
echo =============================================="
echo.

REM Check Docker
where docker >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ❌ Docker is not installed. Please install Docker Desktop first.
    pause
    exit /b 1
)

echo ✅ Docker found
echo.

REM Check Docker Compose
where docker-compose >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo ❌ Docker Compose is not installed.
    pause
    exit /b 1
)

echo ✅ Docker Compose found
echo.

REM Create .env if not exists
if not exist .env (
    echo 📝 Creating .env file from template...
    copy .env.example .env
    echo ✅ .env file created. Update with your settings.
) else (
    echo ✅ .env file exists
)

echo.

REM Create uploads directory
if not exist uploads (
    mkdir uploads
)
echo ✅ Uploads directory ready
echo.

REM Build images
echo 🏗️  Building Docker images...
echo.
docker compose build

if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ Build successful!
    echo.
    echo 🎯 Next: Run 'docker compose up' to start services
    echo.
    echo 📍 Access points:
    echo    Frontend:   http://localhost:3000
    echo    Backend:    http://localhost:8080/api
    echo    Chroma:     http://localhost:8001
    echo    Health:     http://localhost:8080/api/health
) else (
    echo.
    echo ❌ Build failed
    pause
    exit /b 1
)

pause
