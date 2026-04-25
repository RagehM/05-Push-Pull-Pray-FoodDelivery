#!/bin/sh

HOOK_DIR=".githooks"

if [ ! -d "$HOOK_DIR" ]; then
  echo "❌ $HOOK_DIR folder not found."
  exit 1
fi

git config core.hooksPath "$HOOK_DIR"

chmod +x "$HOOK_DIR"/* || exit 1

echo "✅ Shared Git hooks enabled."