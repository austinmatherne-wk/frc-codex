# FRC CODEx

## Applications

### Server
The server hosts the filing index and admin area and runs the indexing service.

Build and test the server with the following command:
```bash
./gradlew build
```

Run the filing server:
```bash
./gradlew bootRun
```

### Processor
> **_TBD:_**  The processor will be responsible for downloading filings,
> processing them, and storing the results.

## Docker
To configure secrets for the FRC CODEx Server, add them in a `frc-codex-server.secrets` file in the root directory. Example:
```properties
COMPANIES_HOUSE_REST_API_KEY=XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
COMPANIES_HOUSE_STREAM_API_KEY=XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
```

Optionally, add `./dev/compose-local.yml` (untracked) to override Docker compose configuration.

Set up a Docker environment with the server and processor:
```bash
    ./dev/env-setup.sh
```

