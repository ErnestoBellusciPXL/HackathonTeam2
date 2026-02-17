## Github Actions pipeline documentatie

Deze documentatie beschrijft de volgende GitHub Actions workflows: de CI-workflow, deploy test environment backend,
deploy staging environment backend en deploy production environment backend.

### Overzicht

- CI workflow: draait SonarQube-analyse op push/pull-request, runt testen en maakt Docker image. Kan manueel gestart
  worden.
- Deploy test environment: (her)start de docker-compose voor de testomgeving op push naar development en kan manueel
  gestart worden.
- Deploy staging environment: (her)start de docker-compose stack met zero downtime (docker swarm) voor productie (
  workflow_dispatch/handmatig trigger).
- Deploy production environment: (her)start de docker-compose stack met zero downtime (docker swarm) voor productie (
  workflow_dispatch/handmatig trigger).

De environment variables in de pipeline worden gebruikt in de docker compose. Hierdoor kan er één compose file en één
dockerfile gebruikt worden voor de 3 omgevingen.

De environment variables in de pipeline worden gebruikt in de docker compose. Hierdoor kan er één compose file en één
dockerfile gebruikt worden voor de 3 omgevingen.
Alle workflows worden uitgevoerd op onze self-hosted runner.

### CI workflow

- Trigger: push naar `main` en `development`, pull request naar `main`, of handmatige trigger (`workflow_dispatch`).
- Runner: self-hosted.
- Belangrijkste stappen:
    - Checkout repository.
    - Controleren of SonarQube bereikbaar is via een healthcheck (curl naar de SonarQube health endpoint).
    - Als SonarQube niet bereikbaar of in een failed state: SonarQube starten of herstarten (via docker compose).
    - Prepareert environment (Java JDK) en runt testen.
    - Dependencies scan
    - SonarQube code kwaliteit scan.
    - Bouw van locally-stored artifact (Docker image)

### Deploy test environment workflow

- Trigger: push naar `development` of een handmatige trigger (`workflow_dispatch`).
- Runner: self-hosted.
- Doel: (her)deployen van de backend en database voor test environment.
- Environment variables:
    - DB_VOLUME: 'test_data'
    - ENV: 'test'
    - REPLICAS: '1'
    - NETWORK_DRIVER: 'bridge'

- Stappen:
    - Checkout repository
    - Deploy new backend test environment

Eerst worden bestaande containers van hetzelfde project (zelfde project-name) gestopt en verwijderd, daarna wordt de
container opnieuw opgebouwd en gestart.

### Deploy staging environment workflow

- Trigger: handmatige trigger (`workflow_dispatch`).
- Runner: self-hosted.
- Doel: (her)deployen van de backend en database voor staging environment
- Environment variables:
    - DB_VOLUME: 'staging_data'
    - ENV: 'staging'
    - REPLICAS: '2'
    - NETWORK_DRIVER: 'overlay'
    - NETWORK: 'staging_network'

- Stappen:
    - Checkout repository
    - Set up docker network
    - Use docker swarm to deploy with zero downtime
    - Connect docker services to shared network for the staging environment
    - Clean up with docker system prune

We gebruiken een overlay network omdat we met docker swarm werken.
We checken in de set up docker network stap of er al een staging network is anders maken we dezen. Dit is nodig omdat de
frontend en backend standaard een apart netwerk maken. Dus we maken manueel een shared network en voegen ze hier alle
twee in toe.

### Deploy production environment workflow

- Trigger: handmatige trigger (`workflow_dispatch`) op main branch.
- Runner: self-hosted.
- Doel: (her)deployen van de backend en database voor production environment
- Environment variables:
    - DB_VOLUME: 'prod_data'
    - ENV: 'production'
    - NETWORK_DRIVER: 'overlay'
    - NETWORK: 'production_network'

- Stappen:
    - Checkout repository
    - Set up docker network
    - Use docker swarm to deploy with zero downtime
    - Connect docker services to shared network for the production environment
    - Clean up with docker system prune

We gebruiken een overlay network omdat we met docker swarm werken.
We checken in de set up docker network stap of er al een production network is anders maken we dezen. Dit is nodig omdat
de frontend en backend standaard een apart netwerk maken. Dus we maken manueel een shared network en voegen ze hier alle
twee in toe.

