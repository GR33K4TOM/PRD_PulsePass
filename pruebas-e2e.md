# Guía Detallada de Pruebas End-to-End (E2E) con Bash y cURL — PulsePass

Este documento describe la arquitectura, requisitos previos, variables de entorno, casos de prueba y modo de ejecución de la suite de pruebas **End-to-End (E2E)** ubicada en la carpeta [`post-tests/`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/).

De acuerdo con los lineamientos del documento [`PRD-PulsePassPruebas-E2E-Postman-Manual.pdf`](file:///home/gabi/Proyectos/PRD_PulsePass/PRD-PulsePassPruebas-E2E-Postman-Manual.pdf) (Secciones 1, 10, 14, 21 y 23), esta suite implementa todas las solicitudes HTTP reales sobre los **22 endpoints** del sistema (`20` de negocio + `2` de autenticación) mediante **scripts de Bash y `cURL` agnósticos de sistema operativo** (ejecutables en Linux, macOS y Windows a través de Git Bash o WSL, sin requerir instalaciones adicionales como `jq` ni clientes gráficos).

---

## 1. Estructura de la Carpeta `post-tests/`

```text
post-tests/
├── .env.e2e.example                           # Plantilla segura de variables de entorno (sin secretos reales)
├── .gitignore                                 # Ignora .env.e2e, .tokens.env y logs locales
├── common_e2e.sh                              # Librería compartida (cURL, extracción JSON POSIX, aserciones y enmascaramiento JWT)
├── 00_setup_fixtures.sh                       # Prepara cuentas semilla vía HTTP + aplica fixtures/seed-e2e.sql
├── FR-E2E-001_flujo_a_descubrimiento_publico.sh  # Flujo A: Venues, artistas y eventos públicos sin token
├── FR-E2E-002_flujo_b_registro_y_jwt.sh          # Flujo B: Registro público, login JWT, /api/auth/me y perfiles
├── FR-E2E-003_flujo_c_catalogo_admin.sh          # Flujo C: Creación/publicación de eventos y asociación de artistas (ADMIN)
├── FR-E2E-004_flujo_d_compra_y_tickets.sh        # Flujo D: Compra de entradas, consultas, cancelación y validación en puerta (STAFF)
├── FR-E2E-005_flujo_e_seguridad_y_negativos.sh   # Flujo E: Anti-IDOR, manipulación JWT, escalamiento y reglas de negocio (409)
├── run_all_e2e.sh                             # Orquestador que ejecuta secuencialmente FR-E2E-001 a FR-E2E-005
├── checklist-e2e-manual.md                    # Lista de verificación (DoR y criterios de aceptación)
├── registro-ejecucion-manual.md               # Plantilla y bitácora de resultados por caso de prueba
└── fixtures/
    └── seed-e2e.sql                           # Datos semilla de prueba (recintos, eventos base y promoción de roles ADMIN/STAFF)
```

---

## 2. Cumplimiento del Definition of Ready (DoR) — Sección 23 del PRD

Antes de ejecutar la suite E2E, el entorno cumple las siguientes 10 condiciones del **Definition of Ready (DoR)**:

1. **PostgreSQL disponible y accesible:** Instancia local o contenedor Docker con base de datos `pulsepass`.
2. **Migraciones Flyway aplicadas (`V1` a `V4`):** La aplicación valida el esquema al arrancar (`ddl-auto=validate`).
3. **Secreto JWT configurado por entorno:** Variable `PULSEPASS_JWT_SECRET_BASE64` con al menos 32 bytes (256 bits) codificados en Base64.
4. **Catálogo semilla cargado:** Artistas iniciales de `V2` más los recintos (`VEN-SMR-01` activo con capacidad controlada de `3`, y `VEN-INACTIVE-01` inactivo) y eventos (`EVT-SEED-DRAFT`, `CMF-2026`) cargados por [`seed-e2e.sql`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/fixtures/seed-e2e.sql).
5. **Usuarios de prueba `ADMIN`, `STAFF` e `INACTIVE_USER` disponibles:** Creados por [`00_setup_fixtures.sh`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/00_setup_fixtures.sh) usando el propio `BCryptPasswordEncoder` de la API y promovidos a sus roles mediante `seed-e2e.sql`, sin almacenar contraseñas ni hashes fijos en el repositorio.
6. **Variables de entorno preparadas:** Plantilla [`.env.e2e.example`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/.env.e2e.example) lista para copiarse a `.env.e2e`.
7. **Scripts HTTP versionados:** Scripts `FR-E2E-001` a `FR-E2E-005` listos y parametrizados.
8. **Plantilla de bitácora lista:** Disponible en [`registro-ejecucion-manual.md`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/registro-ejecucion-manual.md).
9. **Política ante recurso ajeno definida (`EXPECTED_FOREIGN_RESOURCE_STATUS`):** Definida consistentemente en `403 Forbidden` (tanto en `AuthorizationService` como en `.env.e2e.example`).
10. **Aplicación levantando en `BASE_URL`:** Por defecto `http://localhost:8080`.

---

## 3. Instrucciones Paso a Paso para Ejecutar las Pruebas (Linux, macOS y Windows Git Bash / WSL)

### Paso 1: Configurar el archivo `.env.e2e` local
Copia la plantilla de ejemplo hacia `post-tests/.env.e2e` (este archivo está ignorado en Git para proteger tus contraseñas locales):

```bash
cp post-tests/.env.e2e.example post-tests/.env.e2e
```

Edita `post-tests/.env.e2e` y asigna contraseñas locales de al menos **12 caracteres** para las variables:
- `USER_A_PASSWORD`
- `USER_B_PASSWORD`
- `ADMIN_PASSWORD`
- `STAFF_PASSWORD`
- `INACTIVE_PASSWORD`
- `DB_PASSWORD` (y opcionalmente `DOCKER_PG_CONTAINER` si tu PostgreSQL corre dentro de un contenedor Docker).

> [!TIP]
> Si deseas repetir la corrida múltiples veces sobre la misma base de datos sin reiniciarla, basta con cambiar el valor de `RUN_ID` (por ejemplo `RUN_ID=run02`) y `EVENT_TO_PUBLISH_CODE=EVT-E2E-run02` en `post-tests/.env.e2e`.

### Paso 2: Asegurar que PostgreSQL y la API de PulsePass estén en ejecución
En una terminal en la raíz del proyecto, exporta las variables de entorno de la aplicación y levanta Spring Boot:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/pulsepass"
export DB_USER="postgres"
export DB_PASSWORD="tu_password_postgres"
export PULSEPASS_JWT_ISSUER="pulsepass-api"
export PULSEPASS_JWT_SECRET_BASE64="$(openssl rand -base64 32)"

./mvnw spring-boot:run
```

### Paso 3: Cargar los datos semilla de prueba (`00_setup_fixtures.sh`)
Una vez que la aplicación esté escuchando en `BASE_URL`, ejecuta el script de preparación:

```bash
bash post-tests/00_setup_fixtures.sh
```

**¿Qué hace este script?**
1. Registra vía HTTP (`POST /api/users`) las cuentas `admin_e2e`, `laura_staff` y `miguel_inactivo` utilizando las contraseñas definidas en tu `.env.e2e`, de modo que Spring Security genere sus hashes BCrypt reales en `user_credentials`.
2. Ejecuta [`post-tests/fixtures/seed-e2e.sql`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/fixtures/seed-e2e.sql) en PostgreSQL (usando `psql` local o `docker exec` si configuraste `DOCKER_PG_CONTAINER`) para insertar los recintos `VEN-SMR-01` y `VEN-INACTIVE-01`, los eventos base `EVT-SEED-DRAFT` y `CMF-2026`, asignar los roles `ADMIN` y `STAFF`, e inactivar a `miguel_inactivo`.

### Paso 4: Ejecutar la Suite Completa o Flujos Individuales

Para ejecutar **todos los flujos en orden secuencial** (`FR-E2E-001` a `FR-E2E-005`):

```bash
bash post-tests/run_all_e2e.sh
```

También puedes ejecutar **cada flujo por separado** en orden:

```bash
# Flujo A: Descubrimiento público (Venues, Artistas, Eventos publicados y bloqueo de DRAFT)
bash post-tests/FR-E2E-001_flujo_a_descubrimiento_publico.sh

# Flujo B: Registro público, Login JWT, /api/auth/me y consultas de usuario (guarda tokens en .tokens.env)
bash post-tests/FR-E2E-002_flujo_b_registro_y_jwt.sh

# Flujo C: Creación de evento por ADMIN, asociación de artista y publicación
bash post-tests/FR-E2E-003_flujo_c_catalogo_admin.sh

# Flujo D: Compra de entradas (USER_A y USER_B), consultas, cancelación y validación en puerta por STAFF
bash post-tests/FR-E2E-004_flujo_d_compra_y_tickets.sh

# Flujo E: Pruebas negativas de seguridad, Anti-IDOR, manipulación JWT, escalamiento y agotamiento SOLD_OUT
bash post-tests/FR-E2E-005_flujo_e_seguridad_y_negativos.sh
```

---

## 4. Cobertura de Endpoints y Matriz de Casos de Prueba

| Script | Código(s) del PRD | Endpoints Probados | Validaciones Principales |
|---|---|---|---|
| [`FR-E2E-001_flujo_a_descubrimiento_publico.sh`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/FR-E2E-001_flujo_a_descubrimiento_publico.sh) | `TC-SEC-001`, `TC-SEC-002`, `TC-BIZ-005` | `GET /api/venues/active`<br>`GET /api/venues/{code}`<br>`GET /api/artists/active`<br>`GET /api/artists/{id}`<br>`GET /api/artists/by-stage-name`<br>`GET /api/events/published`<br>`GET /api/events/by-artist`<br>`GET /api/events/{eventCode}` | Acceso público `200 OK` sin token; `404 Not Found` en recinto inexistente; ocultamiento de eventos `DRAFT` en listado por artista y `403 Forbidden` al consultar detalle de `DRAFT` sin rol `STAFF`/`ADMIN`. |
| [`FR-E2E-002_flujo_b_registro_y_jwt.sh`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/FR-E2E-002_flujo_b_registro_y_jwt.sh) | `TC-AUTH-001` a `TC-AUTH-007`, `TC-SEC-005` | `POST /api/users`<br>`POST /api/auth/login`<br>`GET /api/auth/me`<br>`GET /api/users/by-email`<br>`GET /api/users/by-username` | Registro `201 Created` sin exponer `passwordHash`; login por username y email (`200 OK`, `tokenType=Bearer`, `expiresIn=900`); fallos de login (`401` por password erróneo o usuario inactivo, `400` por payload vacío); `/api/auth/me` con y sin token; Anti-IDOR (`403`) cuando `USER_A` consulta a `USER_B`. |
| [`FR-E2E-003_flujo_c_catalogo_admin.sh`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/FR-E2E-003_flujo_c_catalogo_admin.sh) | `TC-SEC-003`, `TC-SEC-004`, `TC-BIZ-001` | `POST /api/events`<br>`POST /api/events/{code}/artists/{id}`<br>`GET /api/events/{code}`<br>`GET /api/events/by-artist`<br>`PATCH /api/events/{code}/publish` | Rechazo de creación de eventos sin token (`401`) o con `USER`/`STAFF` (`403`); creación en `DRAFT` por `ADMIN` (`201`); visibilidad de `DRAFT` para `STAFF`; publicación a `PUBLISHED` (`200`) y apertura al público. |
| [`FR-E2E-004_flujo_d_compra_y_tickets.sh`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/FR-E2E-004_flujo_d_compra_y_tickets.sh) | `TC-SEC-007` | `POST /api/tickets`<br>`GET /api/tickets/{code}`<br>`GET /api/tickets/by-user`<br>`GET /api/events/{code}/tickets/paid`<br>`PATCH /api/tickets/{code}/cancel`<br>`PATCH /api/tickets/{code}/use` | Compra de entradas (`201 Created`) con cálculo de precio por tipo (`GENERAL`, `VIP`, `STUDENT`); consulta por propietario y `STAFF`; cancelación por propietario (`CANCELLED`) y validación en puerta por `STAFF` (`USED`). |
| [`FR-E2E-005_flujo_e_seguridad_y_negativos.sh`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/FR-E2E-005_flujo_e_seguridad_y_negativos.sh) | `TC-AUTH-008` a `010`, `TC-SEC-006`, `008`, `009`, `TC-BIZ-002` a `008` | Todos los endpoints protegidos de `tickets`, `events`, `users` y `auth` | Rechazo de esquema distinto a `Bearer`, firma manipulada o JWT corrupto (`401`); Anti-IDOR en lectura, listado, compra y cancelación de tickets ajenos (`403`); bloqueo de escalamiento de privilegios en `POST /api/users`; errores de negocio `409 Conflict` (evento `DRAFT`, recinto inactivo, ticket ya usado/cancelado y agotamiento de aforo `SOLD_OUT`). |

---

## 5. Diseño Agnóstico de Sistema Operativo y Seguridad de Logs

- **Compatibilidad Multiplataforma:** Los scripts usan únicamente utilidades POSIX estándar (`bash`, `curl`, `sed`, `grep`, `tr`, `head`, `tail`) y eliminan automáticamente posibles retornos de carro de Windows (`tr -d '\r'`) al leer archivos `.env.e2e` y encabezados HTTP.
- **Confidencialidad de Tokens (`NFR-E2E-002`):** La función `run_http_test` en [`common_e2e.sh`](file:///home/gabi/Proyectos/PRD_PulsePass/post-tests/common_e2e.sh) enmascara automáticamente cualquier campo `"accessToken"` en la salida de consola (`"accessToken":"<JWT_OCULTO_POR_SEGURIDAD>"`), evitando que los tokens JWT reales queden expuestos en capturas de pantalla o bitácoras de ejecución.
