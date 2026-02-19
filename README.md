# Trust Decay Simulation

This repository contains a Java + Maven simulation project for trust-decaying data retention management with ML-assisted relevance prediction.

## Project Layout

- `trustsim/` — main simulation module
  - `src/main/java/com/example/trustsim/` — source code
  - `pom.xml` — Maven configuration

## Requirements

- Java 17+
- Maven 3.8+

## Build

From the `trustsim` folder:

```bash
mvn clean compile
```

## Run

From the `trustsim` folder:

```bash
mvn exec:java
```

## Output

- Console prints simulation metrics summary.
- CSV audit trail is generated as `trustsim_audit.csv` (ignored by Git).
