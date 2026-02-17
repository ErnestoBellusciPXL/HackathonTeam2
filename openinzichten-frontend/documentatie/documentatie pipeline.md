## Github Actions pipeline documentatie

Deze documentatie beschrijft de volgende GitHub Actions workflows: de CI-workflow, deploy test environment frontend, deploy staging environment frontend en deploy production environment frontend. 

### Overzicht
- CI workflow: draait SonarQube-analyse op push/pull-request en kan manueel gestart worden.
- Deploy test environment: bouwt en (her)start de docker-compose voor de testomgeving op push naar development en kan manueel gestart worden.
- Deploy staging environment: bouwt en (her)start de docker-compose stack met zero downtime (docker swarm) voor productie (workflow_dispatch/handmatig trigger).
- Deploy production environment: bouwt en (her)start de docker-compose stack met zero downtime (docker swarm) voor productie (workflow_dispatch/handmatig trigger).

De environment variables in de pipeline worden gebruikt in de docker compose. Hierdoor kan er één compose file en één dockerfile gebruikt worden voor de 3 omgevingen.

### CI workflow

- Trigger: push naar `main` en `development`, pull request naar `main` en `development`, of handmatige trigger (`workflow_dispatch`).
- Runner: self-hosted.
- Concurrency: gebruikt `openinzichten-frontend-ci` groep om overlappende runs te voorkomen.
- Belangrijkste stappen:
	- Checkout repository
	- Controleren of SonarQube bereikbaar is via een healthcheck (curl naar de SonarQube health endpoint met auth token)
	- Als SonarQube niet bereikbaar of in een failed state: SonarQube starten of herstarten (via docker compose)
	- Node.js 22 installatie
	- Dependencies installeren met `npm ci`
	- **Trivy security scan**: scant filesystem voor HIGH en CRITICAL vulnerabilities (fail bij gevonden issues)
	- **npm audit**: controleert dependencies op MODERATE en hoger vulnerabilities
	- **Docker image build**: bouwt image met resource limits (2GB memory, 2 CPU cores)
	- **Image tagging**: tag met `latest` en `${{ github.sha }}`
	- **Cleanup**: verwijdert oude .tar archives (houdt laatste 3) en oude Docker images (houdt laatste 5)
	- **Backup**: slaat Docker image op als .tar archive in `/home/github/docker-images`
	- **PR comment**: bij falen wordt automatisch een comment op de PR geplaatst met de reden van falen

### Deploy test environment workflow

- Trigger: automatisch na succesvolle CI workflow (`workflow_run`) of handmatige trigger (`workflow_dispatch`).
- Runner: self-hosted.
- Doel: (her)deployen van de frontend voor testdoeleinden op poort 80.
- Environment variables:
    - FRONTEND_PORT: '80'
    - ENV: 'test'
    - REPLICAS: '1'
    - IMAGE_NAME: `${{ vars.DOCKER_IMAGE }}`
    - BACKEND_HOST: 'backend:8080'

- Stappen:
	- Checkout repository
	- Node.js 22.21.0 installatie
	- **Ensure proxy network exists**: initialiseert Docker Swarm en creëert overlay 'proxy' netwerk indien nodig
	- Deploy new frontend test environment met docker compose

Voor het opzetten van de test omgeving maken we gebruik van de volgende commando's:
```bash
docker compose -p $ENV -f docker-compose.yml down
docker compose -p $ENV -f docker-compose.yml up -d
```
Eerst worden bestaande containers van hetzelfde project (zelfde project-name) gestopt en verwijderd, daarna wordt de stack opnieuw opgebouwd en gestart.

De test omgeving gebruikt een overlay netwerk genaamd 'proxy' dat gedeeld wordt met de backend.

### Deploy staging environment workflow

- Trigger: pull request naar `main` of handmatige trigger (`workflow_dispatch`)
- Runner: self-hosted.
- Doel: (her)deployen van de frontend voor staging op poort 80 met zero downtime.
- Environment variables:
	- FRONTEND_PORT: '80'
	- ENV: 'staging'
    - REPLICAS: '2'
    - IMAGE_NAME: `${{ vars.DOCKER_IMAGE }}`
    - BACKEND_HOST: 'openinzicht_backend_staging_backend:8080'

- Stappen:
	- Checkout repository
	- Node.js 22.21.0 installatie
	- **Ensure proxy network exists**: initialiseert Docker Swarm en creëert overlay 'proxy' netwerk indien nodig
	- **Deploy frontend met zero downtime**: gebruikt `docker stack deploy` voor rolling updates
	- Wacht op service rollout en toont status met `docker service ps`

Deployment commando:
```bash
docker stack deploy -c docker-compose.yml openinzicht_frontend_staging
```

De staging omgeving gebruikt Docker Swarm voor zero-downtime deployments. Het 'proxy' overlay netwerk wordt gedeeld met de backend. De update configuratie zorgt ervoor dat nieuwe containers worden gestart voordat oude worden gestopt (start-first strategie).

### Deploy production environment workflow

- Trigger: handmatige trigger (`workflow_dispatch`) alleen vanaf `main` branch (beveiligd met `if: github.ref == 'refs/heads/main'`)
- Runner: self-hosted.
- Doel: (her)deployen van de frontend voor productie op poort 80 met zero downtime.
- Environment variables:
	- FRONTEND_PORT: '80'
	- ENV: 'production'
    - REPLICAS: '2'
    - IMAGE_NAME: `${{ vars.DOCKER_IMAGE }}`
    - BACKEND_HOST: 'openinzicht_backend_production_backend:8080'

- Stappen:
	- Checkout repository (alleen mogelijk vanaf main branch)
	- Node.js 22.21.0 installatie
	- **Ensure proxy network exists**: initialiseert Docker Swarm en creëert overlay 'proxy' netwerk indien nodig
	- **Deploy frontend met zero downtime**: gebruikt `docker stack deploy` voor rolling updates
	- Wacht op service rollout en toont status met `docker service ps`

Deployment commando:
```bash
docker stack deploy -c docker-compose.yml openinzicht_frontend_production
```

De productie omgeving gebruikt Docker Swarm voor zero-downtime deployments en is extra beveiligd door alleen vanaf de main branch te kunnen deployen. Het 'proxy' overlay netwerk wordt gedeeld met de backend. De update configuratie zorgt ervoor dat nieuwe containers worden gestart voordat oude worden gestopt (start-first strategie).
