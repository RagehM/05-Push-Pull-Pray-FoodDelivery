#!/bin/bash
set -e

echo "Running enum script..."
(python3 /automation/enumCreateScript.py) &

(docker-entrypoint.sh postgres) 
