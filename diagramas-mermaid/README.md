# Diagramas — ConceptArena

Diagramas de arquitectura en **Mermaid**, generados a partir del código real del repositorio y
siguiendo notación estándar: **C4** para contexto/componentes/despliegue y **UML** para secuencia y
entidad-relación. GitHub, VS Code y la mayoría de editores renderizan los bloques ` ```mermaid `
directamente. Para editarlos online: <https://mermaid.live>.

Contenido (únicamente estos 5):

1. [Contexto](#1-contexto) — C4 Nivel 1: usuarios y sistemas externos.
2. [Componentes](#2-componentes) — C4 Nivel 2: contenedores (microservicios + infraestructura).
3. [Despliegue](#3-despliegue) — C4 Deployment: infraestructura física real en Azure + CI/CD.
4. [Secuencia (tiempo real)](#4-secuencia-tiempo-real) — UML: flujo realtime de ronda/respuesta (STOMP).
5. [Entidad-Relación](#5-entidad-relación) — UML/ER: modelo de datos de cada base (una por servicio).

> **Actores:** el único usuario del sistema es el **Jugador/Estudiante**; crear y organizar una sala
> es un rol del propio jugador (no un actor aparte). Los desarrolladores **no** son actores del
> sistema: el despliegue lo realiza GitHub Actions (sistema externo), no una persona.

> Los diagramas reflejan `main` al momento de generarlos. Si el código cambia (nueva clase, campo o
> ruta relevante), actualiza el bloque Mermaid correspondiente a mano.

---

## 1. Contexto

C4 Nivel 1: quién usa ConceptArena y con qué sistemas externos habla.

```mermaid
C4Context
    title Diagrama de Contexto — ConceptArena

    Person(player, "Jugador / Estudiante", "Se une o crea/organiza salas y responde en tiempo real")

    System(arena, "ConceptArena", "Plataforma de trivia y estudio colaborativo en tiempo real: salas, rondas, respuestas, ranking y voz")

    System_Ext(smtp, "Servidor SMTP", "MailHog (dev) / SMTP real (prod): entrega códigos OTP")
    System_Ext(github, "GitHub Actions", "CI/CD: construye, prueba y despliega")

    Rel(player, arena, "Juega, crea salas y responde", "HTTPS + WSS")
    Rel(arena, smtp, "Envía código OTP (login sin contraseña)", "SMTP")
    Rel(github, arena, "Despliega automáticamente", "SSH a VM Azure")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

---

## 2. Componentes

C4 Nivel 2 (contenedores): el api-gateway, los 5 microservicios de dominio y la infraestructura.
Cada servicio es dueño exclusivo de sus almacenes; la comunicación asíncrona pasa por RabbitMQ.

