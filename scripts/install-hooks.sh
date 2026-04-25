#!/bin/sh

HOOK_DIR=".githooks"

if [ ! -d "$HOOK_DIR" ]; then
  echo "❌ Hook directory not found: $HOOK_DIR"
  exit 1
fi

git config core.hooksPath "$HOOK_DIR"

echo "✅ Git hooks enabled successfully."
echo "ℹ️ If you're on Windows, run this from Git Bash or WSL."