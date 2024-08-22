# FRC CODEx

## Filing Index
The filing index is the root application of this repository.
Build and test the filing index server with the following command:
```bash
    ./gradlew build
```

Run the filing server:
```bash
    ./gradlew bootRun
```

## Indexer
> **_TBD:_**  The indexer will be responsible for discovering filings, 
> registering them with the database, and adding them to the queue for work.

## Processor
> **_TBD:_**  The processor will be responsible for downloading filings,
> processing them, and storing the results.
