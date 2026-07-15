## **ESCUELA COLOMBIANA DE INGENIERÍA JULIO GARAVITO** 

Arquitecturas de Software - ARSW 2026-1 

## **Inception - ConceptArena** 

_Plataforma Web Multijugador de Estudio Colaborativo en Tiempo Real_ 

**Autor:** Stiven Esneider Pardo Gutiérrez  | **Fecha:** Junio 2026 

## **1. IDENTIFICACIÓN DEL PRODUCTO** 

| **Campo**                | **Detalle**                                     |
| --------------------------| -------------------------------------------------|
| **Nombre del producto**  | ConceptArena                                    |
| **Integrantes**          | Stiven Esneider Pardo Gutiérrez                 |
| **Curso**                | Arquitecturas de Software - ARSW 2026-1         |
| **Institución**          | Escuela Colombiana de Ingeniería Julio Garavito |
| **Fecha de elaboración** | Junio 2026                                      |



## **2. RESUMEN** 

ConceptArena es una aplicación web multijugador en tiempo real orientada al estudio colaborativo. Varios estudiantes se conectan desde cualquier navegador para participar en salas virtuales compartidas sin necesidad de descargas ni instalaciones. La dinámica central consiste en que el sistema presenta una definición, pista o enunciado académico y todos  los  participantes  intentan  escribir  la  respuesta  correcta  antes  de  que  expire  un  temporizador  compartido. Simultáneamente,  cada  sala  dispone  de  chat  de  voz  en  tiempo  real  para  facilitar  la  discusión,  coordinación  y retroalimentación oral entre compañeros. 

La propuesta combina gamificación con una estrategia pedagógica basada en _retrieval practice_ (recuperación activa), que consiste en forzar al estudiante a recordar conceptos desde la memoria en lugar de releerlos pasivamente. Desde la perspectiva técnica, el proyecto representa una oportunidad concreta para demostrar el uso coordinado de WebSockets, STOMP, WebRTC y arquitecturas de tiempo real con múltiples salas concurrentes, todos temas centrales del curso ARSW. 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 1 

## **3. DESCRIPCIÓN DEL PROYECTO** 

## **3.1 Antecedentes y Contexto** 

Una fracción importante de los estudiantes universitarios estudia conceptos, definiciones y relaciones temáticas mediante lectura repetitiva o resúmenes pasivos. La evidencia pedagógica señala que estas estrategias producen menor retención a largo plazo en comparación con la recuperación activa. Las sesiones de estudio en grupo suelen carecer de una estructura digital que combine participación simultánea de todos los integrantes, presión temporal moderada, retroalimentación inmediata y comunicación oral fluida. Herramientas como Kahoot o Quizlet Live abordan parcialmente esta necesidad, pero no integran voz en tiempo real ni permiten respuesta simultánea con validación centralizada. 

## **3.2 Problema que se Resuelve** 

Los grupos de estudio siguen dependiendo de métodos poco dinámicos con baja participación simultánea. Se pierde la oportunidad  de  combinar  competencia  amistosa  con  refuerzo  conceptual  real  y  los  estudiantes  no  obtienen retroalimentación inmediata en grupo sobre si dominan un concepto. 

## **3.3 Propuesta de Valor** 

ConceptArena transforma una mecánica de juego social en un método de estudio colaborativo adaptable a cualquier carrera cuyos contenidos se basen en conceptos, definiciones, clasificaciones, procesos o relaciones clave: salud, ingeniería, derecho, ciencias sociales, administración, entre otras. 

## **3.4 Diferenciadores Frente a Alternativas Existentes** 

|**Características**|**Herramientas existentes**|**ConceptArena**|
|---|---|---|
|**Recuperación activa**|Limitada o inexistente|Eje central del diseño|
|**Participación simultánea**|No siempre garantizada|Todos responden a la vez|
|**Sincronización en tiempo real**|Baja o sin servidor centralizado|Rondas sincronizadas por servidor|
|**Comunicación oral integrada**|No incluida generalmente|Chat de voz con WebRTC|
|**Enfoque colaborativo**|Variable|Alto, por salas compartidas|
|**Valor técnico ARSW**|Bajo|WebSockets + STOMP + WebRTC +<br>salas concurrentes|



