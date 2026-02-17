# OpenInzicht Backend

This README explains how to run the backend locally for development, including how to provide a JWT signing secret via environment variable or application.properties, setup requirements, and other information that can help when developing this project.

## Prerequisites

Make sure the following software is installed on your development machine:

- Java 21 LTS
- Maven
- Docker (for the dev MySQL database, we use MySQL 8.4 LTS currently)

## Table of Contents

- [Quick start](#quick-start)
- [API flow (JWT)](#api-flow-jwt)
- [Register and obtain token](#register-and-obtain-token)
- [Complete Registration using Bearer Token](#complete-registration-using-bearer-token)
- [AI Logic](#ai-logic)
- [Project Structure](#project-structure)
- [Environment Variables](#environment-variables)
- [WireGuard VPN Usage](#wireguard-vpn-usage)


## Quick start

1. To start development containers:  
   `docker compose -f docker/docker-compose.dev.yml up -d`

   To run only one of the containers you can use one of the following commands:

   - `docker compose -f docker/docker-compose.dev.yml up -d mysql` to run MySQL
   - `docker compose -f docker/docker-compose.dev.yml up -d mailpit` to run Mailpit

2. Create a base64-encoded JWT secret and export it as an environment variable. The application reads `app.jwt.secret` from application.properties which can be populated from the environment.

- Generate a new random 256-bit key and store it base64-encoded in APP_JWT_SECRET
  - For PowerShell:
    - `$key = (New-Object Security.Cryptography.HMACSHA256).Key`
    - `$env:APP_JWT_SECRET = [Convert]::ToBase64String($key)`
  - For Bash:
    - `export APP_JWT_SECRET=$(openssl rand -base64 32)`
- Alternatively set `app.jwt.secret` in `src/main/resources/application.properties` directly (not recommended for production)

3. Create an encryption key (preferably 256-bit base64) for sensitive data encryption like chat messages. Set it in the environment variables as `APP_ENCRYPTION_KEY` or set the `app.encryption.key` in `application.properties`.

4. Set the OpenRouter API key for AI functionality:

   - In PowerShell: `$env:OPENROUTER_API_KEY = "your_openrouter_api_key"`
   - In Bash: `export OPENROUTER_API_KEY="your_openrouter_api_key"`
     Alternatively, set `spring.ai.openai.api-key` in `application.properties`.

5. (Optional) set other AI application.properties as needed, they are `spring.ai.openai.chat.options.model` for the model that will be used, and `app.ai.system-prompt-path` for the system prompt classpath with a txt file containing the system prompt for easy editing.

6. Run the Spring Boot application:

   `mvn -DskipTests spring-boot:run`

## API flow (JWT)

- Register (`POST /api/auth/register`)

  - Example request body:

  ```json
  {
    "username": "alice",
    "email": "alice@example.com",
    "password": "MySecurePassword123!"
  }
  ```

  - Response:

  ```json
  { "token": "<JWT_Token>" }
  ```

- Update zipcode (`PUT /api/auth/complete`)
  - Header: `Authorization: Bearer <JWT>`
  - Example request body:
  ```json
  {
    "zipcode": "3500",
    "conditions": ["Aandoening1", "OptioneelAandoeningX", "..."],
    "hasCondition": true
  }
  ```
  - Response: updated account JSON
  ```json
  {
    "id": "abcdef01-abcd-4321-1234-fedcba012345",
    "username": "alice",
    "email": "alice@example.com",
    "zipcode": "3500",
    "hasCondition": true,
    "conditions": ["Aandoening1", "OptioneelAandoeningX", "..."],
    "communities": ["Community1", "Community2", "..."]
  }
  ```

## Register and obtain token

```js
// Load from environment variable
const backendUrl = import.meta.env.VITE_BACKEND_URL;

// Register new user
const response = await fetch(`${backendUrl}/api/auth/register`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    username: "testuser",
    email: "test@example.com",
    password: "secret",
  }),
});

const data = await response.json();
const token = data.token; // JWT token
```

## Complete Registration using Bearer Token

```js
const backendUrl = import.meta.env.VITE_BACKEND_URL;

// Complete registration
const response = await fetch(`${backendUrl}/api/auth/complete`, {
  method: "PUT",
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`, // Use the JWT token obtained earlier
  },
  body: JSON.stringify({
    zipcode: "3500",
    conditions: ["Aandoening1", "OptioneelAandoeningX"],
    hasCondition: true,
  }),
});

const updatedAccount = await response.json();
console.log(updatedAccount);
```

After completing registration, the user can access protected endpoints using the provided JWT token in the `Authorization` header.

Notes

- The application will fall back to an ephemeral in-memory key if `app.jwt.secret` is not provided. That means tokens will be different across restarts. For stable tokens across restarts, set `app.jwt.secret`.
- For production, use a strong, securely-stored key and consider rotating keys via a key management system.

## AI Logic

In this project we also have some AI functionality built in. To use these there are 2 important application.properties
values that can / need to be changed.

- `spring.ai.openai.api-key`: **THIS ONE IS A MUST CHANGE!**, put here the OpenRouter API key that you use to access OpenRouter models.
- `app.ai.system-prompt`: This one is a _can_ change, not really necessary. The system prompt we use is just a basic one that allows for the AI to reformat the story text. Can be rewritten or adjusted to fine tune its functionality.
- `spring.ai.openai.chat.options.model`: This one is a _can_ change. We use `openai/gpt-oss-20b:nitro` as our model. GPT-OSS-20B seems like more than enough for our use-case. the `:nitro` routes it to the fastest provider for the model on OpenRouter.
- `spring.ai.openai.base-url`: This one you should **not** change. We have set it to `https://openrouter.ai/api` since it uses an OpenAI-compatible API. This is how we communicate with OpenRouter. Of course, if there is a need to switch to another provider that is not OpenRouter, you should change this, but as long as we are using models through OpenRouter we should keep this.

## Project Structure

The backend project follows a standard Maven project structure:

```
src/
 ├── main/
 │   ├── java/                                # Java source code
 │   │   └── be/codeforbelgium/openinzichten  # Base package
 │   └── resources/                           # Application resources
 │       ├── application.properties           # Main configuration file
 │       └── data/                            # Zipcode data
 └── test/                                    # Test source code
```

The application code itself is organized into packages based on functionality, such as controllers, services, repositories, models, and configuration. See below:

```
be.codeforbelgium.openinzichten
 ├── annotation/                    # Custom annotations
 ├── api/                           # API-related classes
 │   ├── controllers/               # API controllers
 │   ├── request/                   # Request DTOs
 │   ├── response/                  # Response DTOs
 │   └── RestExceptionHandler.java  # Global exception handler
 ├── aspect/                        # AOP aspects
 ├── config/                        # Application configuration, data loaders, security
 ├── domain/                        # Domain entities and enums
 ├── exceptions/                    # Custom exceptions
 ├── repository/                    # Data access layer
 ├── security/                      # Security-related classes
 ├── service/                       # Business logic layer
 └── OpenInzichtenApplication.java  # Main application class
```

## Environment Variables

The application can be configured using the following environment variables:

`APP_ENCRYPTION_KEY`: Base64-encoded encryption key for chat
`APP_ADMIN_EMAIL`: Email address for the initial admin
`APP_ADMIN_PASSWORD`: Password for the initial admin
`OPENROUTER_KEY`: OpenRouter API key for AI functionality
`APP_JWT_SECRET`: Base64-encoded JWT signing secret


## WireGuard VPN Usage

To connect to some of the internal resources (SonarQube, testing environment, etc) you need to be using WireGuard tunnel.

### Usage

Before we start, you will need to install the [WireGuard app](https://www.wireguard.com/install/), then follow the steps below

#### Windows (and probably Mac)

1. Open WireGuard app, click on `Add Tunnel` -> `Import tunnel(s) from file...` and select your `wg-openinzicht.conf`
2. Click Activate

You should see two IPs in `Allowed IPs` field.

#### Linux

1. Go to folder with `wg-openinzicht.conf` and use this command:

```bash
sudo wg-quick up ./wg-openinzicht.conf
```

To disable the VPN replace `up` with `down`.
