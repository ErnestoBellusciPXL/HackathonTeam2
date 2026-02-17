#!/bin/sh
# wait-for-db.sh
set -e

host="db"
port=3306
elapsed=0

echo "Waiting for MySQL at $host:$port..."

while ! nc -z $host $port; do
  sleep 2
  elapsed=$((elapsed + 2))
done

echo "Starting backend..."
exec java -jar /app/app.jar