## BUSINESS CASE ConceptArena

Plataforma Web Multijugador de Estudio Colaborativo en Tiempo Real

Arquitecturas de Software – ARSW 2026-1 Escuela Colombiana de Ingeniería Julio Garavito

Junio 2026

STIVEN ESNEIDER PARDO GUTIERREZ


## 1. Resumen Ejecutivo

ConceptArena es una plataforma web multijugador en tiempo real orientada al estudio colaborativo. Varios estudiantes se conectan desde cualquier navegador para participar en salas virtuales compartidas, sin necesidad de descargas ni instalaciones. La dinámica central consiste en que el sistema presenta una definición, pista o enunciado académico y todos los participantes intentan escribir la respuesta correcta antes de que expire un temporizador compartido. Simultáneamente, cada sala dispone de chat de voz en tiempo real para facilitar la discusión, coordinación y retroalimentación oral entre compañeros.

La propuesta combina gamificación con una estrategia pedagógica basada en retrieval practice (recuperación activa), que consiste en forzar al estudiante a recordar conceptos desde la memoria en lugar de releerlos pasivamente. Desde la perspectiva técnica, el proyecto representa una oportunidad concreta para demostrar el uso coordinado de WebSockets, STOMP, WebRTC y arquitecturas de tiempo real con múltiples salas concurrentes, todos temas centrales del curso ARSW.

## 2. Problema y Oportunidad

## 2.1 Contexto actual

Una fracción importante de los estudiantes universitarios estudia conceptos, definiciones y relaciones temáticas mediante lectura repetitiva o resúmenes pasivos. La evidencia pedagógica señala que estas estrategias producen menor retención a largo plazo en comparación con la recuperación activa, donde el estudiante debe traer a la memoria la información sin tenerla frente a él.

Adicionalmente, las sesiones de estudio en grupo suelen carecer de una estructura digital que combine participación simultánea de todos los integrantes, presión temporal moderada, retroalimentación inmediata y comunicación oral fluida. Herramientas como Kahoot o Quizlet Live abordan parcialmente esta necesidad, pero no integran voz en tiempo real ni permiten respuesta simultánea con validación centralizada.

## 2.2 Impacto de no resolverlo

- Los grupos seguirán dependiendo de métodos poco dinámicos con baja participación simultánea.

- Se pierde la oportunidad de combinar competencia amistosa con refuerzo conceptual real.

- Los estudiantes no tienen retroalimentación inmediata en grupo sobre si dominan un concepto.

## 2.3 Oportunidad de innovación

ConceptArena propone transformar una mecánica de juego social en un método de estudio colaborativo adaptable a cualquier carrera cuyos contenidos se basen en conceptos, definiciones, clasificaciones, procesos o relaciones clave: salud, ingeniería, derecho, ciencias sociales, administración, entre otras. Esto amplía el mercado potencial más allá de un único programa académico.


## 3. Objetivos del Proyecto

## 3.1 Objetivo general

Diseñar e implementar una aplicación web multijugador en tiempo real orientada al estudio colaborativo, basada en Spring Boot, WebSockets con STOMP, React y WebRTC, que permita a grupos de estudiantes practicar conceptos académicos mediante rondas sincronizadas de respuesta escrita y comunicación por voz en salas compartidas.

## 3.2 Objetivos específicos

- Implementar un sistema de salas públicas y privadas con acceso simultáneo desde distintos navegadores.

- Desarrollar un servidor WebSocket con Spring Boot y STOMP que gestione el estado de cada sala: concepto activo, temporizador, respuestas, puntajes y progreso de ronda.

- Integrar chat de voz en tiempo real mediante WebRTC con señalización sobre WebSocket.

- Construir una interfaz en React que muestre en tiempo real el concepto activo, el temporizador, las respuestas enviadas y la clasificación de la ronda.

