# Capstone Discovery Toolchain

This project implements the discovery workflow described in the lecture notes:

- `data/capstones.csv` is the flat-file source of truth.
- `ProjectRegistry` loads CSV data with `BufferedReader`.
- `ProjectService` exposes lookup behavior.
- `CapstoneDiscoveryServer` publishes a searchable web UI.
- JUnit verifies parsing and lookup behavior.
- Cucumber plus Selenium verifies end-to-end propagation from CSV to UI.

## Run the app

```bash
mvn compile
java -cp target/classes com.concordia.discovery.DiscoveryApplication
```

Open `http://localhost:8080/search`.

Browse the sample registry at `http://localhost:8080/registry`.

## Run tests

```bash
mvn test
```
