# Run OpenInzicht with Docker

This project now supports a unified Docker Compose setup to run the **Backend**, **Frontend**, **Database**, and **Mailpit** with a single command.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

## Quick Start

1.  **Start the Application**:
    Run the following command in the root directory (where this file is located):
    ```bash
    docker compose up --build
    ```
    *(The `--build` flag ensures that the latest code changes in `openinzichten-backend` and `openinzichten-frontend` are recompiled.)*

2.  **Access the Services**:
    - **Frontend**: [http://localhost](http://localhost)
    - **Backend API**: [http://localhost:8080](http://localhost:8080)
    - **Mailpit (Emails)**: [http://localhost:8025](http://localhost:8025)
    - **Database**: Port `3306` matches standard MySQL.

## Configuration

### Environment Variables
The application uses default "development" credentials automatically. You can override these by setting environment variables in your shell before running `docker compose up`, or by creating a `.env` file.

| Variable | Description | Default |
| :--- | :--- | :--- |
| `OPENROUTER_API_KEY` | API Key for AI features | `placeholder_key` |
| `APP_JWT_SECRET` | Secret for signing JWTs | *(Random generated on start)* |
| `APP_ENCRYPTION_KEY` | 256-bit Key for chat encryption | *(None)* |

### Troubleshooting
-   **Frontend waiting for backend**: The frontend container waits for the backend to be reachable. If it takes too long (30s), it might start anyway. Verify the backend is running at `http://localhost:8080`.
-   **Database**: The database data is persisted in a docker volume `db_data`. To reset the database, run `docker compose down -v`.

## Development Notes
-   Changes to Java code (Backend) or Vue code (Frontend) require a restart with `--build` to take effect in this mode. For live reloading, you should run the services locally as described in their respective README files (`mvn spring-boot:run` and `npm run dev`).
