#!/usr/bin/env bash
# =============================================================================
# FR-E2E-001_flujo_a_descubrimiento_publico.sh
# Flujo A: Descubrimiento Público (Sin Token Bearer)
# Cubre:
#   - GET /api/venues/active
#   - GET /api/venues/{code}
#   - GET /api/artists/active
#   - GET /api/artists/{id}
#   - GET /api/artists/by-stage-name
#   - GET /api/events/published
#   - GET /api/events/by-artist (Público: filtra solo PUBLISHED)
#   - GET /api/events/{eventCode} (PUBLISHED -> 200 OK; DRAFT -> 403 Forbidden)
# =============================================================================

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/common_e2e.sh"

echo "================================================================================"
echo "FLUJO A (FR-E2E-001): DESCUBRIMIENTO PÚBLICO (SIN TOKEN)"
echo "================================================================================"

# 1. Venues públicos
run_http_test "TC-SEC-001a" "Listar recintos activos sin token (Público)" \
    "GET" "/api/venues/active" "200" "" ""

run_http_test "TC-SEC-001b" "Consultar recinto por código (${VENUE_ACTIVE_CODE}) sin token" \
    "GET" "/api/venues/${VENUE_ACTIVE_CODE}" "200" "" ""

run_http_test "TC-BIZ-005a" "Consultar recinto inexistente retorna 404 Not Found" \
    "GET" "/api/venues/VEN-NO-EXISTE-999" "404" "" ""

# 2. Artistas públicos
run_http_test "TC-SEC-001c" "Listar artistas activos sin token (Público)" \
    "GET" "/api/artists/active" "200" "" ""

run_http_test "TC-SEC-001d" "Consultar artista por ID (${ARTIST_ID}) sin token" \
    "GET" "/api/artists/${ARTIST_ID}" "200" "" ""

ENCODED_STAGE_NAME=$(printf '%s' "${ARTIST_STAGE_NAME}" | sed 's/ /%20/g')
run_http_test "TC-SEC-001e" "Consultar artista por nombre artístico (${ARTIST_STAGE_NAME}) sin token" \
    "GET" "/api/artists/by-stage-name?stageName=${ENCODED_STAGE_NAME}" "200" "" ""

# 3. Eventos públicos y restricción sobre eventos DRAFT
run_http_test "TC-SEC-001f" "Listar eventos publicados sin token (Público)" \
    "GET" "/api/events/published" "200" "" ""

run_http_test "TC-SEC-001g" "Consultar eventos por artista sin token (solo retorna PUBLISHED)" \
    "GET" "/api/events/by-artist?stageName=${ENCODED_STAGE_NAME}" "200" "" ""

# Verificar que el evento DRAFT no se cuele en la consulta pública por artista
if printf '%s' "${LAST_RESPONSE_BODY}" | grep -q "\"${DRAFT_EVENT_CODE}\""; then
    echo "  [FAIL] El evento DRAFT (${DRAFT_EVENT_CODE}) apareció en el listado público por artista"
    TOTAL_FAIL=$((TOTAL_FAIL + 1))
else
    echo "  [PASS] El listado público por artista oculta correctamente los eventos en estado DRAFT"
    TOTAL_PASS=$((TOTAL_PASS + 1))
fi

run_http_test "TC-SEC-001h" "Consultar detalle de evento PUBLISHED (${PUBLISHED_EVENT_CODE}) sin token" \
    "GET" "/api/events/${PUBLISHED_EVENT_CODE}" "200" "" ""

run_http_test "TC-SEC-002" "Bloquear consulta anónima de evento en estado DRAFT (${DRAFT_EVENT_CODE})" \
    "GET" "/api/events/${DRAFT_EVENT_CODE}" "403" "" ""

print_summary "FR-E2E-001 Flujo A - Descubrimiento Público"