```mermaid
C4Container
    title Diagrama de Componentes (Contenedores) — ConceptArena

    Person(player, "Jugador / Estudiante", "Navegador web")
    Container(fe, "Frontend SPA", "React / Vite", "Desplegado aparte; consume el gateway")

    System_Boundary(backend, "ConceptArena — Backend") {
        Container(gw, "api-gateway", "Spring Cloud Gateway", "CORS · rate limit global · X-Request-Id")

        Container(auth, "auth-service", "Spring Boot", "Registro · login · JWT · OTP")
        Container(room, "room-service", "Spring Boot", "Salas · lobby · STOMP")
        Container(game, "game-engine-service", "Spring Boot", "Rondas · respuestas · ranking")
        Container(bank, "concept-bank-service", "Spring Boot", "Bancos de conceptos")
        Container(voice, "voice-signaling-service", "Spring Boot", "Señalización WebRTC")

        ContainerQueue(mq, "RabbitMQ", "AMQP", "Exchanges por servicio · DLQ")
        Container(mailhog, "MailHog", "SMTP dev", "Buzón de correo de desarrollo")

        ContainerDb(authdb, "auth-db", "PostgreSQL", "")
        ContainerDb(roomdb, "room-outbox-db", "PostgreSQL", "Solo tabla outbox")
        ContainerDb(gamedb, "game-db", "PostgreSQL", "")
        ContainerDb(bankdb, "conceptbank-db", "PostgreSQL", "")

        ContainerDb(authredis, "auth-redis", "Redis", "Rate limit + OTP")
        ContainerDb(roomredis, "room-redis", "Redis", "Estado de dominio (AOF)")
        ContainerDb(gameredis, "game-redis", "Redis", "Estado de partida · locks")
        ContainerDb(voiceredis, "voice-redis", "Redis", "Pub/Sub señalización")
        ContainerDb(gwredis, "gateway-redis", "Redis", "Rate limit global")
    }

    Rel(player, fe, "Usa el juego", "HTTPS")
    Rel(fe, gw, "API + WebSocket", "HTTPS /api/** · WSS /ws/**")

    Rel(gw, auth, "/api/auth/**", "HTTP")
    Rel(gw, room, "/api/rooms/** · /ws/lobby", "HTTP/WS")
    Rel(gw, game, "/api/game/** · /ws/game", "HTTP/WS")
    Rel(gw, bank, "/api/concept-banks/**", "HTTP")
    Rel(gw, voice, "/ws/signaling", "WS")
    Rel(gw, gwredis, "Rate limit", "")

    Rel(auth, authdb, "", "JDBC")
    Rel(auth, authredis, "", "")
    Rel(auth, mailhog, "Código OTP", "SMTP")
    Rel(room, roomdb, "Outbox", "JDBC")
    Rel(room, roomredis, "Estado", "")
    Rel(game, gamedb, "", "JDBC")
    Rel(game, gameredis, "", "")
    Rel(bank, bankdb, "", "JDBC")
    Rel(voice, voiceredis, "", "")

    Rel(auth, mq, "Publica (outbox)", "AMQP")
    Rel(room, mq, "Publica / consume", "AMQP")
    Rel(game, mq, "Publica / consume", "AMQP")
    Rel(bank, mq, "Publica", "AMQP")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

**Nota (patrón Outbox):** en los 4 servicios con BD, escribir estado y registrar el evento es
atómico (misma transacción); un publicador programado drena la tabla `outbox_event` hacia RabbitMQ.

---

## 3. Despliegue

C4 Deployment: infraestructura física **real** desplegada hoy — 1 VM en Azure que corre
`docker-compose.backend.yml`, más el pipeline de GitHub Actions que la actualiza en cada push a `main`.

```mermaid
C4Deployment
    title Diagrama de Despliegue — ConceptArena (Azure)

    Person(player, "Jugador", "Vía frontend desplegado aparte")

    Deployment_Node(gh, "GitHub", "SaaS · Actions runner (ubuntu-latest)") {
        Container(ci, "Pipeline CI/CD", "GitHub Actions", "build+test (Testcontainers) → validación compose → deploy (rsync + SSH)")
    }

    Deployment_Node(azure, "Microsoft Azure — Azure for Students", "Cloud") {
        Deployment_Node(rg, "Resource Group: conceptarena-rg", "región eastus2") {
            Deployment_Node(vm, "VM: conceptarena-backend", "Standard_D4s_v3 · Ubuntu 22.04 · Docker · NSG: 8080 y 22 públicos, resto solo 127.0.0.1") {
                Deployment_Node(compose, "docker-compose.backend.yml", "Docker Compose") {
                    Container(gw, "api-gateway", "Spring Boot", ":8080 (público)")
                    Container(svcs, "5 microservicios", "Spring Boot", "auth · room · game · bank · voice (:8081-8085)")
                    ContainerDb(data, "Datastores", "4 PostgreSQL + 5 Redis", "solo 127.0.0.1")
                    ContainerQueue(mq, "RabbitMQ", "AMQP", "solo 127.0.0.1")
                }
            }
        }
    }

    Rel(player, gw, "HTTP :8080", "vía NSG")
    Rel(ci, gw, "Despliega (SSH azureuser)", "docker compose up -d --build")
    Rel(gw, svcs, "Enruta", "HTTP/WS")
    Rel(svcs, data, "Persisten estado", "JDBC / Redis")
    Rel(svcs, mq, "Eventos de dominio", "AMQP")
