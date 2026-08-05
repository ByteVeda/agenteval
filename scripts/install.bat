@echo off
REM ──────────────────────────────────────────────────────────────────────────
REM AgentEval - Local Build & Install Script (Windows)
REM
REM Builds all modules and installs them to your local Maven repository.
REM Requires: Java 21+
REM
REM Usage:
REM   scripts\install.bat [OPTIONS]
REM
REM Options:
REM   /with-tests     Run tests during the build (default: tests are skipped)
REM   /skip-javadoc   Skip Javadoc generation for faster builds
REM   /help           Show this help message
REM ──────────────────────────────────────────────────────────────────────────

setlocal enabledelayedexpansion

REM ── Resolve project root (parent of scripts\) ────────────────────────────
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.."
set "PROJECT_ROOT=%cd%"
popd

REM ── Defaults ──────────────────────────────────────────────────────────────
set "SKIP_TESTS=true"
set "SKIP_JAVADOC=false"

REM ── Parse arguments ───────────────────────────────────────────────────────
:parse_args
if "%~1"=="" goto :check_java
if /I "%~1"=="/with-tests" (
    set "SKIP_TESTS=false"
    shift
    goto :parse_args
)
if /I "%~1"=="/skip-javadoc" (
    set "SKIP_JAVADOC=true"
    shift
    goto :parse_args
)
if /I "%~1"=="/help" goto :usage
if /I "%~1"=="--help" goto :usage
echo Unknown option: %~1
echo Run 'scripts\install.bat /help' for usage.
exit /b 1

:usage
echo.
echo AgentEval - Local Build ^& Install
echo.
echo Builds all modules and installs them to your local Maven repository.
echo Requires: Java 21+
echo.
echo Usage:
echo   scripts\install.bat [OPTIONS]
echo.
echo Options:
echo   /with-tests     Run tests during the build (default: tests are skipped)
echo   /skip-javadoc   Skip Javadoc generation for faster builds
echo   /help           Show this help message
echo.
echo Examples:
echo   scripts\install.bat                    Quick install (skip tests)
echo   scripts\install.bat /with-tests        Install with tests
echo   scripts\install.bat /skip-javadoc      Skip javadoc generation
exit /b 0

REM ── Check Java ────────────────────────────────────────────────────────────
:check_java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not on PATH.
    echo AgentEval requires Java 21 or later.
    exit /b 1
)

for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VER_RAW=%%~i"
)
for /f "tokens=1 delims=." %%a in ("!JAVA_VER_RAW!") do set "JAVA_VERSION=%%a"

if !JAVA_VERSION! lss 21 (
    echo Error: Java 21+ is required (found Java !JAVA_VERSION!).
    exit /b 1
)

REM ── Build Maven arguments ─────────────────────────────────────────────────
set "MVN_ARGS=clean install"

if "!SKIP_TESTS!"=="true" (
    set "MVN_ARGS=!MVN_ARGS! -DskipTests"
)

if "!SKIP_JAVADOC!"=="true" (
    set "MVN_ARGS=!MVN_ARGS! -Dmaven.javadoc.skip=true"
)

REM ── Run build ─────────────────────────────────────────────────────────────
echo ========================================
echo  AgentEval - Local Install
echo ========================================
echo  Java version : !JAVA_VERSION!
echo  Skip tests   : !SKIP_TESTS!
echo  Skip javadoc : !SKIP_JAVADOC!
echo  Project root : %PROJECT_ROOT%
echo ========================================
echo.

cd /d "%PROJECT_ROOT%"

if not exist "mvnw.cmd" (
    echo Error: Maven wrapper (mvnw.cmd) not found in project root.
    exit /b 1
)

call mvnw.cmd !MVN_ARGS!

if %errorlevel% neq 0 (
    echo.
    echo Build failed. See output above for details.
    exit /b %errorlevel%
)

echo.
echo ========================================
echo  Install complete!
echo  Artifacts are in: %%USERPROFILE%%\.m2\repository\org\byteveda\agenteval\
echo ========================================

endlocal
