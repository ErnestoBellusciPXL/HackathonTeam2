# OpenInZichten Backend — Documentatie

Deze handleiding beschrijft de backend van OpenInZichten: architectuur, API’s, lokale ontwikkeling, configuratie,
beveiliging (JWT), data seeders, testen en deployment-opties met Docker.

## Overzicht

- Framework: Spring Boot 3.5 (Java 21)
- Doel: REST-API voor registratie, authenticatie, verhalen (stories), postcodes en adminbeheer
- Database: MySQL 8.4 (via Docker voor development)
- Beveiliging: Spring Security + JWT (jjwt 0.11.5)
- E-mail: Mail via SMTP (lokaal met Mailpit) of alternatief via SendGrid-client
- Codekwaliteit: Tests met JUnit, JaCoCo coverage; optioneel SonarQube

Applicatie start standaard op http://localhost:8080.

## Technologie-stack en afhankelijkheden

Belangrijkste dependencies (zie `pom.xml`):

- Web/API: `spring-boot-starter-web`
- Realtime: `spring-boot-starter-websocket` (SockJS/STOMP voor chat en live updates)
- Data: `spring-boot-starter-data-jpa` + MySQL driver
- Security: `spring-boot-starter-security`, `io.jsonwebtoken:jjwt-*`
- Monitoring/metrics: `spring-boot-starter-actuator`, `micrometer-registry-prometheus`
- Validatie: `spring-boot-starter-validation`
- E-mail: `spring-boot-starter-mail`, optioneel `com.sendgrid:sendgrid-java`
- AI: `spring-ai-bom` + `spring-ai-starter-model-openai` (herformatering van stories)
- AOP: `spring-boot-starter-aop` (voor o.a. `@RateLimit`)
- Hulpmiddelen: `org.projectlombok:lombok`, `org.apache.commons:commons-csv`
- Testen: `spring-boot-starter-test`, `spring-security-test`, JaCoCo maven plugin

Java-versie: 21 (`<java.version>21</java.version>`).

## Projectstructuur (kern)

```
src/main/java/be/codeforbelgium/openinzichten/
	api/
		controllers/           # REST controllers (Auth, Account, Admin, Story, Zipcode, Condition)
		request/, response/    # DTO’s
	config/                  # SecurityConfig, dataloaders, entry points
	domain/                  # JPA entities (Account, Community, Condition, Story, Zipcode, ...)
	repository/              # Spring Data repositories
	security/                # JWT filter en service
	service/                 # Businesslogica (AuthService, StoryService, ...)
resources/
	application.properties   # DB/JWT/mail configuratie
	data/
		allZipcodes.csv			# Seed data voor postcodes (België)
		zipcodesVlaanderen.csv	# Seed data voor postcodes (Enkel Vlaanderen)
```

## Data seeders

- Postcodes: `ZipcodeDataLoader` leest standaard `classpath:data/allZipcodes.csv` of
  `classpath:data/zipcodesVlaanderen.csv` en vult de tabel `postcodes` bij lege database. Pad aanpasbaar via property
  `app.zipcodes.resource`.
- Aandoeningen & gemeenschappen: `ConditionCommunityDataLoader` zaait voorbeelddata en relaties (éénmalig bij lege
  tabellen).
- Admin account: `UserDataLoader` maakt een standaard admin aan indien niet aanwezig:
    - Email: `admin@openinzicht.be`
    - Wachtwoord: `0penInzicht?1PXL`

Let op: deze seeders zijn bedoeld voor development/acceptatie, niet voor productie.

## Beveiliging (JWT + Spring Security)

- JWT-secret: Base64-gecodeerde sleutel in property `app.jwt.secret` (zie `application.properties`).
    - Ontbreekt de secret, dan wordt een tijdelijke in-memory sleutel gegenereerd (handig voor lokaal testen, tokens
      ongeldig na herstart).
