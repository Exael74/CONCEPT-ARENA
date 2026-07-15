# Auditoría Técnica — ConceptArena (Microservicios + Eventos + Shared Kernel)

Actúa como un Arquitecto de Software Principal haciendo una auditoría
técnica COMPLETA, EXTREMADAMENTE ESTRICTA y sin filtros de cortesía del
proyecto "ConceptArena" (6 microservicios: api-gateway, auth-service,
room-service, game-engine-service, concept-bank-service,
voice-signaling-service; RabbitMQ con patrón Outbox; PostgreSQL por
servicio que persista; WebSocket/STOMP; Shared Kernel DDD; frontend
React/Vite excluido de esta auditoría). Evalúa el código REAL del
proyecto, no lo que se planeó en los prompts. Si algo solo fue
especificado pero nunca se verificó en código, trátalo como NO
VERIFICADO, nunca como CUMPLE.

Da veredicto por cada punto: ✅ CUMPLE / ⚠️ PARCIAL / ❌ NO CUMPLE /
🚫 NO EXISTE, siempre citando la ruta exacta de archivo como evidencia.

═══════════════════════════════════════════
PARTE 0 — ESTRUCTURA DEL PROYECTO
═══════════════════════════════════════════

Estructura de referencia a contrastar:

```
conceptarena/
├── docker-compose.yml, .env.example, README.md
├── docs/ (event-contracts.md, architecture-decisions.md)
├── conceptarena-kernel/  ← Shared Kernel (DDD)
│   ├── src/main/java/com/conceptarena/kernel/
│   │   ├── BaseEntity.java, ValueObject.java, DomainEvent.java, Command.java
│   │   ├── EntityId.java, Email.java, PasswordHash.java
│   │   └── DomainException.java, DuplicateEmailException.java, InvalidCredentialsException.java
│   └── pom.xml (sin dependencias Spring, solo Java)
├── auth-service/
│   ├── model, repository, dto, security, messaging/publisher, messaging/config, service, controller, exception
│   └── pom.xml (Spring Boot 3, depende de kernel, spring-security, jjwt)
├── room-service/
│   ├── model, websocket, manager, dto, controller (SIN repository, SIN JPA — estado en memoria)
│   └── pom.xml (Spring Boot 3, depende de kernel)
├── game-engine-service/
│   ├── model, repository, websocket, engine, messaging/publisher, messaging/config, dto, service, controller
│   └── pom.xml (Spring Boot 3, depende de kernel, JPA, PostgreSQL)
├── concept-bank-service/
│   ├── model, repository, dto, controller
│   └── pom.xml (Spring Boot 3, depende de kernel, JPA, PostgreSQL)
├── voice-signaling-service/
│   ├── websocket, dto, controller
│   └── pom.xml (Spring Boot 3, depende de kernel)
├── api-gateway/ (Spring Cloud Gateway o similar)
├── observability/ (loki-config, promtail-config, grafana/)
└── frontend/ (React/Vite — fuera del alcance de esta auditoría)
```

0.1 **Shared Kernel (conceptarena-kernel)** — ¿existe como módulo
    independiente sin dependencias Spring? ¿Los demás servicios lo
    importan como dependencia Maven? ¿O las clases base están
    duplicadas en cada servicio? Las clases como EntityId, Email,
    PasswordHash, DomainEvent, Command deberían vivir en el kernel.
    Si están duplicadas en cada servicio, ❌ GRAVE.

0.2 **Límites entre microservicios (Database-per-Service)** — ¿algún
    servicio importa clases Java de otro servicio directamente en su
    classpath? Si sí, ❌ GRAVE, rompe el aislamiento fundamental.
    Confirma que los DTOs de eventos (ej: UserRegisteredEvent,
    RoundStartedEvent) están DUPLICADOS a propósito en cada servicio,
    no compartidos vía un módulo "common".

0.3 **room-service** — confirma que NO tiene repository/ ni dependencia
    JPA. Si la tiene, contradice la decisión de que vive 100% en
    memoria.

