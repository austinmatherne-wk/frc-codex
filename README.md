# FRC CODEx

## Applications

### Server
The server hosts the filing index and admin area and runs the indexing service.

To build and test:
```bash
./gradlew build
```

### Processor
The processor receives jobs from the filing index, processes them, and publishes the results.

To install dependencies:
```bash
pip install -r requirements-dev.txt
```

To run unit tests:
```bash
pytest processor_tests
```

To run code style tests:
```bash
flake8 .
```

To run typing tests:
```bash
mypy .
```

## Docker
The preferred method to run any part of the FRC CODEx is using Docker Compose.

### Setup

#### Secrets
To configure secrets for the server and processor, add them in a `frc-codex-server.secrets` file in the root directory. Example:
```properties
AWS_ACCESS_KEY_ID=AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY=AWS_SECRET_ACCESS_KEY
COMPANIES_HOUSE_REST_API_KEY=XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
COMPANIES_HOUSE_STREAM_API_KEY=XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
```

#### Local Configuration
Add `./dev/compose-local.yml` (untracked) to override Docker compose configuration.
This is required to add a volume for the processor to access your local installation of `frc-ixbrl-viewer`.
The processor expects an `iXBRLViewerPlugin` directory in the plugins directory: `/processor/plugins/iXBRLViewerPlugin`.

Example `./dev/compose-local.yml`:
```yaml
services:
  frc-codex-processor:
    volumes:
      - ../../frc-ixbrl-viewer/iXBRLViewerPlugin:/processor/plugins/iXBRLViewerPlugin
```

### Compose
To build images and compose services:
```bash
    ./dev/env-setup.sh
```
Or, to build images and compose services separately, use `./dev/env-build.sh` and `./dev/env-compose.sh` respectively.
