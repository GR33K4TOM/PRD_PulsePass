#!/usr/bin/env bash
# =============================================================================
# run_all_e2e.sh
# Ejecuta secuencialmente los 5 flujos E2E (FR-E2E-001 a FR-E2E-005)
# Agnóstico de sistema operativo (Linux, macOS, Windows Git Bash / WSL)
# =============================================================================

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Limpiar tokens de corridas previas antes de iniciar la suite completa
rm -f "${SCRIPT_DIR}/.tokens.env"

SCRIPTS=(
    "FR-E2E-001_flujo_a_descubrimiento_publico.sh"
    "FR-E2E-002_flujo_b_registro_y_jwt.sh"
    "FR-E2E-003_flujo_c_catalogo_admin.sh"
    "FR-E2E-004_flujo_d_compra_y_tickets.sh"
    "FR-E2E-005_flujo_e_seguridad_y_negativos.sh"
)

SUITES_FAILED=0

for script in "${SCRIPTS[@]}"; do
    if ! bash "${SCRIPT_DIR}/${script}"; then
        SUITES_FAILED=$((SUITES_FAILED + 1))
    fi
    echo ""
done

echo "================================================================================"
if [ "${SUITES_FAILED}" -eq 0 ]; then
    echo "RESULTADO GLOBAL E2E: TODAS LAS SUITES (FR-E2E-001 a FR-E2E-005) PASARON [OK]"
    echo "================================================================================"
    exit 0
else
    echo "RESULTADO GLOBAL E2E: ${SUITES_FAILED} SUITE(S) PRESENTARON FALLOS [ERROR]"
    echo "================================================================================"
    exit 1
fi
