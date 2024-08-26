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
Set up a Docker environment with the server and processor:
```bash
    ./dev/env-setup.sh
```

