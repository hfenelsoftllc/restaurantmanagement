# Observability Stack

This workspace now includes a container-ready observability setup for the restaurant-management services.

## What is included

- **Actuator health checks** wired into `docker-compose.yaml`
- **Prometheus** for centralized metrics scraping
- **Loki + Promtail** for centralized container log aggregation
- **Grafana** for dashboards and alert visibility
- **Alertmanager** for grouped alert handling

## Default URLs

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9095`
- Alertmanager: `http://localhost:9094`
- Loki API: `http://localhost:3100`
- Config Server health: `http://localhost:8888/actuator/health`
- Discovery Service health: `http://localhost:8761/actuator/health`
- User Management health: `http://localhost:9090/actuator/health`
- Restaurant Listing health: `http://localhost:9091/actuator/health`
- Food Catalogue health: `http://localhost:9093/actuator/health`

## Grafana credentials

Set both values in your local `.env` file before running Compose:

- `GRAFANA_ADMIN_USER`
- `GRAFANA_ADMIN_PASSWORD`

No fallback credentials are defined in `docker-compose.yaml`.

## Alert thresholds provisioned

The alert rules and dashboard thresholds use pragmatic API/SRE-style baselines:

- Availability below **99.9%** = warning posture on dashboard
- HTTP 5xx error rate above **1%** for 10 minutes = warning
- HTTP 5xx error rate above **5%** for 5 minutes = critical
- p95 latency above **500ms** for 10 minutes = warning
- p95 latency above **1s** for 5 minutes = critical
- JVM heap above **75%** for 10 minutes = warning
- JVM heap above **90%** for 5 minutes = critical
- CPU above **70%** for 10 minutes = warning
- CPU above **85%** for 5 minutes = critical

## Notes

- Prometheus handles **metrics**. For **logs**, the stack uses Loki/Promtail so Grafana can visualize both metrics and logs centrally.
- Promtail tails Docker container logs through the Docker socket. This works best when the stack is running with Linux containers enabled.

