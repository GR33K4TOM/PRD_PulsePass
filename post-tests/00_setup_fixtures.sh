#!/usr/bin/env bash
# =============================================================================
# 00_setup_fixtures.sh
# Prepara las cuentas semilla (admin_e2e, laura_staff, miguel_inactivo) vía API
# para que Spring Security genere sus hashes BCrypt reales, y aplica el script
# post-tests/fixtures/seed-e2e.sql sobre PostgreSQL.
# =============================================================================

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/common_e2e.sh"

echo "================================================================================"
echo "PREPARACIÓN DE DATOS SEMILLA E2E (00_setup_fixtures.sh)"
echo "================================================================================"

ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
STAFF_PASSWORD="${STAFF_PASSWORD:-}"
INACTIVE_PASSWORD="${INACTIVE_PASSWORD:-}"

if [ -z "${ADMIN_PASSWORD}" ] || [ "${ADMIN_PASSWORD}" = "REEMPLAZAR_PASSWORD_LOCAL_12_CHARS" ]; then
    echo "[ERROR] Define contraseñas válidas (>=12 caracteres) en post-tests/.env.e2e antes de continuar."
    exit 1
fi

echo "[1/2] Registrando cuentas base (admin_e2e, laura_staff, miguel_inactivo) en ${BASE_URL}/api/users..."

register_seed_user() {
    local username="$1"
    local email="$2"
    local first_name="$3"
    local last_name="$4"
    local birth_date="$5"
    local password="$6"

    local payload
    payload=$(printf '{"username":"%s","email":"%s","firstName":"%s","lastName":"%s","phone":"3001112233","city":"Santa Marta","birthDate":"%s","password":"%s"}' \
        "$username" "$email" "$first_name" "$last_name" "$birth_date" "$password")

    local status
    status=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/api/users" \
        -H "Content-Type: application/json" -d "${payload}" | tr -d '\r')

    if [ "${status}" = "201" ]; then
        echo "  -> Usuario '${username}' creado exitosamente (HTTP 201)."
    elif [ "${status}" = "409" ]; then
        echo "  -> Usuario '${username}' ya existía en BD (HTTP 409 - Idempotente)."
    else
        echo "  -> [WARN] Respuesta inesperada al registrar '${username}': HTTP ${status}"
    fi
}

register_seed_user "admin_e2e" "admin@example.test" "Admin" "Operaciones" "1990-01-10" "${ADMIN_PASSWORD}"
register_seed_user "laura_staff" "laura.staff@example.test" "Laura" "Staff" "1995-03-15" "${STAFF_PASSWORD}"
register_seed_user "miguel_inactivo" "miguel.inactivo@example.test" "Miguel" "Inactivo" "1992-07-20" "${INACTIVE_PASSWORD}"

echo "[2/2] Aplicando post-tests/fixtures/seed-e2e.sql en PostgreSQL..."

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-pulsepass}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-}"
DOCKER_PG_CONTAINER="${DOCKER_PG_CONTAINER:-}"
SEED_SQL="${SCRIPT_DIR}/fixtures/seed-e2e.sql"

if [ -n "${DOCKER_PG_CONTAINER}" ]; then
    docker exec -i -e PGPASSWORD="${DB_PASSWORD}" "${DOCKER_PG_CONTAINER}" \
        psql -U "${DB_USER}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 < "${SEED_SQL}"
elif command -v psql >/dev/null 2>&1; then
    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
        -v ON_ERROR_STOP=1 -f "${SEED_SQL}"
else
    echo "[WARN] No se encontró el comando 'psql' ni se definió DOCKER_PG_CONTAINER."
    echo "       Ejecuta manualmente el contenido de ${SEED_SQL} en tu instancia PostgreSQL."
    exit 1
fi

echo "[OK] Fixtures E2E configurados correctamente."