- Garantizar que el temporizador y la validación de respuestas sean controlados exclusivamente por el servidor para mantener consistencia entre clientes.

- Permitir el uso de bancos de conceptos por materia o carrera para adaptar la herramienta a distintos contextos académicos.

## 3.3 Indicadores de éxito

| Indicador | Meta | Método de medición |
| --- | --- | --- |
| Latencia de actualización de ronda | < 200 ms | Pruebas con múltiples clientes |
| Sincronización del temporizador | 100% consistente entre | Logs del servidor y validación |
|   | clientes | funcional |
| Salas concurrentes activas | 5 salas simultáneas | Prueba funcional concurrente |
| Usuarios simultáneos | 20 usuarios | Prueba de carga |
| Calidad del chat de voz | Comunicación estable en sala | Pruebas de conexión y reconexión |
| Tiempo de carga inicial | < 3 segundos | Lighthouse / DevTools |
| Participación simultánea | Todos los jugadores responden | Sesiones de prueba |
|   | en la misma ronda |   |

## 4. Alcance de la Solución

## 4.1 Sistema de salas de estudio


El núcleo del sistema es un conjunto de salas en tiempo real donde los estudiantes pueden reunirse para practicar conceptos de una materia específica. Cada sala mantiene su propio estado aislado: lista de participantes, banco de conceptos asociado, ronda activa, temporizador, respuestas enviadas, puntajes acumulados, ranking y estado del canal de voz.

El lobby permite crear salas públicas o privadas (acceso por código). Una vez dentro, los estudiantes ven el concepto o pista académica, responden por escrito en tiempo real y pueden conversar por voz mientras la ronda está activa, convirtiendo la experiencia en una combinación de juego, estudio guiado y práctica colaborativa.

## 4.2 MVP – Funcionalidades incluidas

| Funcionalidad | Descripción |
| --- | --- |
| Lobby en tiempo real | Visualización de salas de estudio activas y estado de cada sala |
| Salas públicas y privadas | Acceso libre o por código de sala |
| Rondas por conceptos | El sistema muestra una definición, pista o enunciado académico |
| Respuesta simultánea | Todos los estudiantes pueden intentar responder al mismo tiempo |
| Temporizador compartido | El servidor controla el tiempo restante de cada ronda |
| Validación de respuestas | El backend valida la respuesta correcta y asigna puntos |
| Ranking de ronda | Clasificación por aciertos y velocidad de respuesta |
| Chat de voz en tiempo real | Comunicación oral entre compañeros mediante WebRTC |
| Señalización WebRTC | Intercambio de offer, answer e ICE candidates vía WebSocket/Spring |
| Banco de conceptos | Conjuntos de preguntas organizados por materia o tema |

## 4.3 Funcionalidades futuras (fuera de alcance del MVP)

- Modos de estudio por carrera y dificultad adaptativa.

- Historial de desempeño individual y retroalimentación por categoría temática.

- Integración con bancos de preguntas personalizados subidos por el docente.

- Analítica de avance por tema para uso como apoyo sistemático en cursos universitarios.

- Exportación de resultados de sesión.

## 4.4 Diferenciadores frente a alternativas existentes

| Característica | Herramientas pasivas / parciales ConceptArena |   |
| --- | --- | --- |
| Recuperación activa de | Limitada o inexistente | Sí, eje central del diseño |
| conceptos |   |   |
| Participación simultánea de | No siempre garantizada | Sí, todos responden a la vez |
| todos |   |   |


| Característica | Herramientas pasivas / parciales ConceptArena |   |
| --- | --- | --- |
| Sincronización en tiempo real | Baja o sin servidor centralizado | Alta, con rondas sincronizadas por |
|   |   | servidor |
| Comunicación oral integrada | No incluida generalmente | Sí, chat de voz con WebRTC |
| Enfoque colaborativo | Variable según herramienta | Alto, por salas de estudio compartidas |
| Valor técnico en ARSW | Bajo | Alto: WebSockets + STOMP + |
|   |   | WebRTC + salas concurrentes |

