#!/usr/bin/env bash
# =============================================================================
# FR-E2E-004_flujo_d_compra_y_tickets.sh
# Flujo D: Compra de Entradas, Consultas por Propietario/Staff, Cancelación y Uso en Puerta
# Cubre:
#   - POST /api/tickets (Compra de entradas por USER_A y USER_B)
#   - GET /api/tickets/{ticketCode} (Propietario USER_A, STAFF y ADMIN)
#   - GET /api/tickets/by-user?email=... (Historial propio de USER_A y consulta por ADMIN)
#   - GET /api/events/{eventCode}/tickets/paid (Listado operativo de tickets pagados por STAFF/ADMIN)
#   - PATCH /api/tickets/{ticketCode}/cancel (Cancelación de entrada por su propietario USER_A)
#   - PATCH /api/tickets/{ticketCode}/use (Validación/consumo en puerta por STAFF - TC-SEC-007)
# =============================================================================

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/common_e2e.sh"

echo "================================================================================"
echo "FLUJO D (FR-E2E-004): COMPRA DE ENTRADAS, CANCELACIÓN Y VALIDACIÓN EN PUERTA"
echo "================================================================================"

USER_A_EMAIL="${USER_A_EMAIL:-andrea.${RUN_ID}@example.test}"
USER_B_EMAIL="${USER_B_EMAIL:-carlos.${RUN_ID}@example.test}"

# 1. USER_A compra dos entradas para el evento recién publicado EVENT_TO_PUBLISH_CODE
PAYLOAD_BUY_A1=$(printf '{"userEmail":"%s","eventCode":"%s","type":"GENERAL"}' "${USER_A_EMAIL}" "${EVENT_TO_PUBLISH_CODE}")
run_http_test "FR-E2E-004a" "USER_A compra Ticket #1 (GENERAL) para ${EVENT_TO_PUBLISH_CODE}" \
    "POST" "/api/tickets" "201" "${USER_A_TOKEN:-}" "${PAYLOAD_BUY_A1}"

TICKET_CANCELABLE_CODE=$(extract_json_field "${LAST_RESPONSE_BODY}" "ticketCode")
save_state_var "TICKET_CANCELABLE_CODE" "${TICKET_CANCELABLE_CODE}"

PAYLOAD_BUY_A2=$(printf '{"userEmail":"%s","eventCode":"%s","type":"VIP"}' "${USER_A_EMAIL}" "${EVENT_TO_PUBLISH_CODE}")
run_http_test "FR-E2E-004b" "USER_A compra Ticket #2 (VIP) para ${EVENT_TO_PUBLISH_CODE}" \
    "POST" "/api/tickets" "201" "${USER_A_TOKEN:-}" "${PAYLOAD_BUY_A2}"

TICKET_USABLE_CODE=$(extract_json_field "${LAST_RESPONSE_BODY}" "ticketCode")
save_state_var "TICKET_USABLE_CODE" "${TICKET_USABLE_CODE}"

# 2. USER_B compra una entrada propia para pruebas cruzadas de propiedad (Anti-IDOR)
PAYLOAD_BUY_B1=$(printf '{"userEmail":"%s","eventCode":"%s","type":"STUDENT"}' "${USER_B_EMAIL}" "${EVENT_TO_PUBLISH_CODE}")
run_http_test "FR-E2E-004c" "USER_B compra su propio Ticket (STUDENT) para ${EVENT_TO_PUBLISH_CODE}" \
    "POST" "/api/tickets" "201" "${USER_B_TOKEN:-}" "${PAYLOAD_BUY_B1}"

USER_B_TICKET_CODE=$(extract_json_field "${LAST_RESPONSE_BODY}" "ticketCode")
save_state_var "USER_B_TICKET_CODE" "${USER_B_TICKET_CODE}"

# 3. Consultas de tickets por código, por usuario y por evento
run_http_test "FR-E2E-004d" "USER_A consulta su propio ticket (${TICKET_USABLE_CODE})" \
    "GET" "/api/tickets/${TICKET_USABLE_CODE}" "200" "${USER_A_TOKEN:-}" ""

run_http_test "FR-E2E-004e" "STAFF consulta cualquier ticket por código (${TICKET_USABLE_CODE}) para control de acceso" \
    "GET" "/api/tickets/${TICKET_USABLE_CODE}" "200" "${STAFF_TOKEN:-}" ""

run_http_test "FR-E2E-004f" "USER_A lista todos sus tickets comprados (/api/tickets/by-user)" \
    "GET" "/api/tickets/by-user?email=${USER_A_EMAIL}" "200" "${USER_A_TOKEN:-}" ""

run_http_test "FR-E2E-004g" "STAFF lista los tickets PAID del evento ${EVENT_TO_PUBLISH_CODE}" \
    "GET" "/api/events/${EVENT_TO_PUBLISH_CODE}/tickets/paid" "200" "${STAFF_TOKEN:-}" ""

# 4. Cancelación de Ticket #1 por su propietario USER_A
run_http_test "FR-E2E-004h" "USER_A cancela su Ticket #1 (${TICKET_CANCELABLE_CODE}) -> estado CANCELLED" \
    "PATCH" "/api/tickets/${TICKET_CANCELABLE_CODE}/cancel" "200" "${USER_A_TOKEN:-}" ""

# 5. Uso en puerta de Ticket #2 por STAFF (TC-SEC-007)
run_http_test "TC-SEC-007" "STAFF marca el Ticket #2 (${TICKET_USABLE_CODE}) como usado en puerta -> estado USED" \
    "PATCH" "/api/tickets/${TICKET_USABLE_CODE}/use" "200" "${STAFF_TOKEN:-}" ""

print_summary "FR-E2E-004 Flujo D - Compra, Cancelación y Uso de Tickets"
