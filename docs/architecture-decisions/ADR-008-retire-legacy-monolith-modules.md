# ADR-008: Retire the legacy monolith modules from the build

## Status
Accepted

## Context
The migration from the monolith to microservices kept the original modules — `core`, `app`, `infra`,
`web`, `bootstrap` — in the Maven reactor as a reference implementation while the new services were
built out phase by phase (see ADR-001…ADR-007). By the end of the migration these modules were dead
code: `core` duplicated the kernel's `Command`, `DomainEvent`, `EntityId`, `Email` and `PasswordHash`;
`web` duplicated the services' controllers/DTOs under `com.conceptarena.web.rest`. They still compiled
on every build, and their existence created a real hazard of editing the wrong (obsolete) tree, plus a
second, divergent copy of the DDD primitives.

## Decision
Remove `core`, `app`, `infra`, `web` and `bootstrap` from the root `pom.xml` `<modules>` and their
`dependencyManagement` entries, and delete the directories. The microservices + `conceptarena-kernel`
now fully supersede them; nothing in the six services imports `com.conceptarena.core/app/infra/web`
(verified by grep). The old k6 baseline that once targeted `bootstrap` is retained only as a historical
file (`scripts/load/k6-run-output.monolith-baseline-2026-07-15.txt`); the current load test targets the
gateway/microservices stack.

## Consequences
- Faster builds and Docker image builds (less to compile and copy).
- A single source of truth for the DDD primitives (the kernel), removing E1/E6's duplication.
- The monolith is recoverable from git history if ever needed for reference.
- Resolves gaps E6, E7, E8.
