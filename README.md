# PRD_PulsePass - Release V-3.1.0

PulsePass es un caso de estudio académico para el diseño e implementación incremental de las capas de **persistencia**, **servicios**, **controladores REST** y **seguridad stateless con JWT** de una plataforma de eventos, artistas y venta de entradas.

## ¿Qué se espera?
Demostrar la capacidad de transformar requisitos de negocio en un modelo relacional robusto, coordinar la lógica transaccional a través de servicios desacoplados, exponer una API REST estandarizada y proteger todas las operaciones mediante autenticación con BCrypt, emisión/validación de JWT firmados (HS256), control de acceso por roles (`USER`, `STAFF`, `ADMIN`) y validación de propiedad de recursos (anti-IDOR).

## Sección Técnica
- **Lenguaje:** Java 21
- **Framework:** Spring Boot 4.x / Spring MVC
- **Seguridad:** Spring Security 7.x + JJWT 0.13.0 (HS256 Stateless Bearer)
- **ORM:** Spring Data JPA / Hibernate (`ddl-auto=validate`)
- **Base de Datos:** PostgreSQL
- **Migraciones:** Flyway (`V1` a `V4`)
- **Mapeo:** MapStruct 1.6.3
- **Validación:** Bean Validation (Jakarta Validation)
- **Pruebas:** 
  - Integración: Testcontainers (PostgreSQL real)
  - Unitarias de Servicio y Seguridad: JUnit 5, Mockito, AssertJ
  - Pruebas Web y de Seguridad HTTP: `@WebMvcTest`, MockMvc, `spring-security-test`, `@MockitoBean`

---

## Estructura de la Capa de Persistencia (Fase 1)
1. **Modelo:** Entidades JPA (`Venue`, `Event`, `Artist`, `User`, `UserProfile`, `Ticket`, `UserCredential`, `UserRole`) mapeadas contra un esquema relacional estricto con claves foráneas, restricciones de unicidad (`UNIQUE`) y validaciones de rango (`CHECK`).
2. **Migraciones:** Versionado de la base de datos en `src/main/resources/db/migration/`:
   - `V1__create_schema.sql`: Esquema base y restricciones de integridad física y de negocio.
   - `V2__insert_initial_artists.sql`: Carga de catálogo inicial de artistas.
   - `V3__add_streaming_url_to_event.sql`: Incorporación de columna opcional de streaming URL a eventos.
   - `V4__security_credentials_roles.sql`: Tablas `user_credentials` (1:1 con `users`) y `user_roles` (`USER`, `STAFF`, `ADMIN`), más función segura para activación de cuentas legadas.
3. **Consultas:** Repositorios Spring Data utilizando *Query Methods* y consultas *JPQL* con agregaciones (`COUNT`) y múltiples uniones (`JOIN`).

---

## Estructura de la Capa de Servicios (Fase 2)
1. **DTOs (Data Transfer Objects):** Uso exclusivo de `records` inmutables de Java 21 para definir contratos de entrada y salida, asegurando que ninguna entidad JPA ni hash de contraseña sea expuesto fuera de la capa de servicio.
2. **Mapeos (MapStruct):** Interfaces dedicadas para transformaciones compiladas y optimizadas entre entidades y DTOs.
3. **Lógica de Negocio y Transacciones:**
   - Servicios organizados por interfaces e implementaciones (`*ServiceImpl`) con inyección de dependencias por constructor.
   - Control transaccional explícito: `@Transactional(readOnly = true)` en consultas y `@Transactional` atómico en escrituras (como el registro simultáneo de usuario, perfil, credencial BCrypt y rol `USER`, o la compra de tickets con actualización a `SOLD_OUT`).
4. **Manejo de Errores de Dominio:** Excepciones específicas (`ResourceNotFoundException`, `BusinessRuleException`, `DuplicateResourceException`).

---