## **3.5 Arquitectura General** 

La arquitectura se organiza en tres capas principales: el cliente web (React SPA), el servidor de aplicaciones (Spring Boot) y la capa de persistencia (PostgreSQL). El canal de voz peer-to-peer habilitado por WebRTC tiene su señalización sobre el mismo WebSocket del servidor. El estado en vuelo de cada ronda (temporizador, respuestas parciales) se mantiene en memoria para minimizar latencia. 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 2 

|**Capa**|**Tecnología**|**Justificación**|
|---|---|---|
|**Backend**|Java 17, Spring Boot 3|Base robusta para servicios REST y WebSocket|
|**Mensajería RT**|Spring WebSocket, STOMP|Mensajería estructurada para salas y rondas|
|**Frontend**|React JS, react-stomp-<br>hooks|Interfaz reactiva sin recarga de página|
|**Voz RT**|WebRTC (API nativa)|Audio P2P de baja latencia sin plugins|
|**Señalización**|WebSocket Spring Boot|Intercambio de offer/answer e ICE candidates|
|**Persistencia**|PostgreSQL|Usuarios, salas, bancos de conceptos y resultados|
|**Despliegue**|Railway / Render|Nivel gratuito suficiente para el prototipo académico|



## **4. HISTORIAS DE USUARIO** 

## **4.1 Epic y Feature Mapping** 

|**4.1 Epic y Feature Mapping**|||
|---|---|---|
|**Épica**|**Feature**|**HU asociadas**|
|E1-Gestión de usuarios|F1.1 Registro y autenticación|HU-01, HU-02|
|E2-Gestión de salas|F2.1 Lobby y creación de salas|HU-03, HU-04|
|E2-Gestión de salas|F2.2 Ingreso y abandono de sala|HU-05|
|E3-Juego colaborativo RT|F3.1 Rondas de conceptos|HU-06, HU-07|
|E3-Juego colaborativo RT|F3.2 Temporizador centralizado|HU-08|
|E3-Juego colaborativo RT|F3.3 Validación y puntajes|HU-09|
|E4-Comunicación oral RT|F4.1 Chat de voz WebRTC|HU-10, HU-11|
|E5-Bancos de conceptos|F5.1 Gestión de bancos|HU-12, HU-13|
|E6- Observabilidad|F6.1 Logs estructurados|HU-14|
|E6- Observabilidad|F6.2 Métricas y KPIs|HU-15|
|E6- Observabilidad|F6.3 Dashboard de métricas|HU-16|



Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 3 

## **4.2 Priorización MoSCOW** 

|**Prioridad**|**HUs**|
|---|---|
|**Must Have**|HU-01, HU-03, HU-06, HU-07, HU-08, HU-09, HU-12, HU-14, HU-15|
|**Should Have**|HU-02, HU-04, HU-05, HU-10, HU-11, HU-13, HU-16|
|**Could Have**|HU-Historial de desempeño individual, exportación de resultados|
|**Won't Have (MVP)**|Modos adaptativos de dificultad, integración docente de bancos externos|



## **4.3 Definition of Ready (DoR) - Aplicable a todas las HU** 

- La historia está redactada con el formato: Quien, Qué y Para qué. 

- Los criterios de aceptación están expresados en formato Gherkin (Given-When-Then). 

- La historia ha sido estimada por el equipo. 

- No existen dependencias bloqueantes sin resolver. 

- La historia está priorizada con MoSCOW. 

- Los diseños o mockups necesarios están disponibles. 

- Los criterios de rendimiento requeridos son conocidos (latencia, concurrencia). 

## **4.4 Definition of Done (DoD) - Aplicable a todas las HU** 

- El código ha sido revisado y aprobado mediante pull request. 

- Las pruebas unitarias e integración pasan al 100%. 

- Los criterios de aceptación Gherkin se verifican con pruebas automatizadas o manuales documentadas. 

- La funcionalidad está desplegada en el ambiente de pruebas. 

- Los logs estructurados de la funcionalidad están activos y visibles. 

- No existen bugs críticos abiertos relacionados con la historia. 

- La documentación técnica pertinente ha sido actualizada en la Wiki de Azure DevOps. 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 4 