```

**Nota:** esta variante NO incluye frontend ni el stack de observabilidad
(Prometheus/Grafana/Loki/Zipkin viven en `docker-compose.yml`, no desplegado en la VM). El
`JWT_SECRET` vive en `~/conceptarena/.env` de la VM y se excluye del rsync.

---

## 4. Secuencia (tiempo real)

Diagrama de secuencia UML del componente central: iniciar una ronda, enviar respuestas por
WebSocket y difundir el resultado a todos los jugadores suscritos vía STOMP
(`/topic/rooms/{roomId}/round`). El **anfitrión es un jugador** que creó la sala.

```mermaid
sequenceDiagram
    autonumber
    actor Host as Jugador anfitrión
    actor Player as Otros jugadores
    participant GW as api-gateway
    participant WS as game WS / STOMP
    participant Cmd as CommandBus
    participant H as SubmitAnswerHandler
    participant RM as RoomReadModel
    participant Repo as RoundRepository (game-db)
    participant Bus as EventBus
    participant Topic as /topic/rooms/{id}/round

    Note over Host,Topic: Inicio de ronda
    Host->>GW: POST /api/game/{roomId}/start (JWT)
    GW->>Cmd: StartRoundCommand(roomId, userId)
    Cmd->>Repo: guarda Round (ACTIVE)
    Repo-->>Bus: RoundStarted
    Bus-->>Topic: ROUND_STARTED (question, difficulty, durationSeconds)
    Topic-->>Player: pregunta en vivo

    Note over Player,Topic: Envío de respuesta
    Player->>GW: WSS /ws/game — {type:"answer", roomId, payload}
    GW->>WS: frame (handshake ya validó JWT → userId)
    WS->>WS: rate limit 3/s · userId del JWT (no del payload)
    WS->>Cmd: SubmitAnswerCommand(roomId, userId, texto)
    Cmd->>H: handle
    H->>RM: isParticipant(roomId, userId)?
    RM-->>H: sí
    H->>Repo: findActiveRound → submitAnswer → scoring → save
    Repo-->>H: guardado (optimistic lock @Version)
    H-->>Bus: AnswerSubmitted

    Note over Player,Topic: Fin de ronda (timer expira o todos responden)
    Bus-->>Topic: ROUND_ENDED (scores)
    Topic-->>Player: ranking actualizado en vivo
```

**Notas de fidelidad:**
- El `userId` siempre proviene del JWT (handshake WS o principal REST), nunca del cuerpo/payload
  del cliente — no se puede suplantar a otro usuario.
- El mismo `AnswerRateLimiter` (3/seg) aplica en la ruta WS y en `POST /api/game/{roomId}/answer`.
- Un cliente que se conecta a mitad de ronda no recibe el `ROUND_STARTED` pasado (pub/sub sin
  replay); usa el fallback REST `GET /api/game/{roomId}/current-round`.

---

## 5. Entidad-Relación

Diagrama ER (UML/crow's foot). Cada microservicio con BD tiene su **propia** base Postgres (no hay
claves foráneas entre bases; los `room_id` / `user_id` / `bank_id` cruzados son referencias
lógicas). Los read-models de game-engine son proyecciones locales, pobladas consumiendo eventos de
RabbitMQ (ADR-004).

```mermaid
erDiagram
    %% ===== auth-service · auth-db =====
    USERS {
        varchar   id PK
        varchar   email UK
        varchar   username UK
        varchar   password_hash
        boolean   active
        timestamp registered_at
    }

    %% ===== concept-bank-service · conceptbank-db =====
    CONCEPT_BANKS {
        varchar id PK
        varchar name
        varchar subject
    }
    CONCEPTS {
        varchar id PK
        varchar bank_id FK
        text    question
        text    expected_answer
        int     difficulty
    }
    CONCEPT_BANKS ||--o{ CONCEPTS : contiene

    %% ===== game-engine-service · game-db =====
    ROUNDS {
        varchar   id PK
        varchar   room_id
        text      concept_question
        varchar   expected_answer
        int       difficulty
        bigint    duration_seconds
        varchar   status
        timestamp started_at
        timestamp ended_at
    }
    ANSWERS {
        varchar   id PK
        varchar   round_id FK
        varchar   user_id
        text      text
        timestamp submitted_at
        varchar   result
    }
    SESSION_RESULTS {
        varchar   id PK
        varchar   room_id
        varchar   user_id
        int       total_points
        int       correct_answers
        int       incorrect_answers
        bigint    total_time_ms
        timestamp completed_at
    }
    ROUNDS ||--o{ ANSWERS : recibe

    %% read-models locales (proyecciones vía RabbitMQ)
    ROOM_READ_MODEL {
        varchar room_id PK
        varchar concept_bank_id
        int     max_participants
        boolean game_started
    }
    PARTICIPANT_READ_MODEL {
        varchar id PK
        varchar room_id
        varchar user_id
    }
    ROOM_READ_MODEL ||--o{ PARTICIPANT_READ_MODEL : tiene
    CONCEPTBANK_READ_MODEL {
        varchar bank_id PK
        varchar name
        varchar subject
    }
    CONCEPT_READ_MODEL {
        varchar id PK
        varchar bank_id
        text    question
        text    expected_answer
        int     difficulty
    }
    CONCEPTBANK_READ_MODEL ||--o{ CONCEPT_READ_MODEL : contiene
```

**Nota:** `room-service` no tiene tablas de dominio relacionales — su estado (`Room` /
`Participant`) vive en Redis (AOF) y solo persiste una tabla `outbox_event` en `room-outbox-db`.
Los 4 servicios con BD comparten además una tabla `outbox_event` (id, tipo, payload,
correlation_id, estado) omitida arriba por brevedad.
