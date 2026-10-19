#!/usr/bin/env bash
# =============================================================================
# Funciones auxiliares agnósticas de SO (Linux / macOS / Windows Git Bash / WSL)
# =============================================================================

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env.e2e"
TOKENS_FILE="${SCRIPT_DIR}/.tokens.env"

# Carga un archivo .env de forma segura (soporta espacios sin comillas y CRLF de Windows)
load_env_file() {
    local file="$1"
    [ -f "${file}" ] || return 0
    while IFS= read -r line || [ -n "${line}" ]; do
        line=$(printf '%s' "${line}" | tr -d '\r')
        case "${line}" in
            ""|\#*) continue ;;
        esac
        local key="${line%%=*}"
        local val="${line#*=}"
        # Remover comillas simples o dobles envolventes si existen
        val="${val%\"}"
        val="${val#\"}"
        val="${val%\'}"
        val="${val#\'}"
        export "${key}=${val}"
    done < "${file}"
}

if [ -f "${ENV_FILE}" ]; then
    load_env_file "${ENV_FILE}"
else
    echo "[WARN] No se encontró ${ENV_FILE}. Usando valores por defecto de demostración local."
fi

if [ -f "${TOKENS_FILE}" ]; then
    load_env_file "${TOKENS_FILE}"
fi

# Valores por defecto seguros en caso de no estar definidos
BASE_URL="${BASE_URL:-http://localhost:8080}"
RUN_ID="${RUN_ID:-run01}"
VENUE_ACTIVE_CODE="${VENUE_ACTIVE_CODE:-VEN-SMR-01}"
VENUE_INACTIVE_CODE="${VENUE_INACTIVE_CODE:-VEN-INACTIVE-01}"
ARTIST_ID="${ARTIST_ID:-1}"
ARTIST_STAGE_NAME="${ARTIST_STAGE_NAME:-Solar Beat}"
DRAFT_EVENT_CODE="${DRAFT_EVENT_CODE:-EVT-SEED-DRAFT}"
PUBLISHED_EVENT_CODE="${PUBLISHED_EVENT_CODE:-CMF-2026}"
EVENT_TO_PUBLISH_CODE="${EVENT_TO_PUBLISH_CODE:-EVT-E2E-${RUN_ID}}"
EXPECTED_TOKEN_TTL_SECONDS="${EXPECTED_TOKEN_TTL_SECONDS:-900}"
EXPECTED_FOREIGN_RESOURCE_STATUS="${EXPECTED_FOREIGN_RESOURCE_STATUS:-403}"

TOTAL_PASS=0
TOTAL_FAIL=0

# Extrae el valor de un campo JSON simple (string o número) sin depender de jq
extract_json_field() {
    local json="$1"
    local field="$2"
    local val
    val=$(printf '%s' "$json" | sed -n 's/.*"'"${field}"'"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)
    if [ -z "$val" ]; then
        val=$(printf '%s' "$json" | sed -n 's/.*"'"${field}"'"[[:space:]]*:[[:space:]]*\([0-9a-zA-Z.-]*\).*/\1/p' | head -n 1)
    fi
    printf '%s' "$val"
}

# Guarda o actualiza una variable de estado (token o ticketCode) en .tokens.env
save_state_var() {
    local key="$1"
    local value="$2"
    touch "${TOKENS_FILE}"
    if grep -q "^${key}=" "${TOKENS_FILE}" 2>/dev/null; then
        grep -v "^${key}=" "${TOKENS_FILE}" > "${TOKENS_FILE}.tmp" && mv "${TOKENS_FILE}.tmp" "${TOKENS_FILE}"
    fi
    printf '%s=%s\n' "${key}" "${value}" >> "${TOKENS_FILE}"
    export "${key}=${value}"
}

# Ejecuta una petición HTTP con cURL y muestra el reporte formateado
# Uso: run_http_test "TC-ID" "Descripción" "METHOD" "/api/path" "EXPECTED_STATUS" "TOKEN_O_VACIO" 'JSON_BODY_O_VACIO'
run_http_test() {
    local tc_id="$1"
    local description="$2"
    local method="$3"
    local path="$4"
    local expected_status="$5"
    local token="${6:-}"
    local body="${7:-}"

    local url="${BASE_URL}${path}"
    local curl_args=(-s -X "${method}" "${url}" -H "Accept: application/json" -w "\n%{http_code}")

    if [ -n "${token}" ]; then
        curl_args+=(-H "Authorization: Bearer ${token}")
    fi

    if [ -n "${body}" ]; then
        curl_args+=(-H "Content-Type: application/json" -d "${body}")
    fi

    local raw_output
    raw_output=$(curl "${curl_args[@]}")
    local http_status
    http_status=$(printf '%s\n' "${raw_output}" | tail -n 1 | tr -d '\r')
    local response_body
    response_body=$(printf '%s\n' "${raw_output}" | sed '$d')

    LAST_HTTP_STATUS="${http_status}"
    LAST_RESPONSE_BODY="${response_body}"

    echo "--------------------------------------------------------------------------------"
    echo "[${tc_id}] ${description}"
    echo "  Petición : ${method} ${url}"
    if [ "${http_status}" = "${expected_status}" ]; then
        echo "  Estado   : HTTP ${http_status} (Esperado: ${expected_status}) -> [PASS]"
        TOTAL_PASS=$((TOTAL_PASS + 1))
    else
        echo "  Estado   : HTTP ${http_status} (Esperado: ${expected_status}) -> [FAIL]"
        TOTAL_FAIL=$((TOTAL_FAIL + 1))
    fi
    # Ocultar accessToken real en consola por política NFR-E2E-002 (Confidencialidad)
    local safe_body
    safe_body=$(printf '%s' "${response_body}" | sed 's/"accessToken":"[^"]*"/"accessToken":"<JWT_OCULTO_POR_SEGURIDAD>"/g')
    echo "  Respuesta: ${safe_body}"
}

print_summary() {
    local suite_name="$1"
    echo "================================================================================"
    echo "RESUMEN [${suite_name}]: PASS=${TOTAL_PASS} | FAIL=${TOTAL_FAIL}"
    echo "================================================================================"
    if [ "${TOTAL_FAIL}" -gt 0 ]; then
        return 1
    fi
    return 0
}