## **4.5 Fichas de Historias de Usuario** 

**==> picture [511 x 546] intentionally omitted <==**

**----- Start of picture text -----**<br>
HU-01 - Registro de usuario<br>MoSCOW:  Must Have  |   Épica/Feature:  E1 - Gestión de usuarios / F1.1 Registro y autenticación<br>Quién: Un estudiante<br>Qué: registrarme con correo y contraseña<br>Para qué: acceder a la plataforma con una identidad persistente<br>Criterios de Aceptación (Gherkin):<br>Given  que estoy en la página de registro<br>When  ingreso correo válido y contraseña  ≥ 8  caracteres y presiono Registrar<br>Then  mi cuenta se crea y soy redirigido al lobby<br>Given  que ingreso un correo ya existente<br>When  presiono Registrar<br>Then  se muestra el error 'El correo ya está registrado'<br>HU-02 - Inicio de sesión<br>MoSCOW:  Should Have  |   Épica/Feature:  E1 - Gestión de usuarios / F1.1 Registro y autenticación<br>Quién: Un estudiante registrado<br>Qué: iniciar sesión con mis credenciales<br>Para qué: acceder a mis salas y datos personales<br>Criterios de Aceptación (Gherkin):<br>Given  que estoy en la página de login<br>When  ingreso correo y contraseña correctos<br>Then  soy autenticado y redirigido al lobby<br>Given  que ingreso credenciales incorrectas<br>When  presiono Ingresar<br>Then  se muestra el error 'Credenciales inválidas'<br>**----- End of picture text -----**<br>


Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 5 

## **HU-03 - Visualización del lobby en tiempo real** 

_**MoSCOW:** Must Have  |_ _**Épica/Feature:** E2 - Gestión de salas / F2.1 Lobby y creación de salas_ 

**Quién:** Un estudiante autenticado **Qué:** ver en tiempo real la lista de salas activas con su estado **Para qué:** elegir la sala más conveniente sin necesidad de recargar la página 

## **Criterios de Aceptación (Gherkin):** 

**Given** que estoy en el lobby **When** otro usuario crea una sala pública **Then** la sala aparece en mi lista en menos de 500 ms sin recargar 

**Given** que una sala alcanza su capacidad máxima **When** eso ocurre 

**Then** el estado de la sala en mi lobby cambia a 'Llena' en tiempo real 

## **HU-04 - Creación de sala de estudio** 

_**MoSCOW:** Should Have  |_ _**Épica/Feature:** E2 - Gestión de salas / F2.1 Lobby y creación de salas_ 

**Quién:** Un estudiante autenticado **Qué:** crear una sala pública o privada con un banco de conceptos específico **Para qué:** organizar sesiones de estudio dirigidas a una materia concreta 

## **Criterios de Aceptación (Gherkin):** 

**Given** que estoy en el lobby 

**When** selecciono Crear sala, elijo tipo (pública/privada) y un banco de conceptos y confirmo **Then** la sala se crea, aparece en el lobby y recibo el código de invitation si es privada 

**Given** que creo una sala privada **When** la sala se crea 

**Then** recibo un código único de 6 caracteres para compartir 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 6 

**HU-05 - Ingreso a sala de estudio** _**MoSCOW:** Should Have  |_ _**Épica/Feature:** E2 - Gestión de salas / F2.2 Ingreso y abandono de sala_ **Quién:** Un estudiante autenticado **Qué:** ingresar a una sala pública o privada (con código) **Para qué:** participar en una sesión de estudio colaborativo **Criterios de Aceptación (Gherkin): Given** que existe una sala pública disponible **When** hago clic en Unirse **Then** ingreso a la sala y veo la lista de participantes actualizada en tiempo real **Given** que tengo un código de sala privada **When** lo ingreso y presiono Ingresar **Then** accedo a la sala; si el código es inválido, veo el error correspondiente **HU-06 - Visualización del concepto activo en tiempo real** _**MoSCOW:** Must Have  |_ _**Épica/Feature:** E3-Juego colaborativo RT / F3.1 Rondas de conceptos_ **Quién:** Un estudiante dentro de una sala **Qué:** ver el concepto o pista académica de la ronda activa en tiempo real **Para qué:** saber qué debo responder sin depender de recargas manuales **Criterios de Aceptación (Gherkin): Given** que estoy en una sala y hay una ronda activa **When** el servidor publica el evento ROUND_START **Then** veo el enunciado del concepto y el temporizador en mi pantalla en menos de 200 ms **Given** que la ronda finaliza **When** el servidor publica ROUND_END **Then** veo el resumen de resultados de la ronda en menos de 200 ms 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 7 

