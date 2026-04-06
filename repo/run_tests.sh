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
    mvn test
  )
fi

if [ -f "$PROJECT_DIR/app/frontend/package.json" ]; then
  echo "Running frontend tests..."
  (
    cd "$PROJECT_DIR/app/frontend"
    if [ ! -d node_modules ]; then
      echo "Installing frontend dependencies..."
      npm install
    fi
    npm test -- --watch=false --browsers=ChromeHeadless
  )
fi

echo "All tests completed successfully."