## 5. Arquitectura del Sistema

La arquitectura de ConceptArena se organiza en tres capas principales que se comunican de forma asíncrona: el cliente web (React), el servidor de aplicaciones (Spring Boot) y la capa de persistencia (PostgreSQL). A estas tres capas se suma el canal de voz peer-to-peer habilitado por WebRTC, cuya señalización también pasa por el servidor.

## 5.1 Vista general de componentes

Frontend – React (SPA): interfaz de usuario que se ejecuta en el navegador. Se comunica con el servidor a través de dos canales distintos: (1) HTTP/REST para operaciones puntuales como autenticación, creación de sala o carga del banco de conceptos; y (2) WebSocket con STOMP para todos los eventos de tiempo real: actualizaciones de ronda, sincronización del temporizador, envío de respuestas y señalización WebRTC.

Backend – Spring Boot: núcleo del sistema. Expone endpoints REST y gestiona el broker de mensajes STOMP para las salas. Dentro del servidor viven tres subsistemas clave: el Game Engine, responsable del ciclo de vida de cada ronda (selección de concepto, arranque del temporizador, validación de respuestas y cálculo de puntajes); el Room Manager, que mantiene el estado de cada sala de forma aislada; y el Signaling Handler, que retransmite los mensajes de negociación WebRTC (offer, answer, ICE candidates) entre los participantes de una misma sala.

Persistencia – PostgreSQL: almacena entidades de usuarios, salas, bancos de conceptos y resultados de sesión. El estado en vuelo de cada ronda (temporizador, respuestas parciales) se mantiene en memoria dentro del servidor para minimizar latencia.

Canal de voz – WebRTC: una vez que el servidor de señalización ha coordinado el intercambio de descriptores SDP y de candidatos ICE, los navegadores establecen conexiones de audio directas entre sí (peer-to-peer), sin pasar por el servidor. Esto reduce la carga del backend y la latencia del audio. En escenarios donde NAT o firewalls lo impidan, se puede añadir un servidor TURN como solución de respaldo.

## 5.2 Flujo de una ronda de juego

El siguiente flujo resume la secuencia de mensajes más relevante durante una ronda activa. Todos los eventos de tiempo real se transmiten por WebSocket con STOMP; los clientes se suscriben a tópicos específicos de su sala (/topic/sala/{id}/ronda, /topic/sala/{id}/timer, etc.) y el servidor publica en ellos:


- Inicio de ronda: el servidor selecciona el siguiente concepto del banco, registra el timestamp de inicio y publica el evento ROUND_START con el enunciado y la duración del temporizador hacia todos los clientes de la sala.

- Temporizador: el servidor emite actualizaciones periódicas del tiempo restante. Los clientes solo muestran el valor recibido; no calculan el tiempo localmente, lo que garantiza consistencia total.

- Envío de respuesta: cada cliente envía su intento al servidor por STOMP. El servidor valida, asigna puntos según corrección y velocidad, y publica el evento ANSWER_RESULT al jugador correspondiente y el estado actualizado del ranking a toda la sala.

- Cierre de ronda: al expirar el temporizador el servidor publica ROUND_END con el resumen de resultados. Tras un intervalo configurable arranca la siguiente ronda.

- Desconexión y reconexión: el servidor detecta la desconexión mediante el evento WebSocket y puede restaurar el estado al cliente cuando este reconecte, sin interrumpir a los demás participantes.

## 5.3 Flujo de señalización WebRTC

La voz en tiempo real requiere que los navegadores establezcan un canal de audio directo. Para ello es necesario un proceso de señalización previo que se apoya en el mismo WebSocket del servidor:

- Cuando un participante entra a la sala, el servidor notifica su presencia al resto.

- Cada par de participantes intercambia un mensaje offer / answer en formato SDP (Session Description Protocol) retransmitido por el servidor como mensajes STOMP.

