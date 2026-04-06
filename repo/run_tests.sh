#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -d "$ROOT_DIR/repo/app/backend" ] && [ -d "$ROOT_DIR/repo/app/frontend" ]; then
  PROJECT_DIR="$ROOT_DIR/repo"
elif [ -d "$ROOT_DIR/app/backend" ] && [ -d "$ROOT_DIR/app/frontend" ]; then
  PROJECT_DIR="$ROOT_DIR"
else
  echo "Could not locate project directories from $ROOT_DIR" >&2
  exit 1
fi

echo "Using project directory: $PROJECT_DIR"

if [ -d "$PROJECT_DIR/app/backend" ]; then
  echo "Running backend tests..."
  (
    cd "$PROJECT_DIR/app/backend"
    if command -v mvn >/dev/null 2>&1; then
      mvn test
    elif [ -x "./mvnw" ]; then
      ./mvnw test
    elif command -v docker >/dev/null 2>&1; then
      echo "mvn not found; running backend tests in Docker Maven..."
      HOST_PWD="$PWD"
      if pwd -W >/dev/null 2>&1; then
        HOST_PWD="$(pwd -W)"
      fi
      MSYS_NO_PATHCONV=1 docker run --rm -v "${HOST_PWD}:/workspace" -w /workspace maven:3.9.8-eclipse-temurin-17 mvn test
    else
      echo "Backend tests require Maven (mvn/mvnw) or Docker, but none were found." >&2
      exit 1
    fi
  )
fi

if [ -f "$PROJECT_DIR/app/frontend/package.json" ]; then
  echo "Running frontend tests..."
  (
    cd "$PROJECT_DIR/app/frontend"
    echo "Installing frontend dependencies..."
    npm install --include=dev --no-audit --no-fund

    # Defensive install for transient npm dependency resolution issues in CI.
    if [ ! -d "node_modules/is-fullwidth-code-point" ]; then
      npm install is-fullwidth-code-point --no-save --no-audit --no-fund
    fi

    npm test
  )
fi

echo "All tests completed successfully."
