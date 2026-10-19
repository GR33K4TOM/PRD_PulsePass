#!/usr/bin/env bash
# =============================================================================
# FR-E2E-002_flujo_b_registro_y_jwt.sh
# Flujo B: Registro Público, Autenticación JWT, /api/auth/me y Consultas de Usuario
# Cubre:
#   - POST /api/users (Registro de USER_A y USER_B con rol inicial USER)
#   - POST /api/auth/login (TC-AUTH-001 a TC-AUTH-005: username, email, password inválido,
#     usuario inactivo, JSON inválido)
#   - GET /api/auth/me (TC-AUTH-006 con token y TC-AUTH-007 sin token)
#   - GET /api/users/by-email y GET /api/users/by-username (Propios vs ajenos TC-SEC-005 vs ADMIN)
# =============================================================================

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/common_e2e.sh"

echo "================================================================================"
echo "FLUJO B (FR-E2E-002): REGISTRO PÚBLICO, LOGIN JWT Y PERFIL (/api/auth/me)"
echo "================================================================================"

USER_A_USERNAME="${USER_A_USERNAME:-andrea_${RUN_ID}}"
USER_A_EMAIL="${USER_A_EMAIL:-andrea.${RUN_ID}@example.test}"
USER_A_PASSWORD="${USER_A_PASSWORD:-}"

USER_B_USERNAME="${USER_B_USERNAME:-carlos_${RUN_ID}}"
USER_B_EMAIL="${USER_B_EMAIL:-carlos.${RUN_ID}@example.test}"
USER_B_PASSWORD="${USER_B_PASSWORD:-}"

ADMIN_IDENTIFIER="${ADMIN_IDENTIFIER:-admin_e2e}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
STAFF_IDENTIFIER="${STAFF_IDENTIFIER:-laura_staff}"
STAFF_PASSWORD="${STAFF_PASSWORD:-}"
INACTIVE_IDENTIFIER="${INACTIVE_IDENTIFIER:-miguel_inactivo}"
INACTIVE_PASSWORD="${INACTIVE_PASSWORD:-}"

# 1. Registro público de USER_A y USER_B
PAYLOAD_USER_A=$(printf '{"username":"%s","email":"%s","firstName":"Andrea","lastName":"Gomez","phone":"3001234567","city":"Santa Marta","birthDate":"1998-05-14","password":"%s"}' \
    "${USER_A_USERNAME}" "${USER_A_EMAIL}" "${USER_A_PASSWORD}")

run_http_test "FR-E2E-002a" "Registrar públicamente USER_A (${USER_A_USERNAME})" \
    "POST" "/api/users" "201" "" "${PAYLOAD_USER_A}"

# Verificar que la respuesta de registro nunca expone password ni passwordHash
if printf '%s' "${LAST_RESPONSE_BODY}" | grep -Eq '"password"|"passwordHash"'; then
    echo "  [FAIL] La respuesta de POST /api/users expuso credenciales sensibles"
    TOTAL_FAIL=$((TOTAL_FAIL + 1))
else
    echo "  [PASS] POST /api/users no filtra password ni passwordHash en el DTO"
    TOTAL_PASS=$((TOTAL_PASS + 1))
fi

PAYLOAD_USER_B=$(printf '{"username":"%s","email":"%s","firstName":"Carlos","lastName":"Ruiz","phone":"3007654321","city":"Barranquilla","birthDate":"1999-09-20","password":"%s"}' \
    "${USER_B_USERNAME}" "${USER_B_EMAIL}" "${USER_B_PASSWORD}")

run_http_test "FR-E2E-002b" "Registrar públicamente USER_B (${USER_B_USERNAME})" \
    "POST" "/api/users" "201" "" "${PAYLOAD_USER_B}"

# 2. Login por username y por email (TC-AUTH-001 y TC-AUTH-002)
LOGIN_USER_A_BY_USERNAME=$(printf '{"identifier":"%s","password":"%s"}' "${USER_A_USERNAME}" "${USER_A_PASSWORD}")
run_http_test "TC-AUTH-001" "Login válido por username (${USER_A_USERNAME}) retorna JWT Bearer" \
    "POST" "/api/auth/login" "200" "" "${LOGIN_USER_A_BY_USERNAME}"

USER_A_TOKEN=$(extract_json_field "${LAST_RESPONSE_BODY}" "accessToken")
TOKEN_TYPE=$(extract_json_field "${LAST_RESPONSE_BODY}" "tokenType")
EXPIRES_IN=$(extract_json_field "${LAST_RESPONSE_BODY}" "expiresIn")

if [ -n "${USER_A_TOKEN}" ] && [ "${TOKEN_TYPE}" = "Bearer" ] && [ "${EXPIRES_IN}" = "${EXPECTED_TOKEN_TTL_SECONDS}" ]; then
    echo "  [PASS] Contrato AuthResponse verificado (tokenType=Bearer, expiresIn=${EXPIRES_IN})"
    TOTAL_PASS=$((TOTAL_PASS + 1))
    save_state_var "USER_A_TOKEN" "${USER_A_TOKEN}"
else
    echo "  [FAIL] Contrato AuthResponse inválido para USER_A"
    TOTAL_FAIL=$((TOTAL_FAIL + 1))
fi

