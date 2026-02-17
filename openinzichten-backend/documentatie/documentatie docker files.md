## Docker documentatie

Deze sectie beschrijft de `dockerfile` en `docker-compose.yml` die gebruikt worden voor het bouwen en draaien van de
backend en de mysql database.

### Overzicht van de dockerfile

- De dockerfile gebruikt een multi-stage build:
    - Build stage: `maven:3.9.5-eclipse-temurin-21` bouwt de applicatie en maakt een JAR file.
    - Runtime stage: `eclipse-temurin:21-jre` runtime image waar de jar in wordt geplaatst en gerunned. Dit draait
      intern op 8080
- In de Runtime stage gebruiken we het `wait-for-db.sh` script om de backend te starten omdat we eerst moeten kijken of
  de database online is voor we de backend opzetten.

```
# Multi-stage Dockerfile for building and running the Spring Boot backend
# Stage 1: build with Maven
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only what is needed to build (helps with Docker layer caching)
COPY pom.xml ./
COPY src ./src

# Build the application (skip tests to keep image build fast by default)
RUN mvn -B -DskipTests package

# Stage 2: runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the fat jar produced by Maven from the build stage
COPY --from=build /workspace/target/*.jar app.jar

# Install netcat
RUN apt-get update && apt-get install -y netcat-openbsd && rm -rf /var/lib/apt/lists/*

# Copy the wait-for-db script into /app
COPY ./docker/wait-for-db.sh /app/wait-for-db.sh
RUN chmod +x /app/wait-for-db.sh

# Expose backend port
EXPOSE 8080

# Use the wait-for-db script as entrypoint
ENTRYPOINT ["/app/wait-for-db.sh"]
```

### Wait-for-db.sh

Dit script kijk of de database online is en start dan pas de backend op. Is ook het script dat we gebruiken in de
dockerfile als entrypoint.

```
#!/bin/sh
# wait-for-db.sh
set -e

host="db"
port=3306
timeout=20  # maximum wait time in seconds
elapsed=0

echo "Waiting for MySQL at $host:$port..."

while ! nc -z $host $port; do
  if [ $elapsed -ge $timeout ]; then
    echo "Timeout reached ($timeout seconds). Starting backend anyway..."
    break
  fi
  sleep 2
  elapsed=$((elapsed + 2))
done

echo "Starting backend..."
exec java -jar /app/app.jar
```

### Overzicht van docker-compose.yml

- Services:
	- `backend`: draait de Spring Boot-app, exposeert containerpoort 8080 en leest Spring-variabelen uit environment. Verbonden met zowel `network` (voor database) als `proxy` (externe overlay network voor reverse proxy en monitoring).
	- `db`: MySQL 8.4 met benodigde environment-variabelen (`MYSQL_DATABASE`, `MYSQL_ROOT_PASSWORD`, `MYSQL_USER`, `MYSQL_PASSWORD`).

- Volumes:
	- De compose gebruikt volumes om de database data persistent te maken. Er zijn 3 volumes, één per environment (test_data, staging_data, prod_data).

- Networks:
	- `network`: intern netwerk (bridge of overlay afhankelijk van environment) voor communicatie tussen backend en database.
	- `proxy`: extern overlay netwerk voor reverse proxy (Traefik) en monitoring stack integratie.

- Healthcheck:
    - De `db` service gebruikt `mysqladmin ping` als healthcheck om te bepalen wanneer de DB klaar is.

```dockercompose
services:
  db:
    image: mysql:8.4
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_USER: ${MYSQL_USERNAME}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    expose:
      - "3306:3306"
    networks:
      - network
    volumes:
      - ${DB_VOLUME}:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1"]
      interval: 10s
      timeout: 5s
      retries: 5
    deploy:
      replicas: 1
      update_config:
        order: stop-first
        delay: 10s
      restart_policy:
        condition: on-failure
        delay: 5s
  backend:
    image: ${IMAGE_NAME}
    expose:
      - "8080:8080"
    networks:
      network:
        aliases:
            - backend
      proxy:  # External overlay network for reverse proxy & monitoring
    environment:
      SPRING_PROFILES_ACTIVE: default
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/${MYSQL_DATABASE}
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      OPENROUTER_KEY: ${OPENROUTER_API_KEY}
      MANAGEMENT_OTLP_TRACING_ENDPOINT: http://monitoring_stack_tempo:4318/v1/traces
      MANAGEMENT_TRACING_ENABLED: "true"
      MANAGEMENT_TRACING_SAMPLING_PROBABILITY: "1.0"
      OTEL_RESOURCE_ATTRIBUTES: "deployment.environment=${ENV:-unknown}"
    labels:
      - "environment=${ENV:-unknown}"
    depends_on:
      - db
    deploy:
      mode: replicated
      replicas: ${REPLICAS:-2} # Run at least 2 instances for zero downtime
      update_config:
        parallelism: 1     # Update one container at a time
        delay: 20s         # Wait 20s between updates
        order: start-first # Start new before stopping old
      restart_policy:
        condition: any
      placement:
        constraints: []
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"

volumes:
### Environment variabelen
Alle environment variabelen worden doorgegeven via de GitHub Actions pipeline. Belangrijkste variabelen:

**Database configuratie:**
- `MYSQL_DATABASE`: Naam van de database
- `MYSQL_ROOT_PASSWORD`: Root wachtwoord
- `MYSQL_USERNAME`: Database gebruiker
- `MYSQL_PASSWORD`: Database wachtwoord
- `DB_VOLUME`: Volume naam voor persistentie (test_data, staging_data, of prod_data)

**Deployment configuratie:**
- `IMAGE_NAME`: Naam van het Docker image
- `ENV`: Environment naam (test, staging, of production)
- `REPLICAS`: Aantal backend replicas (1 voor test, 2+ voor staging/production)
- `NETWORK_DRIVER`: bridge voor test, overlay voor staging/production

**AI & Monitoring:**
- `OPENROUTER_API_KEY`: API key voor OpenRouter (AI story reformatting)
- Tracing endpoint en sampling configuratie worden automatisch gezet

Bekijk pipeline documentatie voor meer info over hoe deze variabelen worden gebruikt.
  staging_data:
    driver: local
  test_data:
    driver: local

networks:
  network:
    driver: ${NETWORK_DRIVER:-bridge}
  proxy:
    external: true  # Shared overlay network managed by deployment pipeline
```

### Environment variabelen

Alle environment variable worden doorgegeven via de github actions pipeline. Bekijk pipeline documentatie voor meer info
hierover.
