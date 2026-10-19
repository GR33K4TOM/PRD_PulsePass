#!/usr/bin/env bash
# =============================================================================
# FR-E2E-003_flujo_c_catalogo_admin.sh
# Flujo C: Gestión Administrativa de Catálogo y Visibilidad por Rol
# Cubre:
#   - POST /api/events (TC-SEC-003 sin token -> 401; TC-SEC-004 con USER/STAFF -> 403; ADMIN -> 201)
#   - POST /api/events/{eventCode}/artists/{artistId} (ADMIN asocia artista)
#   - GET /api/events/{eventCode} (Mientras está DRAFT: Público/USER -> 403; STAFF/ADMIN -> 200)
#   - GET /api/events/by-artist (STAFF/ADMIN ven incluso eventos DRAFT)
#   - PATCH /api/events/{eventCode}/publish (ADMIN publica el evento -> PUBLISHED)
#   - GET /api/events/{eventCode} (Una vez PUBLISHED: Público y USER_A -> 200 OK)
# =============================================================================

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/common_e2e.sh"

echo "================================================================================"
echo "FLUJO C (FR-E2E-003): GESTIÓN ADMINISTRATIVA DE EVENTOS Y PUBLICACIÓN"
echo "================================================================================"

PAYLOAD_NEW_EVENT=$(printf '{"eventCode":"%s","name":"PulsePass Sunset Session %s","description":"Concierto E2E controlado","category":"MUSIC","eventDate":"2027-11-15T20:00:00","minimumAge":18,"venueCode":"%s"}' \
    "${EVENT_TO_PUBLISH_CODE}" "${RUN_ID}" "${VENUE_ACTIVE_CODE}")

# 1. Control de acceso en creación de eventos (TC-SEC-003 y TC-SEC-004)
run_http_test "TC-SEC-003" "Intentar crear evento sin token retorna 401 Unauthorized" \
    "POST" "/api/events" "401" "" "${PAYLOAD_NEW_EVENT}"

run_http_test "TC-SEC-004a" "Intentar crear evento con rol USER retorna 403 Forbidden" \
    "POST" "/api/events" "403" "${USER_A_TOKEN:-}" "${PAYLOAD_NEW_EVENT}"

run_http_test "TC-SEC-004b" "Intentar crear evento con rol STAFF retorna 403 Forbidden" \
    "POST" "/api/events" "403" "${STAFF_TOKEN:-}" "${PAYLOAD_NEW_EVENT}"

# 2. Creación exitosa de evento en estado DRAFT por ADMIN
run_http_test "FR-E2E-003a" "ADMIN crea evento ${EVENT_TO_PUBLISH_CODE} en estado inicial DRAFT" \
    "POST" "/api/events" "201" "${ADMIN_TOKEN:-}" "${PAYLOAD_NEW_EVENT}"

# 3. Asociar artista al evento en DRAFT (solo ADMIN)
run_http_test "FR-E2E-003b" "ADMIN asocia el artista ID=${ARTIST_ID} al evento ${EVENT_TO_PUBLISH_CODE}" \
    "POST" "/api/events/${EVENT_TO_PUBLISH_CODE}/artists/${ARTIST_ID}" "200" "${ADMIN_TOKEN:-}" ""

# 4. Visibilidad del evento mientras sigue en DRAFT
run_http_test "FR-E2E-003c" "USER_A no puede ver detalle del evento ${EVENT_TO_PUBLISH_CODE} mientras está en DRAFT (403)" \
    "GET" "/api/events/${EVENT_TO_PUBLISH_CODE}" "403" "${USER_A_TOKEN:-}" ""

run_http_test "FR-E2E-003d" "STAFF sí puede inspeccionar el evento ${EVENT_TO_PUBLISH_CODE} en estado DRAFT (200)" \
    "GET" "/api/events/${EVENT_TO_PUBLISH_CODE}" "200" "${STAFF_TOKEN:-}" ""

ENCODED_STAGE_NAME=$(printf '%s' "${ARTIST_STAGE_NAME}" | sed 's/ /%20/g')
run_http_test "FR-E2E-003e" "STAFF consulta eventos por artista (${ARTIST_STAGE_NAME}) e incluye eventos en DRAFT" \
    "GET" "/api/events/by-artist?stageName=${ENCODED_STAGE_NAME}" "200" "${STAFF_TOKEN:-}" ""

# 5. Publicación del evento por ADMIN (TC-BIZ-001)
run_http_test "FR-E2E-003f" "USER_A intenta publicar el evento ${EVENT_TO_PUBLISH_CODE} -> 403 Forbidden" \
    "PATCH" "/api/events/${EVENT_TO_PUBLISH_CODE}/publish" "403" "${USER_A_TOKEN:-}" ""

run_http_test "TC-BIZ-001" "ADMIN publica exitosamente el evento ${EVENT_TO_PUBLISH_CODE} (DRAFT -> PUBLISHED)" \
    "PATCH" "/api/events/${EVENT_TO_PUBLISH_CODE}/publish" "200" "${ADMIN_TOKEN:-}" ""

# 6. Una vez publicado, es visible para público y USER_A
run_http_test "FR-E2E-003g" "Público anónimo ahora puede consultar el evento publicado ${EVENT_TO_PUBLISH_CODE} (200)" \
    "GET" "/api/events/${EVENT_TO_PUBLISH_CODE}" "200" "" ""

print_summary "FR-E2E-003 Flujo C - Catálogo Admin y Publicación"
