#!/usr/bin/env bash
# =============================================================================
# FR-E2E-005_flujo_e_seguridad_y_negativos.sh
# Flujo E: Pruebas de Seguridad, Anti-IDOR, Integridad JWT y Reglas de Negocio Negativas
# Cubre:
#   - TC-AUTH-008: Encabezado Authorization mal formado (sin prefijo Bearer) -> 401
#   - TC-AUTH-009: Token JWT con firma manipulada -> 401
#   - TC-AUTH-010: Token JWT malformado/inválido -> 401
#   - TC-SEC-006 : Anti-IDOR sobre tickets (leer, listar, comprar o cancelar con identidad ajena) -> 403
#   - TC-SEC-008 : Operaciones exclusivas de STAFF/ADMIN intentadas por USER -> 403
#   - TC-SEC-009 : Intento de escalamiento de privilegios en POST /api/users -> rol sigue siendo USER
#   - TC-BIZ-002 : Compra sobre evento en estado DRAFT -> 409 Conflict
#   - TC-BIZ-003 : Límite de aforo (capacidad = 3 en CMF-2026) y transición a SOLD_OUT -> 409 Conflict
#   - TC-BIZ-004 : Crear evento en recinto inactivo o con fecha pasada -> 409 Conflict
#   - TC-BIZ-006 : Cancelar un ticket ya CANCELLED o ya USED -> 409 Conflict
#   - TC-BIZ-007 : Reutilizar un ticket que ya está en estado USED -> 409 Conflict
#   - TC-BIZ-008 : Intentar usar en puerta un ticket CANCELLED -> 409 Conflict
# =============================================================================

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/common_e2e.sh"

echo "================================================================================"
echo "FLUJO E (FR-E2E-005): SEGURIDAD, ANTI-IDOR, TOKENS INVÁLIDOS Y REGLAS DE NEGOCIO"
echo "================================================================================"

USER_A_EMAIL="${USER_A_EMAIL:-andrea.${RUN_ID}@example.test}"
USER_B_EMAIL="${USER_B_EMAIL:-carlos.${RUN_ID}@example.test}"
VENUE_INACTIVE_CODE="${VENUE_INACTIVE_CODE:-VEN-INACTIVE-01}"

# 1. Validación estricta del encabezado Authorization y firma JWT (TC-AUTH-008 a TC-AUTH-010)
echo "--------------------------------------------------------------------------------"
echo "[TC-AUTH-008] Encabezado Authorization sin esquema 'Bearer ' retorna 401 Unauthorized"
RAW_NON_BEARER=$(curl -s -X GET "${BASE_URL}/api/auth/me" -H "Authorization: Token ${USER_A_TOKEN:-abc}" -w "\n%{http_code}")
STATUS_NON_BEARER=$(printf '%s\n' "${RAW_NON_BEARER}" | tail -n 1 | tr -d '\r')
if [ "${STATUS_NON_BEARER}" = "401" ]; then
    echo "  Estado   : HTTP ${STATUS_NON_BEARER} (Esperado: 401) -> [PASS]"
    TOTAL_PASS=$((TOTAL_PASS + 1))
else
    echo "  Estado   : HTTP ${STATUS_NON_BEARER} (Esperado: 401) -> [FAIL]"
    TOTAL_FAIL=$((TOTAL_FAIL + 1))
fi

TAMPERED_TOKEN="${USER_A_TOKEN:-eyJhbGciOiJIUzI1NiJ9.e30.invalido}tampered"
run_http_test "TC-AUTH-009" "Token JWT con firma manipulada retorna 401 Unauthorized" \
    "GET" "/api/auth/me" "401" "${TAMPERED_TOKEN}" ""

run_http_test "TC-AUTH-010" "Token JWT malformado/corrupto retorna 401 Unauthorized" \
    "GET" "/api/auth/me" "401" "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIn0." ""

# 2. Anti-IDOR en Tickets (TC-SEC-006)
run_http_test "TC-SEC-006a" "Anti-IDOR: USER_A intenta leer el ticket de USER_B (${USER_B_TICKET_CODE:-}) -> ${EXPECTED_FOREIGN_RESOURCE_STATUS}" \
    "GET" "/api/tickets/${USER_B_TICKET_CODE:-}" "${EXPECTED_FOREIGN_RESOURCE_STATUS}" "${USER_A_TOKEN:-}" ""

