# Backend — Estado

Todos los ítems críticos, altos y medios de la lista original están resueltos y cubiertos por tests (`mvn test` → 99 tests, BUILD SUCCESS). Quedan fuera de esta pasada, por decisión explícita, los dos ítems de alcance mayor:

- **Buses persistentes (Post-MVP)** — `InMemoryCommandBus`/`InMemoryEventBus` siguen en memoria. Migrar a RabbitMQ/Kafka/DB-backed es un cambio de infraestructura fuera del alcance de esta pasada.
- **Conexión del frontend al backend real** — el mock en `mock/` sigue sin `axios`/`@stomp/stompjs`. Es trabajo de frontend, no de backend.

## Resuelto

1. **Validación de respuestas (NLP)** — `RestNlpAdapter` ahora implementa `AnswerValidationPort` y compara de forma normalizada (minúsculas, sin tildes, espacios colapsados). Se invoca de forma síncrona desde `SubmitAnswerCommandHandler` (no como listener async) para evitar una condición de carrera con `GameSaga` al calcular el resultado de fin de ronda anticipado.
2. **Race condition en fin de ronda** — se creó el puerto `TimerPort` (app) implementado por `ScheduledTimerAdapter` (infra). `GameSaga.triggerEarlyRoundEnd()` y `onRoomLeft()` ahora cancelan el timer programado.
3. **WebSocket handlers raw** — `RawWebSocketConfig` (web) registra `GameWebSocketHandler`, `LobbyWebSocketHandler`, `SignalingWebSocketHandler` en `/ws/game`, `/ws/lobby`, `/ws/signaling`, coexistiendo con el broker STOMP en `/ws`.
4-6. **Tests** — unit tests de dominio (`core`), de command handlers + `GameSaga` con Mockito (`app`), del `RestNlpAdapter` (`infra`), `@WebMvcTest` de los 4 controladores REST (`web`), e integration test de flujo completo registro→sala→ronda→puntaje (`bootstrap`).
7. **Validaciones** — password vacío en login (ahora devuelve 400 en vez de 500), longitud máxima de respuesta, longitud/caracteres de nombre de sala (`Room.create`).
8. **Rate limiting** — `RateLimitingFilter` (10 req/min por IP) en `/api/auth/login` y `/api/auth/register`.
9. **Seguridad** — JWT secret externalizado a `JWT_SECRET` (obligatorio en prod, con default solo en dev), CSRF disable documentado (auth stateless por Bearer token, no aplica).
10. **Endpoints faltantes** — `GET /api/rooms/{id}` y `GET /api/concept-banks/{id}` (ambos omiten datos sensibles: inviteCode y expectedAnswer respectivamente).
11. **Código muerto** — eliminados `StompProperties` y `PageResponse<T>`; `RoomQueryServiceImpl`/`RoomRepositoryImpl` usan el enum `RoomStatus` en vez de strings hardcodeadas.

## Bugs adicionales encontrados y corregidos durante esta pasada

Estos no estaban en la lista original pero bloqueaban el flujo completo o eran vulnerabilidades reales, descubiertos al escribir el integration test end-to-end:

- **`EntityId.from(String)` no existía** — los 4 mappers JPA no compilaban. Se agregó el factory method.
- **Bean duplicado `ObservabilityEventHandlers`** — existía en `app.event` (stub placeholder) e `infra.observability` (implementación real de HU-14). Colisión de nombre de bean rompía el arranque completo de la app. Se eliminó el stub.
- **Creador de sala invisible para `GameSaga`** — `CreateRoomCommandHandler` añadía al creador como participante pero solo publicaba `RoomCreated`, nunca `RoomJoined`. Con exactamente 2 usuarios reales (creador + 1) la ronda nunca arrancaba. Ahora también publica `RoomJoined`.
- **`LazyInitializationException` sistémica** — `RoundRepositoryImpl`, `RoomRepositoryImpl`, `ConceptBankRepositoryImpl` y sus query services leían colecciones `@OneToMany` lazy después de que la transacción implícita de Spring Data JPA ya había cerrado la sesión de Hibernate. Rompía toda lectura contra una BD real (submit answer, listar salas, listar bancos). Se agregó `@Transactional` a los métodos de lectura/escritura de los tres repositorios.
- **Control de acceso roto en `/api/concept-banks` y `/api/rooms`** — `permitAll()` no restringía por método HTTP, permitiendo `POST` (crear) sin autenticación, no solo el `GET` público que decía el comentario. Ahora `permitAll()` es explícitamente GET-only para esas rutas.
