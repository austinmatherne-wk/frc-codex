#!/bin/bash

set -e

if [ -f "./dev/compose-local.yml" ]; then
   EXTRA_ARGS=(-f "./dev/compose-local.yml")
fi
docker compose -f ./dev/compose.yml "${EXTRA_ARGS[@]}" up -d

# Set up the logging
mkdir -p ./dev/logs
for service in frc-codex-processor frc-codex-server; do
  docker compose -f ./dev/compose.yml logs -f $service > ./dev/logs/$service.log 2>&1 &
done

echo "Waiting for healthy services..."
until [ -z "$(docker ps --format '{{.Names}}: {{.Status}}' | grep -v '(healthy)')" ]; do
  if docker ps --format '{{.Names}}: {{.Status}}' | grep '(unhealthy)'; then
      echo "Some services are unhealthy, exiting..."
      exit 1
  fi
  echo "Some services are not ready, waiting..."
  sleep 1
  docker ps --format '  - {{.Names}}: {{.Status}}'
done
echo "All services are healthy!"