- A continuación se intercambian los candidatos ICE para negociar la ruta de red más eficiente entre los dos navegadores.

- Una vez completada la negociación, el flujo de audio viaja directamente entre los navegadores sin pasar por el servidor (modelo peer-to-peer).

- Si la conexión directa falla (NAT simétrico, firewall restrictivo), se puede incorporar un servidor TURN como intermediario de medios.

## 5.4 Stack tecnológico

| Capa | Tecnología | Justificación |
| --- | --- | --- |
| Backend | Java 17+ · Spring Boot 3 | Base robusta para servicios REST y WebSocket |
| Mensajería tiempo | Spring WebSocket · STOMP Mensajería estructurada para salas y rondas |   |
| real |   |   |
| Frontend | React · JavaScript · | Interfaz reactiva y dinámica sin recarga de página |
|   | react-stomp-hooks |   |
| Voz en tiempo real | WebRTC (API nativa del | Audio peer-to-peer de baja latencia sin plugins |
|   | navegador) |   |


| Capa | Tecnología | Justificación |
| --- | --- | --- |
| Señalización | WebSocket · Spring Boot | Intercambio de offer, answer e ICE candidates |
| WebRTC |   |   |
| Persistencia | PostgreSQL | Usuarios, salas, bancos de conceptos y resultados |
| Escalado futuro | Redis Pub/Sub | Sincronización entre múltiples instancias del servidor |
| Despliegue | Railway / Render | Despliegue sencillo para el prototipo académico |

## 5.5 Consideraciones de escalabilidad

En su forma más simple el sistema opera con una única instancia de Spring Boot que gestiona todas las salas en memoria. Para escalar horizontalmente sería necesario externalizar el estado de las salas a un almacén compartido como Redis y usar su mecanismo de Pub/Sub para propagar los eventos entre instancias. Spring Framework ofrece soporte nativo para un broker STOMP externo basado en STOMP over TCP (RabbitMQ, ActiveMQ), lo que facilita esta transición sin cambiar la interfaz hacia el cliente.

## 6. Beneficios Esperados

## 6.1 Cuantitativos

- Reducción del tiempo para organizar sesiones grupales de estudio, al centralizar conceptos, respuestas y comunicación en una sola plataforma web.

- Soporte para múltiples salas simultáneas reutilizando la misma arquitectura de mensajería y señalización.

- Reutilización de la arquitectura para distintas materias sin rediseñar el núcleo del sistema.

- Participación activa de todos los estudiantes en cada ronda, alineada con recomendaciones pedagógicas para mejorar la retención por recuperación activa.

## 6.2 Cualitativos

- Estudio más dinámico, social y menos pasivo al convertir conceptos en rondas interactivas.

- Mayor motivación y engagement gracias al formato lúdico y competitivo de bajo riesgo.

- Mejor integración del trabajo en grupo, porque los estudiantes hablan por voz mientras resuelven conceptos compartidos.

- Alto valor académico y técnico al combinar aprendizaje activo con una arquitectura moderna de tiempo real.

- Adaptable a múltiples áreas del conocimiento, aumentando el potencial de adopción.

## 7. Análisis de Viabilidad

## 7.1 Viabilidad técnica


Todas las tecnologías del stack son de código abierto, ampliamente documentadas y utilizadas en producción en proyectos de alta escala. Spring WebSocket con STOMP y WebRTC son especialmente relevantes porque forman parte del temario del curso ARSW, lo que reduce la curva de aprendizaje del equipo. El mayor reto técnico es la coordinación entre el estado del juego, la señalización WebRTC y la estabilidad del chat de voz en salas con varios usuarios concurrentes.

## 7.2 Viabilidad económica