## **HU-07 - Envío simultáneo de respuesta** 

_**MoSCOW:** Must Have  |_ _**Épica/Feature:** E3 Juego colaborativo RT / F3.1 Rondas de conceptos_ 

**Quién:** Un estudiante dentro de una sala activa **Qué:** enviar mi respuesta al concepto activo mientras el temporizador está corriendo **Para qué:** competir en igualdad de condiciones con los demás participantes 

## **Criterios de Aceptación (Gherkin):** 

**Given** que estoy en una sala con una ronda activa y el temporizador no ha expirado **When** escribo mi respuesta y presiono Enviar **Then** el servidor recibe mi intento, lo valida y me notifica el resultado en menos de 200 ms 

**Given** que el temporizador expira **When** intento enviar una respuesta **Then** el sistema rechaza el intento con el mensaje 'Tiempo agotado' 

## **HU-08 - Temporizador centralizado y sincronizado** 

_**MoSCOW:** Must Have  |_ _**Épica/Feature:** E3 Juego colaborativo RT / F3.2 Temporizador centralizado_ **Quién:** Un estudiante dentro de una sala activa **Qué:** ver el mismo temporizador que todos los demás participantes **Para qué:** garantizar equidad en el tiempo disponible para responder 

## **Criterios de Aceptación (Gherkin):** 

**Given** que hay una ronda activa **When** el servidor emite actualizaciones del temporizador **Then** todos los clientes en la sala muestran el mismo valor con una diferencia máxima de 100 ms 

**Given** que el temporizador llega a cero **When** eso ocurre **Then** el servidor publica ROUND_END y ningún cliente puede enviar más respuestas para esa ronda 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 8 

## **HU-09 - Ranking de ronda** 

_**MoSCOW:** Must Have  |_ _**Épica/Feature:** E3-Juego colaborativo RT / F3.3 Validación y puntajes_ 

**Quién:** Un estudiante dentro de una sala **Qué:** ver la clasificación de la ronda al finalizar cada ciclo **Para qué:** conocer mi desempeño relativo frente a los demás participantes 

## **Criterios de Aceptación (Gherkin):** 

**Given** que una ronda acaba de finalizar **When** el servidor publica ROUND_END 

**Then** veo la tabla de clasificación con nombre, respuesta correcta/incorrecta y puntaje acumulado 

**Given** que dos jugadores responden correctamente en la misma ronda **When** se calcula el ranking 

**Then** el que respondió más rápido ocupa la posición superior 

## **HU-10 - Chat de voz en tiempo real** 

_**MoSCOW:** Should Have  |_ _**Épica/Feature:** E4-Comunicación oral RT / F4.1 Chat de voz WebRTC_ 

**Quién:** Un estudiante dentro de una sala activa **Qué:** comunicarme por voz con los demás participantes de la sala **Para qué:** coordinar estrategias y discutir conceptos sin salir de la plataforma 

## **Criterios de Aceptación (Gherkin):** 

**Given** que estoy en una sala y activo el micrófono 

**When** el proceso de señalización WebRTC se completa 

**Then** escucho el audio de los demás participantes con latencia menor a 300 ms 

**Given** que un participante activa/desactiva su micrófono **When** eso ocurre 

**Then** el icono de estado de audio de ese participante se encuentra actualizado en menos de 500 ms para todos 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 9 

## **HU-11 - Reconexión al canal de voz** 

