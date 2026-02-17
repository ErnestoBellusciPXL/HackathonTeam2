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