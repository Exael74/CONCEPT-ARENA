# SonarQube — static analysis & coverage

This wires the project for [SonarQube](https://www.sonarsource.com/products/sonarqube/): JaCoCo
produces coverage per module (configured in the root `pom.xml`), and a local SonarQube server
(here) ingests the analysis.

## What's configured

- **Root `pom.xml`**: the JaCoCo Maven plugin (agent on `test`, XML report on `verify`) runs in
  every module, plus Sonar properties (`sonar.projectKey=conceptarena`,
  `sonar.coverage.jacoco.xmlReportPaths`, `sonar.host.url=http://localhost:9000`) and a pinned
  `sonar-maven-plugin`.
- **`sonar/docker-compose.yml`**: a self-hosted SonarQube 10 Community server + its Postgres.

## Running it

```bash
# 1. Start the SonarQube server (separate from the app stack).
docker compose -f sonar/docker-compose.yml up -d

# 2. Open http://localhost:9000 (first login admin/admin, you'll be asked to change it).
#    Create a project token: My Account → Security → Generate Token.

# 3. Build with coverage, then analyze.
mvn clean verify
mvn sonar:sonar -Dsonar.token=<YOUR_TOKEN>
#    (override the server if not local: -Dsonar.host.url=https://sonar.example.com)
```

After the scan finishes, the results are at `http://localhost:9000/dashboard?id=conceptarena`.

## Prerequisites / notes

- SonarQube needs `vm.max_map_count >= 262144` on the Docker host. Docker Desktop (Windows/macOS)
  sets this in its Linux VM automatically. On a native Linux host: `sudo sysctl -w vm.max_map_count=262144`.
- The server needs ~2 GB RAM; give Docker Desktop enough headroom.
- Coverage requires the JaCoCo reports to exist, so always run `mvn verify` (not just `test`)
  before `sonar:sonar`.
- The legacy monolith modules (`core`/`app`/`infra`/`web`/`bootstrap`) are analyzed alongside the
  microservices; exclude them via `sonar.exclusions` later if you only want the new services scored.
