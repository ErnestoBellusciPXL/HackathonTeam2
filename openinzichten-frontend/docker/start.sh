#!/bin/sh

# Default backend URL (can be overridden by environment)
: "${BACKEND_HOST:=http://backend:8080}"

echo "Checking backend availability at $BACKEND_HOST ..."

# Extract host and port from BACKEND_HOST
HOST=$(echo "$BACKEND_HOST" | sed -E 's#https?://([^:/]+).*#\1#')
PORT=$(echo "$BACKEND_HOST" | sed -E 's#.*:([0-9]+).*#\1#')
[ -z "$PORT" ] && PORT=80

# Retry loop for backend reachability
for i in $(seq 1 15); do
  if nc -z "$HOST" "$PORT" 2>/dev/null; then
    echo "✅ Backend is reachable!"
    break
  fi
  echo "Waiting for backend... ($i/15)"
  sleep 2
done

# If not reachable after 15 tries, warn but continue
if [ $i -eq 15 ]; then
  echo "⚠️ Backend did not respond after 30s, starting Nginx anyway."
fi

# Substitute environment vars and start Nginx
envsubst '$BACKEND_HOST' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf
nginx -g 'daemon off;'
