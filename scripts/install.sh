#!/usr/bin/env bash
#
# AgentEval - Local Build & Install Script (Linux / macOS)
#
# Builds all modules and installs them to your local Maven repository (~/.m2/repository).
# Requires: Java 21+
#
# Usage:
#   ./scripts/install.sh [OPTIONS]
#
# Options:
#   --with-tests     Run tests during the build (default: tests are skipped)
#   --skip-javadoc   Skip Javadoc generation for faster builds
#   --help           Show this help message
#

set -euo pipefail

# ── Resolve project root (parent of scripts/) ──────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# ── Defaults ────────────────────────────────────────────────────────────────
SKIP_TESTS=true
SKIP_JAVADOC=false

# ── Parse arguments ─────────────────────────────────────────────────────────
usage() {
    cat <<EOF
AgentEval - Local Build & Install

Builds all modules and installs them to your local Maven repository (~/.m2/repository).
Requires: Java 21+

Usage:
  ./scripts/install.sh [OPTIONS]

Options:
  --with-tests     Run tests during the build (default: tests are skipped)
  --skip-javadoc   Skip Javadoc generation for faster builds
  --help           Show this help message

Examples:
  ./scripts/install.sh                     # Quick install (skip tests & javadoc)
  ./scripts/install.sh --with-tests        # Install with tests
  ./scripts/install.sh --skip-javadoc      # Skip javadoc generation
EOF
    exit 0
}

for arg in "$@"; do
    case "$arg" in
        --with-tests)   SKIP_TESTS=false ;;
        --skip-javadoc) SKIP_JAVADOC=true ;;
        --help)         usage ;;
        *)
            echo "Unknown option: $arg"
            echo "Run './scripts/install.sh --help' for usage."
            exit 1
            ;;
    esac
done

# ── Check Java ──────────────────────────────────────────────────────────────
if ! command -v java &>/dev/null; then
    echo "Error: Java is not installed or not on PATH."
    echo "AgentEval requires Java 21 or later."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n1 | sed -E 's/.*"([0-9]+).*/\1/')
if [ "$JAVA_VERSION" -lt 21 ] 2>/dev/null; then
    echo "Error: Java 21+ is required (found Java $JAVA_VERSION)."
    exit 1
fi

# ── Build Maven arguments ──────────────────────────────────────────────────
MVN_ARGS="clean install"

if [ "$SKIP_TESTS" = true ]; then
    MVN_ARGS="$MVN_ARGS -DskipTests"
fi

if [ "$SKIP_JAVADOC" = true ]; then
    MVN_ARGS="$MVN_ARGS -Dmaven.javadoc.skip=true"
fi

# ── Run build ───────────────────────────────────────────────────────────────
echo "========================================"
echo " AgentEval - Local Install"
echo "========================================"
echo " Java version : $JAVA_VERSION"
echo " Skip tests   : $SKIP_TESTS"
echo " Skip javadoc : $SKIP_JAVADOC"
echo " Project root : $PROJECT_ROOT"
echo "========================================"
echo ""

cd "$PROJECT_ROOT"

# Use the Maven wrapper (no Maven installation required)
if [ ! -f "./mvnw" ]; then
    echo "Error: Maven wrapper (mvnw) not found in project root."
    exit 1
fi

chmod +x ./mvnw
./mvnw $MVN_ARGS

echo ""
echo "========================================"
echo " Install complete!"
echo " Artifacts are in: ~/.m2/repository/org/byteveda/agenteval/"
echo "========================================"