_**MoSCOW:** Should Have  |_ _**Épica/Feature:** E4-Comunicación oral RT / F4.1 Chat de voz WebRTC_ **Quién:** Un estudiante cuya conexión de voz se interrumpe **Qué:** reconectarme automáticamente al canal de audio de la sala **Para qué:** no perder la sesión de estudio por problemas transitorios de red **Criterios de Aceptación (Gherkin): Given** que mi conexión WebRTC se interrumpe **When** el sistema detecta la desconexión **Then** intenta reconectar automáticamente en menos de 5 s y me notifica del estado **Given** que la reconexión directa falla por NAT/firewall **When** eso ocurre **Then** el sistema usa el servidor TURN como intermediario y establece la conexión 

## **HU-12 - Carga de banco de conceptos** 

_**MoSCOW:** Must Have  |_ _**Épica/Feature:** E5-Bancos de conceptos / F5.1 Gestión de bancos_ **Quién:** Un estudiante autenticado **Qué:** seleccionar o crear un banco de conceptos por materia o tema al crear una sala **Para qué:** garantizar que las rondas sean relevantes para la asignatura que estamos estudiando 

## **Criterios de Aceptación (Gherkin):** 

**Given** que estoy creando una sala **When** selecciono un banco de conceptos existente **Then** la sala usa ese banco para generar las rondas **Given** que no existe un banco para mi materia **When** creo un banco nuevo con al menos 5 preguntas y lo guardo **Then** queda disponible para futuras salas 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 10 

**HU-13 - Consulta de resultados de sesión** _**MoSCOW:** Should Have  |_ _**Épica/Feature:** E5- Bancos de conceptos / F5.1 Gestión de bancos_ **Quién:** Un estudiante autenticado **Qué:** consultar el resumen de resultados de mis sesiones anteriores **Para qué:** hacer seguimiento a mi progreso y detectar conceptos donde debo mejorar **Criterios de Aceptación (Gherkin): Given** en que he participado en al menos una sesión **When** accedo a la sección Mis resultados **Then** veo una lista de sesiones con fecha, sala, banco usado y puntaje obtenido **Given** que selecciono una sesión específica **When** lo hago **Then** veo el detalle de cada ronda: concepto presentado, mi respuesta y si fue correcta 

## **5. HISTORIAS DE USUARIO DE OBSERVABILIDAD** 

## **HU-14 - Registro de logs estructurados** 

_**MoSCOW:** Must Have  |_ _**Épica/Feature:** E6- Observabilidad / F6.1 Logs estructurados_ **Quién:** El sistema (componente de backend) **Qué:** registrar todos los eventos relevantes del juego en formato estructurado (JSON) **Para qué:** facilitar el diagnóstico de errores, auditoría de partidas y análisis de comportamiento **Criterios de Aceptación (Gherkin): Given** que un jugador envía una respuesta **When** el backend procesa el evento **Then** se registra un log JSON con: timestamp, salaId, userId, rondaId, respuesta, resultado, latencia_ms **Given** que ocurre un error de validación de respuesta **When** el backend lo detecta **Then** se genera un log con nivel ERROR y el stack trace correspondiente **DoR adicional:** El esquema JSON de los logs está definido y acordado antes de iniciar desarrollo. **DoD adicional:** Los logs son consultables en el ambiente de pruebas y no contienen datos sensibles en texto plano. 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 11 

**HU-15 - Captura de métricas técnicas y KPIs de negocio** _**MoSCOW:** Must Have  |_ _**Épica/Feature:** E6- Observabilidad / F6.2 Métricas y KPIs_ **Quién:** El equipo de desarrollo **Qué:** capturar automáticamente métricas técnicas y de negocio durante la operación del sistema **Para qué:** medir el rendimiento real y verificar los indicadores de éxito del Business Case **Criterios de Aceptación (Gherkin): Given** que el sistema está en operación **When** se procesa cualquier evento de ronda **Then** se actualizan las métricas: salas_activas, usuarios_concurrentes, latencia_respuesta_ms, rondas_completas **Given** que termina una sesión de sala **When** el servidor cierra la sala **Then** se persisten los KPIs de esa sesión: tasa_aciertos, tiempo_promedio_respuesta, usuarios_que_respondieron 

## **HU-16 - Visualización de métricas en dashboard** 

