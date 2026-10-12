# PRD_PulsePass - V-2.0.0

PulsePass es un caso de estudio académico para el diseño e implementación de las capas de persistencia, servicios y controladores REST de una plataforma de eventos y venta de entradas.

## ¿Qué se espera?
Demostrar la capacidad de transformar requisitos de negocio en un modelo relacional robusto, coordinar la lógica transaccional a través de servicios desacoplados y exponer una API REST segura, predecible y estandarizada utilizando Spring Boot, Spring Data JPA, MapStruct, Bean Validation, Flyway, Testcontainers y MockMvc.

## Sección Técnica
- **Lenguaje:** Java 21
- **Framework:** Spring Boot 4.x / Spring MVC
- **ORM:** Spring Data JPA / Hibernate (modo `ddl-auto=validate`)
- **Base de Datos:** PostgreSQL
- **Migraciones:** Flyway
- **Mapeo:** MapStruct 1.6.3
- **Validación:** Bean Validation (Jakarta Validation)
- **Pruebas:** 
  - Integración: Testcontainers (PostgreSQL real)
  - Unitarias de Servicio: JUnit 5, Mockito, AssertJ
  - Pruebas de Controladores: `@WebMvcTest`, MockMvc, `@MockitoBean`

---

## Estructura de la Capa de Persistencia
1. **Modelo:** Entidades JPA (`Venue`, `Event`, `Artist`, `User`, `UserProfile`, `Ticket`) mapeadas contra un esquema relacional estricto con claves foráneas, restricciones de unicidad (`UNIQUE`) y validaciones de rango (`CHECK`).
2. **Migraciones:** Versionado de la base de datos en `src/main/resources/db/migration/`:
   - `V1`: Esquema base y restricciones de integridad física y de negocio.
   - `V2`: Carga de catálogo inicial de artistas.
   - `V3`: Incorporación de columna opcional de streaming URL a eventos.
3. **Consultas:** Repositorios Spring Data utilizando *Query Methods* y consultas *JPQL* con agregaciones (`COUNT`) y múltiples uniones (`JOIN`).

---

## Estructura de la Capa de Servicios
1. **DTOs (Data Transfer Objects):** Uso exclusivo de `records` inmutables de Java 21 para definir contratos de entrada y salida, asegurando que ninguna entidad JPA sea expuesta fuera de la capa de servicio.
2. **Mapeos (MapStruct):** Interfaces dedicadas para transformaciones compiladas y optimizadas entre entidades y DTOs sin reflexión manual.
3. **Lógica de Negocio y Transacciones:**
   - Servicios organizados por interfaces e implementaciones (`*ServiceImpl`) con inyección de dependencias por constructor (`final`).
   - Control transaccional explícito: `@Transactional(readOnly = true)` en consultas y `@Transactional` atómico en escrituras y transiciones de estado.
   - Validación de reglas de dominio (capacidad de aforo, mayoría de edad, eventos publicados, tickets pagados, etc.).
4. **Manejo de Errores de Dominio:** Excepciones específicas (`ResourceNotFoundException`, `BusinessRuleException`, `DuplicateResourceException`).

---

## Estructura de la Capa de Controladores REST
1. **Controladores Delgados:** Responsabilidad acotada a transporte HTTP, enrutamiento, códigos de respuesta semánticos y validación sintáctica. No acceden a repositorios ni duplican reglas de negocio.
2. **Validación Declarativa:** Uso de Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Min`, `@Email`, `@Size`) en los DTOs de request antes de invocar la capa de servicio.
3. **Manejo Global de Excepciones:** Componente `@RestControllerAdvice` (`GlobalExceptionHandler`) que centraliza la captura de excepciones y garantiza respuestas homogéneas bajo la estructura `ErrorResponse(timestamp, status, error, message, details)`:
   - `400 Bad Request`: Errores de validación estructural o JSON mal formado.
   - `404 Not Found`: Búsqueda de recursos no encontrados (`ResourceNotFoundException`).
   - `409 Conflict`: Conflictos de unicidad (`DuplicateResourceException`) o reglas de negocio incumplidas (`BusinessRuleException`).
   - `500 Internal Server Error`: Errores internos no controlados.

### Catálogo de Endpoints Principales (20 operaciones cubiertas)
| Método | Endpoint | Descripción | Código Éxito |
|---|---|---|---|
| `GET` | `/api/venues/{code}` | Consulta venue por código de negocio | 200 OK |
| `GET` | `/api/venues/active` | Lista venues activos | 200 OK |
| `POST` | `/api/events` | Creación de nuevo evento en DRAFT | 201 Created |
| `GET` | `/api/events/{eventCode}` | Consulta detalle de evento por código | 200 OK |
| `GET` | `/api/events/published` | Cartelera de eventos publicados | 200 OK |
| `PATCH`| `/api/events/{eventCode}/publish` | Publicar evento DRAFT | 200 OK |
| `POST` | `/api/events/{eventCode}/artists/{artistId}` | Asociar artista a evento | 200 OK |
| `GET` | `/api/events/by-artist?stageName=...` | Buscar eventos por nombre de artista | 200 OK |
| `GET` | `/api/artists/{id}` | Consulta artista por ID numérico | 200 OK |
| `GET` | `/api/artists/by-stage-name?stageName=...` | Consulta artista por nombre artístico | 200 OK |
| `GET` | `/api/artists/active` | Lista artistas activos | 200 OK |
| `POST` | `/api/users` | Registro de usuario y perfil | 201 Created |
| `GET` | `/api/users/by-email?email=...` | Consulta usuario por email | 200 OK |
| `GET` | `/api/users/by-username?username=...` | Consulta usuario por username | 200 OK |
| `POST` | `/api/tickets` | Compra atómica de entrada | 201 Created |
| `GET` | `/api/tickets/{ticketCode}` | Consulta entrada por código | 200 OK |
| `GET` | `/api/tickets/by-user?email=...` | Listar entradas de un usuario | 200 OK |
| `GET` | `/api/events/{eventCode}/tickets/paid` | Listar entradas PAID de un evento | 200 OK |
| `PATCH`| `/api/tickets/{ticketCode}/cancel` | Cancelar entrada PAID | 200 OK |
| `PATCH`| `/api/tickets/{ticketCode}/use` | Marcar entrada PAID como usada | 200 OK |

---

## Capa de Seguridad (Próxima Fase)
<!-- Esta sección se encuentra preparada para la siguiente fase del proyecto, donde se implementará la capa de seguridad con Spring Security, autenticación y autorización (JWT/OAuth2). -->

---

## Cómo Usar y Ejecutar Pruebas

Para ejecutar la suite completa de pruebas (Persistencia con Testcontainers, Servicios con Mockito y Controladores con MockMvc):

```bash
./mvnw clean test
```

### Variables de Entorno
Copiar el archivo de plantilla `.env.example` hacia `.env` según la configuración del entorno local:

```bash
cp .env.example .env
```

*(El archivo `.env` se encuentra excluido de control de versiones y contiene únicamente parámetros de conexión sin credenciales sensibles).*

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
   Se alteraría el *checksum* registrado en `flyway_schema_history`, provocando que Flyway aborte el arranque de la aplicación por incoherencia de migración. Todo cambio en esquema debe realizarse mediante migraciones incrementales (`V2`, `V3`).
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
    Incorporar dependencias de Spring Security y JWT, configurar un `SecurityFilterChain`, crear un filtro para interceptar y validar el token en el encabezado `Authorization`, y asociar el usuario autenticado al `SecurityContext`.