El proyecto es viable con costo prácticamente nulo para el prototipo académico: todas las tecnologías son gratuitas y los servicios de despliegue como Railway o Render ofrecen niveles gratuitos suficientes para pruebas y demostración. La arquitectura elegida no requiere licencias propietarias ni servicios de terceros de pago para el MVP.

## 7.3 Cronograma de implementación

| Semanas | Hito |
| --- | --- |
| 24 – 25 | Configuración del proyecto, estructura base, autenticación simple y modelo de |
|   | usuario/sala. |
| 25 – 26 | Lobby en tiempo real, creación de salas públicas y privadas, ingreso de participantes. |
| 26 – 27 | Integración de WebSockets + STOMP y eventos base de sala, ronda y puntajes. |
| 27 – 28 | Temporizador centralizado, visualización del concepto y envío simultáneo de respuestas. |
| 28 – 29 | Validación de respuestas, ranking de ronda y carga de bancos de conceptos por tema. |
| 29 – 30 | Integración de señalización WebRTC con Spring y pruebas de chat de voz en sala. |
| 30 – 31 | Pruebas integrales con múltiples usuarios, corrección de errores y manejo de |
|   | reconexiones. |
| 31 | Despliegue, documentación técnica y preparación de la presentación final. |

## 8. Riesgos y Mitigaciones

| Riesgo | Nivel | Impacto | Mitigación |
| --- | --- | --- | --- |
| Desincronización del | Alto | Injusticia en la ronda | El servidor es la única fuente de tiempo; los |
| temporizador |   |   | clientes solo muestran el valor recibido. |
| Respuestas | Alto | Pérdida de validez | Validación y asignación de puntos centralizada |
| inconsistentes entre |   | pedagógica | exclusivamente en el backend. |
| clientes |   |   |   |
| Complejidad del chat de | Alto | Problemas de | Usar WebRTC con señalización clara por |
| voz (WebRTC) |   | comunicación entre | WebSocket; documentar el flujo antes de |
|   |   | participantes | implementar. |
| Desconexión | Medio | Interrupción de la | Reconexión automática y restauración del |
| inesperada de usuarios |   | sesión | estado desde el servidor. |


| Riesgo | Nivel | Impacto | Mitigación |
| --- | --- | --- | --- |
| Exceso de alcance | Medio | No terminar el MVP a | Priorizar conceptos escritos y voz básica antes |
|   |   | tiempo | de cualquier funcionalidad adicional. |
| Curva de aprendizaje en | Medio | Retrasos en la | Basarse en ejemplos de señalización |
| WebRTC |   | integración de voz | existentes con Spring Boot antes de construir |
|   |   |   | desde cero. |
| NAT / Firewall bloquea | Bajo | Voz no funcional en | Incorporar servidor TURN como respaldo para |
| conexión P2P |   | algunos entornos | medios cuando la conexión directa falle. |

## 9. Conclusión y Recomendación

ConceptArena es una propuesta técnica y pedagógicamente sólida. Desde la perspectiva del curso ARSW, demuestra de forma clara y articulada el uso coordinado de WebSockets con STOMP para mensajería estructurada en tiempo real, WebRTC para comunicación multimedia peer-to-peer, arquitectura de salas concurrentes con estado aislado, sincronización de estado entre múltiples clientes y gestión de reconexiones. Estos componentes representan desafíos de implementación genuinos y alineados con el temario del curso.

Desde la perspectiva académica, el enfoque en retrieval practice le otorga valor pedagógico real más allá del ejercicio técnico, y la adaptabilidad a distintas materias y carreras amplía significativamente el potencial de adopción de la plataforma.

Se recomienda iniciar por el sistema de salas, el flujo de rondas y la validación de conceptos antes de integrar el chat de voz. Una vez que los estudiantes puedan entrar a una sala, leer un concepto, escribir la respuesta al mismo tiempo y ver resultados compartidos en tiempo real, la integración de voz añadirá una capa valiosa de colaboración sin alterar la base arquitectónica del proyecto.