run_http_test "TC-SEC-006b" "Anti-IDOR: USER_A intenta listar tickets de USER_B por email -> ${EXPECTED_FOREIGN_RESOURCE_STATUS}" \
    "GET" "/api/tickets/by-user?email=${USER_B_EMAIL}" "${EXPECTED_FOREIGN_RESOURCE_STATUS}" "${USER_A_TOKEN:-}" ""

run_http_test "TC-SEC-006c" "Anti-IDOR: USER_A intenta cancelar el ticket de USER_B (${USER_B_TICKET_CODE:-}) -> ${EXPECTED_FOREIGN_RESOURCE_STATUS}" \
    "PATCH" "/api/tickets/${USER_B_TICKET_CODE:-}/cancel" "${EXPECTED_FOREIGN_RESOURCE_STATUS}" "${USER_A_TOKEN:-}" ""

PAYLOAD_IMPERSONATE_BUY=$(printf '{"userEmail":"%s","eventCode":"%s","type":"GENERAL"}' "${USER_B_EMAIL}" "${EVENT_TO_PUBLISH_CODE}")
run_http_test "TC-SEC-006d" "Anti-IDOR: USER_A intenta comprar ticket a nombre de USER_B -> ${EXPECTED_FOREIGN_RESOURCE_STATUS}" \
    "POST" "/api/tickets" "${EXPECTED_FOREIGN_RESOURCE_STATUS}" "${USER_A_TOKEN:-}" "${PAYLOAD_IMPERSONATE_BUY}"

# 3. Separación de privilegios por Rol (TC-SEC-008)
run_http_test "TC-SEC-008a" "USER_A intenta usar un ticket en puerta (PATCH /use exclusivo de STAFF/ADMIN) -> 403" \
    "PATCH" "/api/tickets/${USER_B_TICKET_CODE:-}/use" "403" "${USER_A_TOKEN:-}" ""

run_http_test "TC-SEC-008b" "USER_A intenta listar tickets pagados de un evento (exclusivo de STAFF/ADMIN) -> 403" \
    "GET" "/api/events/${EVENT_TO_PUBLISH_CODE}/tickets/paid" "403" "${USER_A_TOKEN:-}" ""

run_http_test "TC-SEC-008c" "STAFF intenta comprar un ticket (POST /api/tickets exclusivo de USER) -> 403" \
    "POST" "/api/tickets" "403" "${STAFF_TOKEN:-}" "${PAYLOAD_IMPERSONATE_BUY}"

# 4. Prevención de escalamiento de privilegios en registro público (TC-SEC-009)
ESC_USER="esc_${RUN_ID}"
ESC_EMAIL="esc.${RUN_ID}@example.test"
PAYLOAD_ESCALATION=$(printf '{"username":"%s","email":"%s","firstName":"Intento","lastName":"Admin","phone":"3009998877","city":"Bogota","birthDate":"1995-01-01","password":"%s","role":"ADMIN","roles":["ADMIN","STAFF"]}' \
    "${ESC_USER}" "${ESC_EMAIL}" "${USER_A_PASSWORD:-}")

run_http_test "TC-SEC-009a" "Registro público enviando campos extra role=ADMIN ignora privilegios y crea cuenta" \
    "POST" "/api/users" "201" "" "${PAYLOAD_ESCALATION}"

LOGIN_ESC=$(printf '{"identifier":"%s","password":"%s"}' "${ESC_USER}" "${USER_A_PASSWORD:-}")
run_http_test "TC-SEC-009b" "Login de cuenta creada con intento de escalamiento" \
    "POST" "/api/auth/login" "200" "" "${LOGIN_ESC}"
ESC_TOKEN=$(extract_json_field "${LAST_RESPONSE_BODY}" "accessToken")

run_http_test "TC-SEC-009c" "Verificar en /api/auth/me que la cuenta solo posee ROLE_USER y no ROLE_ADMIN" \
    "GET" "/api/auth/me" "200" "${ESC_TOKEN}" ""

if printf '%s' "${LAST_RESPONSE_BODY}" | grep -Eq 'ADMIN|STAFF'; then
    echo "  [FAIL] Escalamiento de privilegios detectado en /api/auth/me"
    TOTAL_FAIL=$((TOTAL_FAIL + 1))
