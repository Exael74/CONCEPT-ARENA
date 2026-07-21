# Kubernetes manifests (S2)

Kubernetes deployment for the six app services — this is the orchestrator the audit noted was
missing (only docker-compose existed). Unlike compose's `deploy.replicas` (Swarm-only), the
`replicas` here actually run N pods, and the HPAs auto-scale on load.

## What's here
- `base.yaml` — namespace, shared `ConfigMap` (infra DNS names, profile), and the `JWT_SECRET` Secret placeholder.
- `app-deployments.yaml` — Deployment + Service for each of the 6 services, with Actuator health probes
  and resource requests/limits. `api-gateway` is the only `LoadBalancer` (D1: everything else is
  cluster-internal). game-engine/room/auth/concept-bank run 2 replicas; voice runs 1 (S12).
- `hpa.yaml` — HorizontalPodAutoscalers (CPU target 75%) for game-engine and concept-bank.

## Prerequisites
- Build + push images tagged `conceptarena/<service>:latest` (or edit the image refs):
  ```
  docker build -f game-engine-service/Dockerfile -t conceptarena/game-engine-service:latest .
  # ...repeat per service, then push to your registry
  ```
- **Stateful infra is NOT in these manifests.** Deploy Postgres (x4), Redis (x2) and RabbitMQ (with the
  `rabbitmq_stomp` + `rabbitmq_prometheus` plugins) via their official Helm charts or managed offerings,
  and expose them under the Service names in `base.yaml`'s ConfigMap (`auth-db`, `game-redis`,
  `rabbitmq`, …). This keeps the app tier (stateless, scalable) separate from stateful infra.
- `metrics-server` must be installed for the HPAs to work.

## Apply
```
kubectl apply -f k8s/base.yaml
kubectl apply -f k8s/app-deployments.yaml
kubectl apply -f k8s/hpa.yaml
kubectl -n conceptarena get pods,svc,hpa
```

## Notes
- Set a real `JWT_SECRET` first: `kubectl -n conceptarena create secret generic conceptarena-secrets --from-literal=JWT_SECRET=...` (or edit `base.yaml`, but don't commit the real value).
- Observability (Prometheus/Grafana/Loki/Zipkin) can run via the kube-prometheus-stack Helm chart;
  the services already expose `/actuator/prometheus` and report spans to Zipkin.
