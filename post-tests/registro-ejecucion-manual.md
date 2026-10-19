# Registro de Ejecución de Pruebas E2E — PulsePass

Bitácora de resultados de la suite de scripts `post-tests/` (`Bash + cURL`).

| ID Caso | Script | Método y Ruta | Actor | HTTP Esperado | HTTP Obtenido | Resultado |
|---|---|---|---|---|---|---|
| `TC-SEC-001a` | `FR-E2E-001` | `GET /api/venues/active` | Anónimo | `200` | `200` | PASS |
| `TC-SEC-001b` | `FR-E2E-001` | `GET /api/venues/{code}` | Anónimo | `200` | `200` | PASS |
| `TC-BIZ-005a` | `FR-E2E-001` | `GET /api/venues/VEN-NO-EXISTE-999` | Anónimo | `404` | `404` | PASS |
| `TC-SEC-001c` | `FR-E2E-001` | `GET /api/artists/active` | Anónimo | `200` | `200` | PASS |
| `TC-SEC-001d` | `FR-E2E-001` | `GET /api/artists/{id}` | Anónimo | `200` | `200` | PASS |
| `TC-SEC-001e` | `FR-E2E-001` | `GET /api/artists/by-stage-name` | Anónimo | `200` | `200` | PASS |
| `TC-SEC-001f` | `FR-E2E-001` | `GET /api/events/published` | Anónimo | `200` | `200` | PASS |
| `TC-SEC-001g` | `FR-E2E-001` | `GET /api/events/by-artist` | Anónimo | `200` | `200` | PASS |
| `TC-SEC-001h` | `FR-E2E-001` | `GET /api/events/CMF-2026` | Anónimo | `200` | `200` | PASS |
| `TC-SEC-002` | `FR-E2E-001` | `GET /api/events/EVT-SEED-DRAFT` | Anónimo | `403` | `403` | PASS |
| `FR-E2E-002a` | `FR-E2E-002` | `POST /api/users` (`USER_A`) | Anónimo | `201` | `201` | PASS |
| `FR-E2E-002b` | `FR-E2E-002` | `POST /api/users` (`USER_B`) | Anónimo | `201` | `201` | PASS |
| `TC-AUTH-001` | `FR-E2E-002` | `POST /api/auth/login` (username) | `USER_A` | `200` | `200` | PASS |
| `TC-AUTH-002` | `FR-E2E-002` | `POST /api/auth/login` (email) | `USER_A` | `200` | `200` | PASS |
| `TC-AUTH-003` | `FR-E2E-002` | `POST /api/auth/login` (password erróneo) | `USER_A` | `401` | `401` | PASS |
| `TC-AUTH-004` | `FR-E2E-002` | `POST /api/auth/login` (inactivo) | `INACTIVE_USER` | `401` | `401` | PASS |
| `TC-AUTH-005` | `FR-E2E-002` | `POST /api/auth/login` (payload vacío) | Anónimo | `400` | `400` | PASS |
| `TC-AUTH-006` | `FR-E2E-002` | `GET /api/auth/me` | `USER_A` | `200` | `200` | PASS |
| `TC-AUTH-007` | `FR-E2E-002` | `GET /api/auth/me` | Anónimo | `401` | `401` | PASS |
| `TC-SEC-005a` | `FR-E2E-002` | `GET /api/users/by-email` (ajeno) | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-005b` | `FR-E2E-002` | `GET /api/users/by-username` (ajeno) | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-003` | `FR-E2E-003` | `POST /api/events` | Anónimo | `401` | `401` | PASS |
| `TC-SEC-004a` | `FR-E2E-003` | `POST /api/events` | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-004b` | `FR-E2E-003` | `POST /api/events` | `STAFF` | `403` | `403` | PASS |
| `FR-E2E-003a` | `FR-E2E-003` | `POST /api/events` | `ADMIN` | `201` | `201` | PASS |
| `FR-E2E-003b` | `FR-E2E-003` | `POST /api/events/{code}/artists/{id}` | `ADMIN` | `200` | `200` | PASS |
| `TC-BIZ-001` | `FR-E2E-003` | `PATCH /api/events/{code}/publish` | `ADMIN` | `200` | `200` | PASS |
| `FR-E2E-004a` | `FR-E2E-004` | `POST /api/tickets` (Ticket #1) | `USER_A` | `201` | `201` | PASS |
| `FR-E2E-004b` | `FR-E2E-004` | `POST /api/tickets` (Ticket #2) | `USER_A` | `201` | `201` | PASS |
| `FR-E2E-004c` | `FR-E2E-004` | `POST /api/tickets` (Ticket `USER_B`) | `USER_B` | `201` | `201` | PASS |
| `FR-E2E-004g` | `FR-E2E-004` | `GET /api/events/{code}/tickets/paid` | `STAFF` | `200` | `200` | PASS |
| `FR-E2E-004h` | `FR-E2E-004` | `PATCH /api/tickets/{code}/cancel` | `USER_A` | `200` | `200` | PASS |
| `TC-SEC-007` | `FR-E2E-004` | `PATCH /api/tickets/{code}/use` | `STAFF` | `200` | `200` | PASS |
| `TC-AUTH-008` | `FR-E2E-005` | `GET /api/auth/me` (esquema no Bearer) | `USER_A` | `401` | `401` | PASS |
| `TC-AUTH-009` | `FR-E2E-005` | `GET /api/auth/me` (firma manipulada) | `USER_A` | `401` | `401` | PASS |
| `TC-AUTH-010` | `FR-E2E-005` | `GET /api/auth/me` (JWT corrupto) | Anónimo | `401` | `401` | PASS |
| `TC-SEC-006a` | `FR-E2E-005` | `GET /api/tickets/{code}` (ajeno) | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-006b` | `FR-E2E-005` | `GET /api/tickets/by-user` (ajeno) | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-006c` | `FR-E2E-005` | `PATCH /api/tickets/{code}/cancel` (ajeno) | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-006d` | `FR-E2E-005` | `POST /api/tickets` (email ajeno) | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-008a` | `FR-E2E-005` | `PATCH /api/tickets/{code}/use` | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-008b` | `FR-E2E-005` | `GET /api/events/{code}/tickets/paid` | `USER_A` | `403` | `403` | PASS |
| `TC-SEC-008c` | `FR-E2E-005` | `POST /api/tickets` | `STAFF` | `403` | `403` | PASS |
| `TC-SEC-009` | `FR-E2E-005` | `POST /api/users` (intento rol ADMIN) | Anónimo | `201` (`ROLE_USER`) | `201` (`ROLE_USER`) | PASS |
| `TC-BIZ-002` | `FR-E2E-005` | `POST /api/tickets` (evento `DRAFT`) | `USER_A` | `409` | `409` | PASS |
| `TC-BIZ-003` | `FR-E2E-005` | `POST /api/tickets` (sobreventa `SOLD_OUT`) | `USER_A` | `409` | `409` | PASS |
| `TC-BIZ-004` | `FR-E2E-005` | `POST /api/events` (venue inactivo) | `ADMIN` | `409` | `409` | PASS |
| `TC-BIZ-006` | `FR-E2E-005` | `PATCH /api/tickets/{code}/cancel` (ya `USED`) | `USER_A` | `409` | `409` | PASS |
| `TC-BIZ-007` | `FR-E2E-005` | `PATCH /api/tickets/{code}/use` (ya `USED`) | `STAFF` | `409` | `409` | PASS |
| `TC-BIZ-008` | `FR-E2E-005` | `PATCH /api/tickets/{code}/use` (`CANCELLED`) | `STAFF` | `409` | `409` | PASS |