else
    echo "  [PASS] Prevención de escalamiento confirmada: el usuario solo recibió ROLE_USER"
    TOTAL_PASS=$((TOTAL_PASS + 1))
fi

# 5. Reglas de negocio negativas sobre eventos y tickets (TC-BIZ-002 a TC-BIZ-008)
PAYLOAD_BUY_DRAFT=$(printf '{"userEmail":"%s","eventCode":"%s","type":"GENERAL"}' "${USER_A_EMAIL}" "${DRAFT_EVENT_CODE}")
run_http_test "TC-BIZ-002" "Comprar ticket para un evento en estado DRAFT (${DRAFT_EVENT_CODE}) retorna 409 Conflict" \
    "POST" "/api/tickets" "409" "${USER_A_TOKEN:-}" "${PAYLOAD_BUY_DRAFT}"

PAYLOAD_INACTIVE_VENUE=$(printf '{"eventCode":"EVT-INACT-%s","name":"Evento en Recinto Cerrado","description":"Debe fallar","category":"CONCERT","eventDate":"2027-12-01T20:00:00","minimumAge":18,"venueCode":"%s"}' \
    "${RUN_ID}" "${VENUE_INACTIVE_CODE}")
run_http_test "TC-BIZ-004" "ADMIN intenta crear evento en recinto inactivo (${VENUE_INACTIVE_CODE}) -> 409 Conflict" \
    "POST" "/api/events" "409" "${ADMIN_TOKEN:-}" "${PAYLOAD_INACTIVE_VENUE}"

run_http_test "TC-BIZ-006" "USER_A intenta cancelar un ticket que ya fue usado (${TICKET_USABLE_CODE:-}) -> 409 Conflict" \
    "PATCH" "/api/tickets/${TICKET_USABLE_CODE:-}/cancel" "409" "${USER_A_TOKEN:-}" ""

run_http_test "TC-BIZ-007" "STAFF intenta volver a usar un ticket ya USED (${TICKET_USABLE_CODE:-}) -> 409 Conflict" \
    "PATCH" "/api/tickets/${TICKET_USABLE_CODE:-}/use" "409" "${STAFF_TOKEN:-}" ""

run_http_test "TC-BIZ-008" "STAFF intenta usar en puerta un ticket CANCELLED (${TICKET_CANCELABLE_CODE:-}) -> 409 Conflict" \
    "PATCH" "/api/tickets/${TICKET_CANCELABLE_CODE:-}/use" "409" "${STAFF_TOKEN:-}" ""

# 6. Agotamiento de capacidad (capacity = 3 en VEN-SMR-01 sobre el evento CMF-2026) -> TC-BIZ-003
PAYLOAD_CAPACITY=$(printf '{"userEmail":"%s","eventCode":"%s","type":"GENERAL"}' "${USER_A_EMAIL}" "${PUBLISHED_EVENT_CODE}")
run_http_test "TC-BIZ-003a" "Compra #1 en ${PUBLISHED_EVENT_CODE} (cupo 1 de 3)" \
    "POST" "/api/tickets" "201" "${USER_A_TOKEN:-}" "${PAYLOAD_CAPACITY}"
run_http_test "TC-BIZ-003b" "Compra #2 en ${PUBLISHED_EVENT_CODE} (cupo 2 de 3)" \
    "POST" "/api/tickets" "201" "${USER_A_TOKEN:-}" "${PAYLOAD_CAPACITY}"
run_http_test "TC-BIZ-003c" "Compra #3 en ${PUBLISHED_EVENT_CODE} (cupo 3 de 3 -> evento pasa a SOLD_OUT)" \
    "POST" "/api/tickets" "201" "${USER_A_TOKEN:-}" "${PAYLOAD_CAPACITY}"
run_http_test "TC-BIZ-003d" "Compra #4 en ${PUBLISHED_EVENT_CODE} agotado (SOLD_OUT) es rechazada con 409 Conflict" \
    "POST" "/api/tickets" "409" "${USER_A_TOKEN:-}" "${PAYLOAD_CAPACITY}"

print_summary "FR-E2E-005 Flujo E - Seguridad, Anti-IDOR y Reglas de Negocio"