_**MoSCOW:** Should Have  |_ _**Épica/Feature:** E6- Observabilidad / F6.3 Dashboard de métricas_ **Quién:** El equipo de desarrollo **Qué:** visualizar en un dashboard las métricas técnicas y KPIs de negocio en tiempo real **Para qué:** monitorear la salud del sistema y el comportamiento de los usuarios de forma centralizada **Criterios de Aceptación (Gherkin): Given** que el dashboard está abierto **When** hay actividad en el sistema **Then** los gráficos se actualizan automáticamente mostrando: salas activas, usuarios concurrentes, latencia promedio y tasa de aciertos por ronda **Given** que la latencia de respuesta supera 500 ms **When** eso ocurre **Then** el dashboard muestra una alerta visual en el panel correspondiente 

## **6. KPIS DE NEGOCIO (ALINEADOS CON EL BUSINESS CASE)** 

|**KPI**|**Descripción**|**Método de medición**|**Meta**|
|---|---|---|---|
|**KPI-1**|Latencia de ronda|Tiempo desde que el servidor publica<br>ROUND_START hasta que llega a todos los<br>clientes.|**_< 200 ms_**|



Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 12 

|**KPI**|**Descripción**|**Método de medición**|**Meta**|
|---|---|---|---|
|**KPI-2**|Sincronía del temporizador|Diferencia máxima del valor del temporizador<br>entre clientes en la sala por comparación de<br>logs.|**_< 100 ms_**|
|**KPI-3**|Capacidad concurrente|Número máximo de salas activas<br>simultáneamente sin degradación de carga<br>funcional.|**_≥ 50 salas_**|
|**KPI-4**|Tasa de aciertos por sesión|Porcentaje de rondas en las que al menos un<br>jugador responde correctamente.|**_> 70%_**|
|**KPI-5**|Disponibilidad del canal de voz|Porcentaje de sesiones en que el chat de voz<br>WebRTC se establece con éxito.|**_> 95%_**|
|**KPI-6**|Tiempo de carga inicial|Tiempo desde la primera solicitud HTTP hasta<br>que la interfaz es totalmente interactiva<br>(DevTools).|**_< 2 s_**|



## **7. INTEGRACIÓN DE SERVICIO MODERNO (BONO)** 

Se propone integrar un componente de Procesamiento de Lenguaje Natural (NLP) para la validación semántica de respuestas. En lugar de comparación exacta de cadenas, el servidor utilizará un modelo de similitud semántica (por ejemplo, _sentence-transformers_ vía  una  API  REST  ligera)  para  determinar  si  la  respuesta  del  estudiante  es conceptualmente correcta aunque no coincida palabra a palabra con la respuesta esperada. 

Valor agregado a las historias de usuario: 

- **HU-09 (Validación de respuestas):** el puntaje ya no penaliza errores ortográficos menores ni sinónimos válidos. 

- **HU-06 (Concepto activo):** se puede generar retroalimentación automática explicando por qué la respuesta fue marcada correcta o incorrecta. 

- **HU-15 (KPIs):** se puede registrar el grado de similitud semántica promedio de las respuestas como indicador de dominio conceptual. 

La integración no es un experimento aislado: impacta directamente la mecánica central del juego y mejora la experiencia del usuario final al reducir la frustración por respuestas correctas rechazadas por diferencias superficiales de redacción. 

## **8. TRAZABILIDAD CON EL BUSINESS CASE** 

Todas las historias de usuario y KPIs de este Inception se derivan directamente del Business Case ConceptArena (ARSW 2026-1). La tabla siguiente muestra la trazabilidad directa. 

Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 13 

|**Sección del Business Case**|**Elemento en el Inception**|
|---|---|
|Sección 3.3 - Indicadores de éxito|KPI-1 a KPI-6 (Sección 6)|
|Sección 4.2 - MVP|HU-03, 04, 05, 06, 07, 08, 09, 10, 12|
|Sección 5.1 - Arquitectura|Stack tecnológico en HU-06, 07, 08, 10, 11|
|Sección 5.2 - Flujo de ronda|HU-06, 07, 08, 09|
|Sección 5.3 - Flujo WebRTC|HU-10, HU-11|
|Sección 8 - Riesgos|DoR/DoD de HU-08, 10, 11 (mitigación en criterios de<br>aceptación)|
|||



Stiven Esneider Pardo Gutiérrez - Junio 2026 

Página 14 