## Estructura de la Capa de Controladores REST (Fase 3)
1. **Controladores Delgados:** Responsabilidad acotada a transporte HTTP, enrutamiento, códigos de respuesta semánticos y validación sintáctica.
2. **Validación Declarativa:** Uso de Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Min`, `@Email`, `@Size`) en los DTOs de request antes de invocar la capa de servicio.
3. **Manejo Global de Excepciones:** Componente `@RestControllerAdvice` (`GlobalExceptionHandler`) junto con `RestAuthenticationEntryPoint` y `RestAccessDeniedHandler` que garantizan respuestas homogéneas bajo la estructura `ErrorResponse(timestamp, status, error, message, details)`:
   - `400 Bad Request`: Errores de validación estructural o JSON mal formado.
   - `401 Unauthorized`: Credenciales inválidas, ausencia de token Bearer, token expirado/manipulado o usuario inactivo.
   - `403 Forbidden`: Usuario autenticado sin el rol requerido o intento de operar sobre recursos de otro usuario (anti-IDOR).
   - `404 Not Found`: Búsqueda de recursos no encontrados (`ResourceNotFoundException`).
   - `409 Conflict`: Conflictos de unicidad (`DuplicateResourceException`) o reglas de negocio incumplidas (`BusinessRuleException`).
   - `500 Internal Server Error`: Errores internos no controlados (sin exponer stack traces).

---

## Estructura de la Capa de Seguridad (Fase 4)
1. **Autenticación y Registro Seguro:**
   - Contraseñas almacenadas exclusivamente mediante hash irreversible `BCryptPasswordEncoder` en `user_credentials`.
   - Registro público (`POST /api/users`) exige contraseña de entre 12 y 72 caracteres y asigna exclusivamente `ROLE_USER` dentro de la misma transacción.
2. **Emisión y Validación de JWT (`JwtService`):**
   - Tokens JWS firmados con algoritmo `HS256` utilizando JJWT `0.13.0` y clave secreta en Base64 de mínimo 32 bytes (`PULSEPASS_JWT_SECRET_BASE64`).
   - Claims obligatorios validados en cada solicitud: `sub` (ID persistente del usuario), `iss` (`pulsepass-api`), `iat`, `exp` (15 minutos / 900 segundos) y `jti`.
3. **Filtro Stateless y Revalidación (`JwtAuthenticationFilter` + `CustomUserDetailsService`):**
   - API 100% *stateless* (`SessionCreationPolicy.STATELESS`), sin `JSESSIONID` ni aceptación de tokens por query string.
   - Cada solicitud con encabezado `Authorization: Bearer <token>` verifica la firma/vigencia del JWT y consulta en base de datos el estado actual (`active = true`) y los roles vigentes del usuario.
4. **Roles y Control de Propiedad (`AuthorizationService`):**
   - **`ROLE_USER`:** Puede comprar entradas únicamente para su propio correo, consultar su propio perfil/entradas y cancelar únicamente sus propias entradas pagadas.
   - **`ROLE_STAFF`:** Personal operativo; puede consultar entradas por código, listar entradas pagadas de un evento y marcar entradas como usadas (`PATCH /api/tickets/{ticketCode}/use`), además de visualizar eventos no publicados. *(Limitación documentada del MVP: autorización global sobre eventos hasta que una fase futura incorpore asignación de staff por evento).*
   - **`ROLE_ADMIN`:** Administración de eventos (`POST /api/events`, publicar eventos, asociar artistas) y acceso administrativo autorizado.

### Catálogo Completo de Endpoints y Matriz de Autorización (22 operaciones)
| Método | Endpoint | Acceso / Rol Requerido | Regla de Propiedad / Visibilidad | Código Éxito |
|---|---|---|---|---|
| `POST` | `/api/auth/login` | Público | Autentica username/email + password y emite JWT | 200 OK |
| `GET` | `/api/auth/me` | Autenticado (Bearer) | Retorna identidad y roles del principal actual | 200 OK |
| `GET` | `/api/venues/{code}` | Público | — | 200 OK |
| `GET` | `/api/venues/active` | Público | Solo venues activos | 200 OK |
| `POST` | `/api/events` | `ADMIN` | — | 201 Created |
| `GET` | `/api/events/{eventCode}` | Público / `STAFF` / `ADMIN` | Público solo si `PUBLISHED`; otros estados requieren `STAFF`/`ADMIN` | 200 OK |
| `GET` | `/api/events/published` | Público | Solo eventos `PUBLISHED` | 200 OK |
| `PATCH`| `/api/events/{eventCode}/publish` | `ADMIN` | — | 200 OK |
| `POST` | `/api/events/{eventCode}/artists/{artistId}` | `ADMIN` | — | 200 OK |
| `GET` | `/api/events/by-artist?stageName=...` | Público / `STAFF` / `ADMIN` | Público filtra solo `PUBLISHED`; `STAFF`/`ADMIN` ven todos los estados | 200 OK |
| `GET` | `/api/artists/{id}` | Público | — | 200 OK |
| `GET` | `/api/artists/by-stage-name?stageName=...` | Público | — | 200 OK |
| `GET` | `/api/artists/active` | Público | Solo artistas activos | 200 OK |
| `POST` | `/api/users` | Público | Crea cuenta con rol inicial `USER` y hash BCrypt | 201 Created |
| `GET` | `/api/users/by-email?email=...` | `USER` / `ADMIN` | `USER` solo su propio usuario (`userId`); o `ADMIN` | 200 OK |
| `GET` | `/api/users/by-username?username=...` | `USER` / `ADMIN` | `USER` solo su propio usuario (`userId`); o `ADMIN` | 200 OK |
| `POST` | `/api/tickets` | `USER` | `userEmail` debe pertenecer al usuario autenticado | 201 Created |
| `GET` | `/api/tickets/{ticketCode}` | `USER` / `STAFF` / `ADMIN` | `USER` solo si es propietario del ticket; o `STAFF`/`ADMIN` | 200 OK |
| `GET` | `/api/tickets/by-user?email=...` | `USER` / `ADMIN` | `USER` solo sus propios tickets; o `ADMIN` | 200 OK |
| `GET` | `/api/events/{eventCode}/tickets/paid` | `STAFF` / `ADMIN` | — | 200 OK |
| `PATCH`| `/api/tickets/{ticketCode}/cancel` | `USER` / `ADMIN` | `USER` solo si es propietario del ticket; o `ADMIN` | 200 OK |
| `PATCH`| `/api/tickets/{ticketCode}/use` | `STAFF` / `ADMIN` | — | 200 OK |

---

## Cómo Configurar y Ejecutar Pruebas

### Variables de Entorno
Copiar la plantilla `.env.example` hacia `.env` y configurar una clave secreta Base64 de al menos 32 bytes para ejecución local:

```bash
cp .env.example .env
# Ejemplo para generar un secreto local seguro de 32 bytes en Base64:
openssl rand -base64 32
```

Variables utilizadas por la aplicación (`application.yaml`):
- `DB_URL`, `DB_USER`, `DB_PASSWORD`: Conexión a PostgreSQL.
- `PULSEPASS_JWT_ISSUER`: Emisor esperado del token (por defecto `pulsepass-api`).
- `PULSEPASS_JWT_SECRET_BASE64`: Clave secreta HMAC-SHA256 en Base64 (mínimo 32 bytes decodificados; la aplicación falla al iniciar si está ausente o es débil).

*(En las pruebas automatizadas con Maven, el archivo `src/test/resources/application.yaml` provee una clave ficticia exclusiva para el entorno de test).*

### Ejecución de Pruebas Automatizadas
Para ejecutar la suite completa de 98 pruebas (Persistencia con Testcontainers, Servicios con Mockito, Controladores y Seguridad con MockMvc):

```bash
./mvnw clean test
```

### Pruebas End-to-End (E2E) con Bash y cURL
El proyecto cuenta además con una suite de scripts multiplataforma (`Bash + cURL`) en la carpeta [`post-tests/`](./post-tests/) para validar de extremo a extremo los 22 endpoints, los flujos por rol (`USER`, `STAFF`, `ADMIN`), la seguridad JWT y las reglas de negocio. Para consultar su arquitectura e instrucciones detalladas de uso y ejecución, revisa el documento [**Guía de Pruebas End-to-End (`pruebas-e2e.md`)**](./pruebas-e2e.md).

---

## Preguntas para el equipo

### Capa de Persistencia
1. **¿Por qué Ticket debe ser una entidad en lugar de un @ManyToMany entre User y Event?**  
   Porque posee identidad propia y atributos de negocio transaccionales esenciales (`ticketCode`, `type`, `price`, `status`, `purchaseDate`). Una relación `@ManyToMany` plana no permite modelar datos propios del ticket ni controlar su ciclo de vida individual.
2. **¿Qué reglas pertenecen a PostgreSQL y cuáles deberían quedar para una futura capa Service?**  
   PostgreSQL salvaguarda la integridad física y estructural de los datos (`NOT NULL`, llaves foráneas, `UNIQUE` en códigos y correos, y `CHECK` para precios y capacidades no negativas). La capa Service maneja la lógica de negocio temporal y dinámica (verificación de mayoría de edad al momento del evento, validación de aforo en tiempo real y transiciones de estado).
3. **¿Qué consultas pueden expresarse claramente como Query Methods y cuáles justifican JPQL?**  
   Las búsquedas directas por atributos simples o navegación de relaciones básicas (como `findByCode` o `findByStatusOrderByEventDateAsc`) son idóneas para Query Methods. Las que requieren agregaciones (`COUNT`), uniones complejas (`JOIN`) o filtros sobre tablas intermedias (conteo de entradas pagadas o búsqueda de eventos por artista) justifican el uso explícito de JPQL.
4. **¿Qué consecuencias tendría modificar V1 después de haberla aplicado en un ambiente compartido?**  
   Se alteraría el *checksum* registrado en `flyway_schema_history`, provocando que Flyway aborte el arranque de la aplicación por incoherencia de migración. Todo cambio en esquema debe realizarse mediante migraciones incrementales (`V2`, `V3`, `V4`).
5. **¿Qué diferencias podría ocultar una prueba con H2 frente a PostgreSQL?**  
   Diferencias de sintaxis en funciones nativas, soporte de tipos específicos, evaluación estricta de restricciones y transacciones concurrentes, y validación exacta de las migraciones SQL de producción.
6. **¿Cómo evolucionaría el modelo para soportar inventario de tickets y evitar sobreventa?**  
   Introduciendo entidades desacopladas para inventario y sectores (`EventCapacity`, `TicketBatch`), aplicando control de concurrencia optimista (`@Version`) o bloqueos pesimistas, y gestionando reservas temporales con temporizador antes de confirmar el pago.

### Capa de Servicios
1. **¿Cuál es la responsabilidad de Service?**  
   Orquestar la lógica de negocio, coordinar repositorios, administrar transacciones (`@Transactional`), validar reglas de dominio y transformar entidades a DTOs.
2. **¿Qué diferencia existe entre Service y Repository?**  
   Repository se encarga exclusivamente de las operaciones de lectura y escritura en la base de datos (I/O). Service decide cuándo, cómo y bajo qué reglas de negocio se ejecutan esas operaciones.
3. **¿Por qué Service no debería retornar entidades JPA?**  
   Para evitar acoplar el contrato externo con el esquema de base de datos, prevenir fugas de campos internos o sensibles, y eliminar riesgos de `LazyInitializationException` fuera del contexto transaccional.
4. **¿Qué ventaja tiene record para DTOs?**  
   Garantiza inmutabilidad nativa, provee métodos automáticos (`equals`, `hashCode`, `toString`) y reduce drásticamente el código boilerplate en Java 21.
5. **¿Qué hace MapStruct?**  
   Genera código Java seguro en tiempo de compilación para convertir objetos (Entidad a DTO y viceversa), evitando errores manuales y sobrecargas de rendimiento por reflexión.
6. **¿Por qué preferir constructor injection?**  
   Permite declarar las dependencias como `final`, garantiza que el bean se cree en un estado totalmente inicializado y simplifica la instanciación directa en tests unitarios sin levantar Spring.
7. **¿Qué diferencia existe entre ResourceNotFoundException y BusinessRuleException?**  
   `ResourceNotFoundException` indica que un recurso solicitado no existe físicamente (404). `BusinessRuleException` indica que el recurso sí existe pero la operación solicitada no es permitida por las reglas de negocio (409).
8. **¿Por qué evitar Optional.get()?**  
   Porque si el valor no está presente lanzará `NoSuchElementException` de forma imprevista; debe manejarse mediante `.orElseThrow()` indicando la excepción de negocio correspondiente.
9. **¿Cuándo utilizar @Transactional(readOnly = true)?**  
   En operaciones de solo lectura para optimizar memoria y tiempo de CPU, indicándole al motor de persistencia que omita el seguimiento de cambios (*dirty checking*).
10. **¿Cuándo utilizar @Transactional?**  
    En operaciones que mutan el estado de la base de datos (creación, edición, eliminación) para asegurar que si algo falla, ocurra un *rollback* completo y la base de datos no quede en estado inconsistente.
11. **¿Por qué una compra debe ser atómica?**  
    Para evitar que se registre una entrada pero falle la actualización de aforo o el cambio de estado a `SOLD_OUT`, lo que generaría sobreventa.
12. **¿Por qué la capacidad pertenece a una regla de Service?**  
    Porque el cálculo del cupo disponible depende de dinámicas de negocio (entradas pagadas, cortesías, reservas) que no se limitan a un campo estático de tabla.
13. **¿Por qué el cálculo de edad pertenece al negocio?**  
    Porque las políticas de restricción de edad varían por evento y deben evaluarse dinámicamente con base en la fecha de realización del evento y la fecha de nacimiento del asistente.
14. **¿Por qué utilizar BigDecimal para precios?**  
    Para evitar imprecisiones de redondeo y pérdida de decimales asociadas al cálculo en punto flotante (`float` o `double`) en operaciones monetarias.
15. **¿Qué diferencia existe entre unit test e integration test?**  
    El unit test prueba una clase en aislamiento simulando sus dependencias en memoria para respuesta instantánea. El integration test valida la interacción armónica entre varias capas y servicios reales (como PostgreSQL).
16. **¿Por qué Mockito permite probar Service sin PostgreSQL?**  
    Porque reemplaza las llamadas a los repositorios por respuestas simuladas en memoria, permitiendo validar las ramas de decisión y excepciones del servicio sin base de datos.
17. **¿Qué demuestra verify(repository, never()).save(...)?**  
    Demuestra que cuando una validación o regla de negocio falló, el flujo se detuvo oportunamente y ningún dato fue guardado indebidamente.
18. **¿Por qué SOLD_OUT debe actualizarse dentro de la misma transacción?**  
    Para asegurar que inmediatamente después de comprar el último cupo, el evento quede marcado como agotado, evitando compras concurrentes adicionales.
19. **¿Por qué Ticket es una entidad y no solo una relación N:M?**  
    Porque modela una transacción de compra individual con identificador propio, precio facturado, tipo de entrada y estados de uso independientes.
20. **¿Qué reglas moverías a componentes especializados si PulsePass creciera?**  
    La tarificación dinámica a un `PricingService`, el control de cupos a un `InventoryService` y los cobros a un `PaymentService`.

### Capa de Controladores
1. **¿Qué responsabilidad pertenece al Controller y cuál al Service?**  
   El Controller se responsabiliza del protocolo HTTP (mapeo de rutas, parámetros, códigos de estado, deserialización de requests, validación estructural y delegación). El Service contiene y ejecuta la lógica del negocio y la integridad transaccional.
2. **¿Por qué un Controller no debería acceder directamente a Repository?**  
   Porque viola la arquitectura en capas, elude las validaciones de negocio y transacciones gestionadas por el Service, y expone entidades de dominio hacia el exterior.
3. **¿Qué diferencia existe entre una validación estructural y una regla de negocio?**  
   La validación estructural revisa que los datos vengan en el formato y presencia esperados (campo no nulo, formato de email, tamaño) antes de llegar al servicio; la regla de negocio evalúa el estado del dominio (si el usuario está activo, si hay aforo o si el evento ya caducó).
4. **¿Cuándo debe utilizarse 400, 404 y 409?**  
   `400 Bad Request` para payloads mal formados o violaciones de Bean Validation; `404 Not Found` cuando el recurso solicitado no existe; `409 Conflict` ante conflictos de unicidad (duplicados) o violaciones de reglas lógicas del negocio.
5. **¿Por qué una creación debería retornar 201 en lugar de 200?**  
   Porque el estándar HTTP define semánticamente `201 Created` para señalar explícitamente que la solicitud derivó en la creación exitosa de un nuevo recurso.
6. **¿Por qué los errores deben utilizar una estructura uniforme?**  
   Para que los consumidores de la API (aplicaciones web, móviles o clientes externos) procesen los mensajes de error de forma consistente y predecible a través de un contrato estandarizado (`ErrorResponse`).
7. **¿Qué ventaja aporta @RestControllerAdvice frente a capturar excepciones en cada endpoint?**  
   Centraliza el control de errores en un solo lugar, desacopla los controladores de la lógica de respuesta a fallos y elimina la duplicación de bloques `try-catch`.
8. **¿Por qué @WebMvcTest es más apropiado que @SpringBootTest para estas pruebas?**  
   Porque inicializa exclusivamente la capa web de Spring MVC en milisegundos, permitiendo probar controladores, filtros y manejadores de excepciones sin levantar bases de datos ni el contexto completo de la aplicación.
9. **¿Cómo verificarías que un request inválido nunca invoca el Service?**  
   Utilizando `verify(serviceMock, never()).metodo(...)` de Mockito en la prueba de MockMvc tras enviar un payload que viole Bean Validation.
10. **¿Qué cambios serían necesarios para agregar autenticación JWT en una fase posterior?**  
    Incorporar dependencias de Spring Security y JJWT, configurar un `SecurityFilterChain` stateless, crear un filtro `OncePerRequestFilter` para interceptar y validar el token en el encabezado `Authorization: Bearer`, y asociar el `SecurityPrincipal` al `SecurityContext`.