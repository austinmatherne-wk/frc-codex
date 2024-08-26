#!/bin/bash

set -e
docker compose -f ./dev/compose.yml up -d
echo "Waiting for healthy services..."
until [ -z "$(docker ps --format '{{.Names}}: {{.Status}}' | grep -v '(healthy)')" ]; do
  if docker ps --format '{{.Names}}: {{.Status}}' | grep '(unhealthy)'; then
      echo "Some services are unhealthy, exiting..."
      exit 1
  fi
  sleep 1
done
echo "All services are healthy!"
