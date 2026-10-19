# Checklist de Validación E2E — PulsePass

Lista de verificación alineada con la **Sección 18** y **Sección 23 (DoR)** del documento `PRD-PulsePassPruebas-E2E-Postman-Manual.pdf`.

## 1. Precondiciones (Definition of Ready)
- [x] PostgreSQL en ejecución y accesible.
- [x] Migraciones Flyway `V1` a `V4` aplicadas sin errores.
- [x] Variable `PULSEPASS_JWT_SECRET_BASE64` configurada con >= 32 bytes en Base64.
- [x] Plantilla `post-tests/.env.e2e.example` copiada a `post-tests/.env.e2e` con contraseñas locales válidas.
- [x] Script `post-tests/00_setup_fixtures.sh` ejecutado (`VEN-SMR-01`, `VEN-INACTIVE-01`, `EVT-SEED-DRAFT`, `CMF-2026`, `admin_e2e`, `laura_staff`, `miguel_inactivo`).

## 2. Verificación de Flujos y Seguridad
- [x] **Flujo A (`FR-E2E-001`):** Endpoints públicos responden `200 OK` sin token y los eventos `DRAFT` permanecen ocultos al público (`403 Forbidden` en detalle directo).
- [x] **Flujo B (`FR-E2E-002`):** Registro público crea cuentas con `ROLE_USER` sin filtrar `passwordHash`; login emite JWT HS256 con `expiresIn=900`; `/api/auth/me` identifica al principal autenticado; un `USER` no puede consultar el perfil de otro usuario (`403 Forbidden`).
- [x] **Flujo C (`FR-E2E-003`):** Solo `ADMIN` puede crear eventos, asociar artistas y publicar eventos (`DRAFT` -> `PUBLISHED`).
- [x] **Flujo D (`FR-E2E-004`):** `USER` compra entradas para sí mismo, consulta sus entradas y cancela una entrada pagada; `STAFF` consulta entradas pagadas del evento y valida una entrada en puerta (`USED`).
- [x] **Flujo E (`FR-E2E-005`):** Tokens manipulados o sin prefijo `Bearer ` retornan `401 Unauthorized`; intentos de leer/cancelar/comprar con identidad ajena retornan `403 Forbidden`; intentos de escalamiento de rol en `POST /api/users` son neutralizados; violaciones de reglas de negocio (evento `DRAFT`, recinto inactivo, ticket ya usado/cancelado, aforo `SOLD_OUT`) retornan `409 Conflict`.