LOGIN_USER_A_BY_EMAIL=$(printf '{"identifier":"%s","password":"%s"}' "${USER_A_EMAIL}" "${USER_A_PASSWORD}")
run_http_test "TC-AUTH-002" "Login válido por email (${USER_A_EMAIL}) retorna JWT Bearer" \
    "POST" "/api/auth/login" "200" "" "${LOGIN_USER_A_BY_EMAIL}"

# Login de USER_B, STAFF y ADMIN para guardar sus tokens en .tokens.env
LOGIN_USER_B=$(printf '{"identifier":"%s","password":"%s"}' "${USER_B_USERNAME}" "${USER_B_PASSWORD}")
run_http_test "FR-E2E-002c" "Login válido de USER_B (${USER_B_USERNAME})" \
    "POST" "/api/auth/login" "200" "" "${LOGIN_USER_B}"
USER_B_TOKEN=$(extract_json_field "${LAST_RESPONSE_BODY}" "accessToken")
save_state_var "USER_B_TOKEN" "${USER_B_TOKEN}"

LOGIN_STAFF=$(printf '{"identifier":"%s","password":"%s"}' "${STAFF_IDENTIFIER}" "${STAFF_PASSWORD}")
run_http_test "FR-E2E-002d" "Login válido de STAFF (${STAFF_IDENTIFIER})" \
    "POST" "/api/auth/login" "200" "" "${LOGIN_STAFF}"
STAFF_TOKEN=$(extract_json_field "${LAST_RESPONSE_BODY}" "accessToken")
save_state_var "STAFF_TOKEN" "${STAFF_TOKEN}"

LOGIN_ADMIN=$(printf '{"identifier":"%s","password":"%s"}' "${ADMIN_IDENTIFIER}" "${ADMIN_PASSWORD}")
run_http_test "FR-E2E-002e" "Login válido de ADMIN (${ADMIN_IDENTIFIER})" \
    "POST" "/api/auth/login" "200" "" "${LOGIN_ADMIN}"
ADMIN_TOKEN=$(extract_json_field "${LAST_RESPONSE_BODY}" "accessToken")
save_state_var "ADMIN_TOKEN" "${ADMIN_TOKEN}"

# 3. Casos negativos de Login (TC-AUTH-003, TC-AUTH-004, TC-AUTH-005)
LOGIN_WRONG_PASS=$(printf '{"identifier":"%s","password":"PasswordIncorrecta999!"}' "${USER_A_USERNAME}")
run_http_test "TC-AUTH-003" "Login con contraseña incorrecta retorna 401 Unauthorized" \
    "POST" "/api/auth/login" "401" "" "${LOGIN_WRONG_PASS}"

LOGIN_INACTIVE=$(printf '{"identifier":"%s","password":"%s"}' "${INACTIVE_IDENTIFIER}" "${INACTIVE_PASSWORD}")
run_http_test "TC-AUTH-004" "Login de usuario inactivo (${INACTIVE_IDENTIFIER}) retorna 401 Unauthorized" \
    "POST" "/api/auth/login" "401" "" "${LOGIN_INACTIVE}"

run_http_test "TC-AUTH-005" "Login con payload vacío/inválido retorna 400 Bad Request" \
    "POST" "/api/auth/login" "400" "" '{"identifier":"","password":""}'

# 4. Identidad actual (/api/auth/me)
run_http_test "TC-AUTH-006" "Consultar /api/auth/me con token válido de USER_A retorna 200 OK y rol USER" \
    "GET" "/api/auth/me" "200" "${USER_A_TOKEN}" ""

run_http_test "TC-AUTH-007" "Consultar /api/auth/me sin token retorna 401 Unauthorized" \
    "GET" "/api/auth/me" "401" "" ""

# 5. Consultas de perfil propio vs ajeno (Anti-IDOR TC-SEC-005) vs ADMIN
run_http_test "FR-E2E-002f" "USER_A consulta su propio perfil por email" \
    "GET" "/api/users/by-email?email=${USER_A_EMAIL}" "200" "${USER_A_TOKEN}" ""

run_http_test "FR-E2E-002g" "USER_A consulta su propio perfil por username" \
    "GET" "/api/users/by-username?username=${USER_A_USERNAME}" "200" "${USER_A_TOKEN}" ""

run_http_test "TC-SEC-005a" "Anti-IDOR: USER_A intenta consultar perfil de USER_B por email -> ${EXPECTED_FOREIGN_RESOURCE_STATUS}" \
    "GET" "/api/users/by-email?email=${USER_B_EMAIL}" "${EXPECTED_FOREIGN_RESOURCE_STATUS}" "${USER_A_TOKEN}" ""

run_http_test "TC-SEC-005b" "Anti-IDOR: USER_A intenta consultar perfil de USER_B por username -> ${EXPECTED_FOREIGN_RESOURCE_STATUS}" \
    "GET" "/api/users/by-username?username=${USER_B_USERNAME}" "${EXPECTED_FOREIGN_RESOURCE_STATUS}" "${USER_A_TOKEN}" ""

run_http_test "FR-E2E-002h" "ADMIN puede consultar el perfil de cualquier usuario (${USER_B_EMAIL})" \
    "GET" "/api/users/by-email?email=${USER_B_EMAIL}" "200" "${ADMIN_TOKEN}" ""

print_summary "FR-E2E-002 Flujo B - Registro, Login JWT y Perfil"
