## Monitoring documentatie
Deze documentatie beschrijft de monitoring stack die draait als Docker Swarm services.

### Overzicht van de monitoring
Voor het opzetten van de monitoring maken we gebruik van een geïntegreerde observability stack met:
- **Grafana**: Visualisatie en dashboards (poort 3000)
- **Prometheus**: Metrics verzameling en storage (poort 9090)
- **Loki**: Log aggregatie en querying (poort 3100)
- **Tempo**: Distributed tracing backend (poorten 3200, 4317, 4318)
- **Promtail**: Log collector die Docker container logs naar Loki stuurt
- **cAdvisor**: Container resource metrics (poort 8080)
- **Node Exporter**: Host system metrics (poort 9100)

De monitoring stack wordt gedeployed met de `deploy-monitoring.yml` GitHub Actions workflow (handmatig te triggeren).

### Docker compose monitoring
De monitoring stack draait als Docker Swarm services en maakt gebruik van het externe `proxy` overlay netwerk.

**Belangrijkste services:**

- **Prometheus**: Verzamelt metrics van de backend (via `/actuator/prometheus`), cAdvisor en Node Exporter. Configuratie in `monitoring-config/prometheus.yml`.

- **Grafana**: Dashboard platform dat data visualiseert van Prometheus (metrics), Loki (logs) en Tempo (traces). Datasources worden automatisch geconfigureerd via `grafana/datasources/provisioning.yml`. Dashboards in `grafana/dashboards/` worden gedeployed via de pipeline.

- **Loki**: Aggregeert en indexeert logs van alle Docker containers. Configuratie in `monitoring-config/loki.yml`.

- **Promtail**: Leest Docker container logs en stuurt ze naar Loki. Monitort `/var/lib/docker/containers` en gebruikt labels voor filtering. Configuratie in `monitoring-config/promtail.yml`.

- **Tempo**: Opslag en querying voor distributed traces. De backend stuurt OpenTelemetry traces naar `http://monitoring_stack_tempo:4318/v1/traces`. Configuratie in `monitoring-config/tempo.yml`.

- **cAdvisor**: Verzamelt container resource usage (CPU, memory, network, disk) en exposeert deze aan Prometheus.

- **Node Exporter**: Verzamelt host-level metrics (CPU, memory, disk, network) van de server zelf.

De stack gebruikt persistent volumes voor data opslag:
- `prometheus_data`: Prometheus time-series database
- `grafana_data`: Grafana configuratie en dashboards
- `loki_data`: Loki log storage
- `tempo_data`: Tempo trace storage
- `promtail_positions`: Promtail log read positions


### Dashboard deployment
Dashboards worden automatisch gedeployed naar Grafana via de GitHub Actions workflow:

1. Workflow wacht tot Grafana beschikbaar is
2. Verwijdert oude dashboards (behalve "Home")
3. Uploadt alle JSON bestanden uit `docker/grafana/dashboards/`

Beschikbare dashboards:
- `dashboard-vps.json`: Server resource monitoring (CPU, memory, disk, network)
- `dashboard-loki.json`: Log analysis en filtering
- `dashboard-tempo.json`: Distributed tracing visualisatie

Om een nieuw dashboard toe te voegen, exporteer het als JSON vanuit Grafana en plaats het in `docker/grafana/dashboards/`. Bij de volgende deployment wordt het automatisch geladen.