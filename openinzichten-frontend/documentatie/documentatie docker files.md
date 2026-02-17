## Docker documentatie
Deze sectie beschrijft de `dockerfile` en `docker/docker-compose.yml` die gebruikt worden voor het bouwen en draaien van de frontend.
De frontend wordt gebouwd met Node.js (Vite), opgezet met Nginx.

De setup bestaat uit drie hoofdbestanden:
- `Dockerfile`: Bouwen en configureren van de webcontainer.
- `start.sh`: Opstartscript dat de backend beschikbaarheid controleert en Nginx configureert.
- `docker-compose.yml`: Opzetten van de containers.

### dockerfile

- De dockerfile gebruikt een multi-stage build:
	- Build stage: Bouwt de frontend met Node.js.
	- Runtime stage: Gebruikt Nginx voor deployment.
- Voor het starten van de frontend gebruiken we het start.sh script. Dit kijkt of de backend online is voordat we de frontend opstarten.

```
# Stage 1: Build the app
FROM node:lts-alpine AS build

WORKDIR /app

# Accept VITE_BACKEND_URL as a build argument
ARG VITE_BACKEND_URL=/api
ENV VITE_BACKEND_URL=$VITE_BACKEND_URL

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy all source files
COPY . .

# Build the app
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Install envsubst
RUN apk add --no-cache gettext

# Remove default Nginx static files
RUN rm -rf /usr/share/nginx/html/*

# Copy built app
COPY --from=build /app/dist /usr/share/nginx/html

# Copy Nginx template
COPY ./docker/default.conf.template /etc/nginx/conf.d/default.conf.template

# Copy startup script
COPY ./docker/start.sh /start.sh
RUN chmod +x /start.sh

# Expose port 80
EXPOSE 80

# Start Nginx with envsubst
CMD ["/start.sh"]
```

### start.sh script
Het `start.sh` script zorgt ervoor dat:
- De backend bereikbaar is voordat Nginx start.
- De juiste backend-URL in de Nginx-configuratie wordt ingevoegd via envsubst.
- default.conf.template wordt gebruikt voor Nginx om tijdens runtime de backend url aan te passen.

```
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

```

### docker-compose.yml
- Services:
	- `web`: Start de frontend op met externe 'proxy' netwerk.

```yaml
services:
  web:
    image: ${IMAGE_NAME}
    environment:
      BACKEND_HOST: ${BACKEND_HOST}
      FRONTEND_PORT: ${FRONTEND_PORT:-80}
      OTEL_RESOURCE_ATTRIBUTES: "deployment.environment=${ENV:-unknown}"
    labels:
      - "environment=${ENV:-unknown}"
    expose:
      - "80"
    networks:
      proxy:
    deploy:
      mode: replicated
      replicas: ${REPLICAS:-2}
      update_config:
        parallelism: 1        # Update één container tegelijk
        delay: 10s            # Wacht 10s tussen updates
        order: start-first    # Start nieuwe container voordat oude stopt
      restart_policy:
        condition: any
      placement:
        constraints: []
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

networks:
  proxy:
    external: true            # Gebruikt extern overlay netwerk
```

**Belangrijke wijzigingen:**
- Poorten worden niet meer direct exposed (`ports`), maar via `expose` binnen het netwerk
- Gebruikt extern 'proxy' overlay netwerk dat gedeeld wordt met backend
- OpenTelemetry resource attributes voor monitoring per environment
- Environment labels voor betere container identificatie

### Omgevingsvariabelen

| Variabele          | Beschrijving                                     | Default               | Gebruikt in          |
| ------------------ | ------------------------------------------------ | --------------------- | -------------------- |
| `IMAGE_NAME`       | Naam van de Docker image voor de frontend       | —                     | docker-compose.yml   |
| `FRONTEND_PORT`    | Poort waarop de frontend beschikbaar is          | `80`                  | docker-compose.yml   |
| `BACKEND_HOST`     | URL van de backend service                       | `http://backend:8080` | start.sh, compose    |
| `REPLICAS`         | Aantal containerinstanties (Swarm)               | `2`                   | docker-compose.yml   |
| `ENV`              | Environment naam (test/staging/production)       | `unknown`             | docker-compose.yml   |
| `VITE_BACKEND_URL` | Build-tijd URL voor frontend naar backend        | `/api`                | Dockerfile           |
| `OTEL_RESOURCE_ATTRIBUTES` | OpenTelemetry metadata voor monitoring   | —                     | docker-compose.yml   |

**Netwerk configuratie:**
- Test environment: gebruikt standaard bridge netwerk via docker compose
- Staging/Production: gebruikt extern 'proxy' overlay netwerk (Docker Swarm)
- Alle environments worden verbonden met de backend via het gedeelde netwerk