0.4 **OutboxEvent** — cada servicio con BD (auth-service,
    game-engine-service, concept-bank-service) debe tener su propia
    clase OutboxEvent y su tabla outbox. Confirma si existe o no.

0.5 **Capas internas de cada servicio** — model/ solo entidades,
    repository/ solo interfaces de queries, dto/ solo transferencia sin
    lógica, service/ solo lógica de negocio, controller/ solo mapeo
    HTTP/STOMP. Señala fat controllers, lógica de negocio en DTOs, o
    validaciones de negocio (no de formato) mal ubicadas.

0.6 **messaging/ siempre subdividido en publisher/ y config/**, nunca
    plano. Señala violaciones.

0.7 **Consistencia de naming** — mismo patrón de paquetes
    `com.conceptarena.{servicio}.*` y misma versión Spring Boot en
    todos los POMs de los servicios.

0.8 **Archivos/carpetas ad-hoc sin categoría clara** — señal de
    degradación estructural.

Da un % de fidelidad estructural.

═══════════════════════════════════════════
PARTE 1 — ESCALABILIDAD
Usa EXACTAMENTE las 6 métricas: RPS, p95/p99, Error Rate, CPU/Memoria,
DB Queries, Queue Lag.
═══════════════════════════════════════════

1.1 Para cada una de las 6 métricas: ¿el proyecto la expone HOY?
    ¿Con qué herramienta? Si no se expone, ❌ explícito, sin excusas.
    - RPS: ¿algún servicio mide requests/segundo (Micrometer Counter)?
    - p95/p99: ¿hay algún histograma de latencia (Micrometer Timer con
      publishPercentiles) configurado, o solo Timer simple?
    - Error Rate: ¿los GlobalExceptionHandler incrementan algún
      contador medible, o solo loguean?
    - CPU/Memoria: ¿Actuator expone /actuator/metrics scrapeado por
      Prometheus/Grafana, o solo /actuator/health superficial?
    - DB Queries: ¿hay logging de slow queries configurado en
      PostgreSQL?
    - Queue Lag: ¿el panel de RabbitMQ está siendo scrapeado hacia
      Grafana o es solo un panel manual que nadie mira?

1.2 Escenarios aplicados al proyecto real:
    a) ¿Existe ALGÚN test de carga (k6/JMeter/Artillery) contra
       game-engine-service con 30 jugadores simulados? Si no existe
       ninguno, dilo así: "0% de escalabilidad validada
       empíricamente, todo es diseño teórico sin prueba".
    b) Si p95 sube pero CPU está normal, ¿el proyecto puede distinguir
       si el cuello está en BD/red/locks? Necesita ambas métricas
       simultáneas — confirma si existen.
    c) Si Queue Lag creciera sin detenerse en algún servicio, ¿hay
       alguna alerta configurada, o el sistema se degradaría
       silenciosamente?

1.3 Cuellos de botella conocidos y sospechosos:
    a) game-engine-service con ActiveGameState en memoria — si se
       corrieran 2+ instancias detrás de un balanceador, ¿el diseño
       lo soporta? Si el estado no está externalizado (Redis), NO
       categórico, con el fallo exacto explicado.
    b) room-service — mismo problema con LobbyManager en memoria,
       límite duro de escalabilidad horizontal.
    c) Las BD sin réplicas de lectura — si el ranking global generara
       carga alta, ¿hay cache o réplica? Si no, ❌ explícito.

1.4 Auto-scaling — confirma si existe CUALQUIER mecanismo, ni siquiera
    `deploy.replicas` en docker-compose. Si no hay ninguno, dilo así.

Da un % de escalabilidad real.

═══════════════════════════════════════════
PARTE 2 — OBSERVABILIDAD
═══════════════════════════════════════════

2.1 Las 3 señales por separado con evidencia de archivo real:
    - Logs: ¿los 6 microservicios usan Logback JSON de forma
      consistente, o solo algunos?
    - Métricas: ¿el dashboard de KPIs funciona REALMENTE en Grafana
      con datos, o solo se generó configuración sin verificar que los
      paneles muestren datos reales?
    - Trazas: ningún prompt pidió trazas distribuidas
      (Zipkin/Jaeger/OpenTelemetry). Marca esto ❌ NO IMPLEMENTADO —
      sin esto es imposible seguir una request completa a través de
      los 6 microservicios cuando algo falla.

2.2 CorrelationId — ¿se PROPAGA realmente entre microservicios (header
    HTTP o parte del payload de eventos RabbitMQ), o cada uno genera
    el suyo aislado sin relación con la request original? Si es lo
    segundo, ⚠️ PARCIAL explícito.

2.3 SLI/SLO/Error budget — ¿existe ALGÚN SLO numérico documentado
    (ej: "99% de rondas iniciadas en <200ms")? Si no, ❌ NO CUMPLE.

2.4 Alertas accionables — ¿Grafana tiene reglas de alerta configuradas
    para RabbitMQ caído, alguna BD caída, o p95 sobre umbral? Si solo
    hay dashboards visuales sin ninguna alerta automática, dilo:
    "Observabilidad PASIVA, no ACTIVA".

2.5 MTTD/MTTR — ¿puedes estimar hoy cuánto tardarías en detectar que
    game-engine-service dejó de consumir eventos? Si depende de
    revisión manual sin alerta, el MTTD real es "indefinido", dilo así
    de crudo.

Da un % de observabilidad real.

═══════════════════════════════════════════
PARTE 3 — SEGURIDAD EN RESPUESTAS (ANTI-TRAMPA)
═══════════════════════════════════════════

3.1 **AnswerValidator** (análogo a un validador de movimiento):
    a) ¿Calcula validez de la respuesta comparando contra
       expectedAnswer Y verificando que el temporizador no haya
       expirado? O solo valida el texto sin considerar la ventana
       temporal (no detectaría respuestas enviadas después del
       tiempo límite)?
    b) ¿Usa el timestamp del CLIENTE o del SERVIDOR para determinar
       cuándo se envió la respuesta? Si usa el del cliente sin
       validar contra el reloj del servidor, señala esto como
       vulnerabilidad real.
    c) ¿Hay rate limiting real de mensajes STOMP/respuestas (ej: 3
       respuestas/segundo por usuario) con código que lo haga cumplir,
       o solo quedó como número en un comentario sin enforcement?
       Si no hay enforcement, ❌ explícito: un cliente malicioso
       podría saturar el servicio sin límite.

3.2 **TimerConsistencyValidator** — ¿el servidor es la ÚNICA fuente de
    tiempo? ¿Se cancela el timer programado cuando todos los
    participantes responden antes del fin (early end), o existe una
    carrera donde el timer y el early-end publican RoundEnded
    duplicado?

3.3 Autorización sobre WebSocket:
    a) ¿Valida en CADA mensaje que el userId del Principal coincida
       con el userId que dice enviar la respuesta, o confía ciegamente
       en la sesión inicial? Si un jugador pudiera enviar respuestas
       con el userId de otro dentro de la misma sesión, es una
       vulnerabilidad grave — verifícalo con el código exacto.
    b) ¿Se valida que el userId pertenezca realmente al roomId al que
       envía el mensaje, o cualquiera podría enviar respuestas a
       cualquier sala?

3.4 **Idempotencia en eventos** — ¿los listeners de eventos del bus
    (UserRegistered, GameSessionFinished) manejan duplicados o
    procesarían el mismo evento dos veces causando estado
    inconsistente?

Da un % de seguridad en respuestas — trata cualquier "no verificado"
como vulnerabilidad potencial hasta que el código demuestre lo
contrario.

═══════════════════════════════════════════
PARTE 4 — CONSISTENCIA DE CONTRATOS Y EVIDENCIA DE PRUEBAS
(verifica que lo documentado coincida con el código, y que lo que "se
implementó" tenga prueba real, no solo exista)
═══════════════════════════════════════════

4.1 **docs/event-contracts.md vs código real** — para CADA evento
    (UserRegisteredEvent, GameSessionFinishedEvent, RoomCreatedEvent,
    RoomClosedEvent): ¿el eventType string, el routing key (RabbitMQ),
    y la estructura del payload en el código coinciden EXACTAMENTE con
    lo documentado? Lista cualquier discrepancia encontrada, por mínima
    que sea (ej: un campo renombrado, un routing key con typo).
    Si no existe docs/event-contracts.md, 🚫 NO EXISTE.

4.2 **Cobertura de tests real** — cuenta cuántos archivos de test
    (@Test de JUnit) existen realmente en cada microservicio vs
    cuántos se necesitan. Si se pidieron "tests unitarios para
    AnswerValidator, ScoringService, GameSaga, TimerConsistency" pero
    solo existe un test parcial o ninguno, dilo explícitamente con el
    conteo real: "se requieren N clases de test, existen 0".

4.3 **Idempotencia verificada** — para UserRegisteredListener y
    GameSessionFinishedListener, ¿existe un test que específicamente
    envíe el mismo evento dos veces y confirme que NO se duplica el
    efecto? Si el código tiene la lógica de idempotencia pero NUNCA se
    probó automáticamente, márcalo ⚠️ PARCIAL: "la idempotencia está
    implementada pero no verificada por test, es una promesa sin
    prueba".

4.4 **Prueba de resiliencia real** — ¿existe algún script de test de
    integración (docker-compose + verificación) que se haya ejecutado
    al menos una vez con resultado documentado? Diferencia claramente
    "el script existe" de "el script se corrió y pasó".

4.5 **Manejo de errores consistente** — ¿los 6 microservicios usan el
    mismo formato de ErrorResponse (mismos campos: message, field,
    timestamp), o cada uno inventó su propio formato de error? Un
    frontend que consume 6 formatos de error distintos es un defecto
    de consistencia de API.

4.6 **Versionado de dependencias** — ¿los POMs de todos los servicios
    usan la MISMA versión de Spring Boot, Java, y librerías
    compartidas (jjwt, resilience4j, lombok)? Si hay versiones
    distintas sin razón documentada, señala el riesgo.

Da un % de consistencia de contratos y pruebas.

═══════════════════════════════════════════
PARTE 5 — INTERCONEXIONES ENTRE TODOS LOS REQUISITOS
═══════════════════════════════════════════

Identifica explícitamente los puntos donde:
- Un problema de estructura (Parte 0) CAUSA un problema de
  escalabilidad o seguridad (ej: estado en memoria mal aislado
  bloqueando escalado horizontal).
- Falta de rate limiting (Seguridad) ES TAMBIÉN un problema de
  escalabilidad (vector de DoS).
- Falta de observabilidad hace invisible un problema de seguridad
  (ej: sin métrica de "respuestas rechazadas por AnswerValidator",
  nunca sabrías si alguien intenta hacer trampa ahora mismo).
- Falta de tests (Parte 4) significa que cualquier afirmación de
  cumplimiento en las Partes 1-3 es, en rigor, solo una promesa de
  diseño, no un hecho verificado.
- La ausencia de Shared Kernel (Parte 0) fuerza duplicación de código
  base entre servicios, incrementando riesgo de inconsistencias y
  bugs.

═══════════════════════════════════════════
PARTE 6 — VEREDICTO FINAL Y PLAN DE ACCIÓN
═══════════════════════════════════════════

1. Porcentajes individuales: Estructura, Escalabilidad, Observabilidad,
   Seguridad en respuestas, Consistencia de contratos/pruebas.
2. Porcentaje global ponderado: Estructura 15%, Escalabilidad 20%,
   Observabilidad 20%, Seguridad en respuestas 30%, Consistencia de
   contratos/pruebas 15%.
3. Lista de los 7 gaps más críticos de TODO el proyecto (no por
   sección, sino los 7 más graves en conjunto), ordenados por
   prioridad, cada uno con: archivo/componente exacto, por qué es
   grave, y el cambio de código específico necesario (nunca genérico
   tipo "mejorar observabilidad" — siempre concreto, ej: "agregar
   validación de respuesta llamando a answer.markCorrect() en
   RestNlpAdapter.java línea 29 y exponer métrica
   game.answer.validated en /actuator/prometheus").
4. NO cierres con frases motivacionales. Termina con la lista de
   prioridades y nada más.
