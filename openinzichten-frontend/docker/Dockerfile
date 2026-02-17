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