- Tokenverval: standaard 24 uur; met `rememberMe=true` in login wordt dit 90 dagen.
- Autorisatie: Bearer token in de `Authorization` header.
- Belangrijke toestemmingen (zie `SecurityConfig`):
    - Publiek: OPTIONS, `/error`, `/api/debug/**`, `GET /api/heatmap/**`
    - Auth vrij: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/check-info`,
      `POST /api/auth/password-reset`, `POST /api/auth/password-reset/confirm`, `GET /api/zipcodes`, alle methodes op
      `/api/conditions/**`, `GET /api/stories/**` (lezen)
    - Auth nodig: `PUT /api/auth/complete`, `PUT /api/auth/zipcode`, `POST /api/stories`,
      `PUT /api/stories/update-my-story`, `POST /api/stories/ai/**`, `POST /api/stories/{id}/likes`,
      `DELETE /api/stories/{id}/likes`, `DELETE /api/stories/{id}`, `GET /api/stories/byuserid` (huidige gebruiker via
      JWT), `/api/connection/**`, `/api/chat/**`, `POST /api/tickets`, `POST /api/account/**`
    - Admin: alle paden onder `/api/admin/**` vereisen rol `Admin`

## API-overzicht (belangrijkste endpoints)

Auth (`/api/auth`)

- POST `/register` — Account aanmaken, geeft `{ token }`
- POST `/login` — Inloggen met `{ username|email, password, rememberMe }`, geeft `{ token }`
- PUT `/complete` — Registratie vervolledigen (profiel/condities), vereist JWT
- PUT `/zipcode` — Postcode updaten, vereist JWT
- POST `/password-reset` — Resetverzoek per e-mail
- POST `/password-reset/confirm` — Reset bevestigen met token
- POST `/check-info` — Bestaan van username/email controleren (boolean)

Account (`/api/account`)

- DELETE `/delete` — Account verwijderen o.b.v. `userId` + `password` (JWT vereist in security policy)

Conditions (`/api/conditions`)

- GET `/` — Lijst (optionele `?limit=`)
- GET `/user-conditions` — Condities voor ingelogde gebruiker (JWT)

Stories (`/api/stories`)

- POST `/` — Verhaal toevoegen (JWT)
- GET `/{id}` — Verhaal ophalen
- GET `/byuserid/{id}` — Verhalen van gebruiker
- GET `/` — Pagineren met `?page=` en `?size=`
- PUT `/update-my-story` — Eigen verhaal bijwerken (JWT)
- POST `/ai/reformat` — AI-herformatering van storytekst (JWT, rate limit 3/uur)
- POST `/{id}/likes` — Story liken (JWT)
- DELETE `/{id}/likes` — Like verwijderen (JWT)
- DELETE `/{id}` — Eigen verhaal verwijderen (JWT)

Connections (`/api/connection`)

- POST `/invite` — Connectieverzoek versturen (JWT, body `fromUserId`, `toUserId`)
- GET `/{accountId}` — Lijst connecties en pending requests voor gebruiker (JWT)
- POST `/accept` — Inkomend verzoek accepteren (JWT)
- POST `/reject` — Inkomend verzoek weigeren (JWT)
- POST `/disconnect` — Verbinding beëindigen (JWT)
- POST `/cancel` — Uitgaand verzoek intrekken (JWT)

Chat (`/api/chat`)

- POST `/send` — Bericht sturen in een chat (JWT, body `chatId`, `message`)
- GET `/{chatId}` — Berichten in een chat ophalen (JWT)
- GET `/quickoverview/{userId}` — Overzicht van chats met laatste bericht (JWT)

Tickets (`/api/tickets`)

- POST `/` — Ticket aanmaken/rapporteren (JWT, body met `type`, `reportReason`, optioneel `storyId`, `otherReason`)

Zipcodes (`/api/zipcodes`)

- GET `/` — Alle postcodes (code + gemeenten)
- GET `/{zipcode}/gemeenten` — Gemeenten voor postcode

Heatmap (`/api/heatmap`)

- GET `/counts-by-municipality` — Aantal communityleden per gemeente; optioneel filter `?condition=` voor specifieke
  aandoening (publiek)

Admin (`/api/admin`)

- GET `/accounts` — Paginatie van accounts (rol `Admin` vereist)

Admin tickets (`/api/admin/tickets`)

- GET `/` — Tickets pagineren met `?page=` en `?pageSize=` (Admin)
- GET `/{ticketId}` — Ticketdetails ophalen (Admin)
- POST `/{ticketId}/close` — Ticket sluiten; optioneel `?removeStory=` om bijhorende story te verwijderen (Admin)

## Configuratie (application.properties)

Standaardwaarden voor lokale ontwikkeling (MySQL via Docker):

- JDBC URL: `jdbc:mysql://localhost:3306/app_db`
- Gebruiker/wachtwoord: `app_user` / `dev_password`
- JPA: `spring.jpa.hibernate.ddl-auto=update` (pas aan voor productie)
- JWT secret: `app.jwt.secret` (Base64). Voorbeeld genereren:

Genereer een 256-bit Base64-gecodeerde sleutel:

- Linux/macOS (bash/zsh):

```bash
head -c 32 /dev/urandom | base64
```

- Windows (PowerShell):

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Stel vervolgens de secret in via environment variabele of in `application.properties`:

- Linux/macOS (bash/zsh):

```bash
export APP_JWT_SECRET="<plak-je-base64-sleutel>"
```

- Windows (PowerShell):

```powershell
$env:APP_JWT_SECRET = "<plak-je-base64-sleutel>"
```

- Windows (CMD, tijdelijk voor huidige sessie):

```bat
set APP_JWT_SECRET=<plak-je-base64-sleutel>
```

- Windows (CMD, persistent voor nieuwe sessies):

```bat
setx APP_JWT_SECRET "<plak-je-base64-sleutel>"
```

Opmerking: je kunt ook `app.jwt.secret` rechtstreeks in `application.properties` zetten, maar dit is niet aanbevolen
voor productie.

Mail (lokaal met Mailpit):

```
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
app.mail.from=no-reply@local.test
```

Frontend reset-URL (voor wachtwoordresetlinks): `app.frontend.reset-url`

## Lokale ontwikkeling

1) Start de dev-services (MySQL + Mailpit) met Docker:

```sh
docker compose -f docker/docker-compose.dev.yml up -d
```

2) Controleer dat `application.properties` overeenkomt met de compose-credentials:

- user: `app_user`
- password: `dev_password`
- db: `app_db`

3) (Aanbevolen) Zet een stabiele JWT secret (Base64) in de omgeving:

- Linux/macOS (bash/zsh):

```bash
export APP_JWT_SECRET="$(head -c 32 /dev/urandom | base64)"
```

- Windows (PowerShell):

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:APP_JWT_SECRET = [Convert]::ToBase64String($bytes)
```

4) Start de applicatie met Maven:

```sh
mvn -DskipTests spring-boot:run
```

Applicatie draait op http://localhost:8080.

## Snelle API-check

Hieronder vind je voorbeelden voor zowel bash/zsh (Linux/macOS) als PowerShell (Windows).

### Linux/macOS (bash/zsh) met curl + jq

```bash
# Registreren en token ophalen
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","email":"test@example.com","password":"secret"}' | jq -r .token)

# Postcode updaten met Bearer token
curl -s -X PUT http://localhost:8080/api/auth/zipcode \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"zipcode":"3000"}'
```

### Windows (PowerShell) met Invoke-RestMethod

```powershell
# Registreren en token ophalen
$registerBody = @{ username = 'testuser'; email = 'test@example.com'; password = 'secret' } | ConvertTo-Json
$token = (Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/auth/register' -ContentType 'application/json' -Body $registerBody).token

# Postcode updaten met Bearer token
$headers = @{ Authorization = "Bearer $token" }
$zipcodeBody = @{ zipcode = '3000' } | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri 'http://localhost:8080/api/auth/zipcode' -Headers $headers -ContentType 'application/json' -Body $zipcodeBody
```

Tip: `jq` installeren

- macOS: `brew install jq`
- Debian/Ubuntu: `sudo apt-get install jq`
- Windows: `choco install jq` of `scoop install jq`

## Testen, coverage en kwaliteitscontrole

- Unit tests draaien:

```sh
mvn test
```

- Coverage rapport (JaCoCo): `target/site/jacoco/index.html`
- Test- en coverageconfig zijn reeds aanwezig in `pom.xml`. Bepaalde infrastructuurklassen zijn uitgesloten van
  coverage.
- SonarQube: er is een compose-bestand aanwezig (`docker/docker-compose-sonarqube.yml`). Start SonarQube indien nodig en
  configureer je scanner buiten deze repo.

## Deployment met Docker Compose

Voor een productie-achtige setup is `docker/docker-compose.yml` beschikbaar. Belangrijke env-variabelen:

- `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`
- `IMAGE_NAME` voor de backendcontainer
- De backend verwacht datasource-URL/credentials via omgevingsvariabelen (`SPRING_DATASOURCE_*`).

Het bestand is geconfigureerd voor meerdere replicas en rolling updates-instellingen via `deploy.update_config`.

## Monitoring en extra docker

- `docker/docker-compose-monitoring.yml` en `docker/prometheus/*.yml` bevatten voorbeelden voor monitoring (Prometheus).
  Pas aan naar eigen omgeving.
- `docker/selenium-compose.yml` is beschikbaar voor e2e-testscenario’s met Selenium (optioneel).

## Probleemoplossing (FAQ)

- MySQL connectie geweigerd: controleer of compose actief is en poort 3306 vrij is. Verifieer credentials en
  `spring.datasource.url`.
- JWT invalid/expired: stel een geldige Base64 secret in via `APP_JWT_SECRET`; login opnieuw voor een nieuw token.
- 401 op publieke endpoints: zie `SecurityConfig` voor uitzonderingen; voeg eventueel CORS/headers toe in verzoek.
- Zipcodes niet geladen: controleer of `app.zipcodes.resource` pad naar `data/allZipcodes.csv` of
  `data/zipcodesVlaanderen.csv` aanwezig is en of de database leeg was op de eerste start; overschrijf pad met
  `app.zipcodes.resource` indien nodig.
- Mail niet ontvangen: controleer Mailpit Web UI op http://localhost:8025 en SMTP-instellingen in
  `application.properties`.

## Referenties

- `pom.xml` — afhankelijkheden, build en JaCoCo
- `src/main/resources/application.properties` — DB/JWT/mail-config
- `docker/` — compose-bestanden voor dev/monitoring/sonarqube
- `src/main/java/.../config/*DataLoader.java` — seeders
- `src/main/java/.../config/SecurityConfig.java` — beveiligingsregels

—

Laat weten als je specifieke integraties (CI/CD, Sonar, monitoring) verder wil uitwerken; de compose- en
configuratiebestanden zijn aanwezig als vertrekpunt.

