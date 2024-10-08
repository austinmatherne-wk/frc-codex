#!/bin/bash

set -e
docker build -t frc-codex-lambda --platform linux/amd64 -f frc-codex-lambda.Dockerfile .
docker build -t frc-codex-processor -f frc-codex-processor.Dockerfile .
docker build -t frc-codex-server -f frc-codex-server.Dockerfile .
