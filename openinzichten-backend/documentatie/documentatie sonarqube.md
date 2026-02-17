## SonarQube documentatie

Deze sectie beschrijft hoe SonarQube wordt opgezet en gebruikt binnen dit project.

### Overzicht
- Doel: Automatische code kwaliteitscontrole als onderdeel van de CI-pipeline.
- Locatie configuratie: `docker/docker-compose-sonarqube.yml` bevat de compose-configuratie voor SonarQube en de bijbehorende PostgreSQL database.
- Standaard webpoort: SonarQube gebruikt poort `9000` voor de webinterface.
- URL: https://sonarqube.openinzicht.be (via reverse proxy)

### Opstarten (lokaal / runner)

Je kunt SonarQube lokaal of op de self-hosted runner starten met de `docker/docker-compose-sonarqube.yml` file.

Voorbeeld (in de projectroot):

```bash
# Start SonarQube en de benodigde database
docker compose -f docker/docker-compose-sonarqube.yml up -d --build

# Stop en verwijder
docker compose -f docker/docker-compose-sonarqube.yml down
```

### Healthcheck en CI-integratie

De CI-workflow controleert automatisch de status van SonarQube via de health endpoint en start indien nodig SonarQube opnieuw op.

**Health check proces:**
1. CI workflow maakt verbinding met `https://sonarqube.openinzicht.be/api/system/health`
2. Health check gebruikt authenticatie token (`secrets.HealthCheckTokenSonarQube`)
3. Response wordt geparst voor `"health"` status
4. Mogelijke statussen:
   - `GREEN`: SonarQube is volledig operationeel, CI gaat verder
   - `YELLOW`, `RED`, of geen response: SonarQube wordt (her)gestart via `docker compose -f docker/docker-compose-sonarqube.yml up -d --build`

**CI integratie:**
Na het health check proces draait de CI workflow:
- Maven tests met JaCoCo coverage
- SonarQube analyse via Maven: `mvn sonar:sonar`
  - Project key: `OpenInzicht`
  - Host URL: https://sonarqube.openinzicht.be
  - Token: `secrets.SONARQUBE_BACKEND_TOKEN`
  - Coverage report: `target/site/jacoco/jacoco.xml`

### Environment variabelen en volumes

De compose-file krijgt het SonarQube admin password via de pipeline, opgeslagen als GitHub secret `SQ_PASSWORD`.

**Persistent storage:**
- Volume wordt gebruikt door de PostgreSQL container voor database persistentie
- SonarQube configuratie en analyse geschiedenis blijft behouden bij herstarts

### Toegang en gebruik

1. Navigeer naar https://sonarqube.openinzicht.be
2. Log in met admin credentials
3. Project "OpenInzicht" bevat alle code quality metrics:
   - Code coverage percentage
   - Code smells, bugs, en vulnerabilities
   - Security hotspots
   - Duplicated code
   - Technical debt

Analyse resultaten worden automatisch bijgewerkt bij elke CI run (push of PR naar main/development).