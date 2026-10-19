-- =============================================================================
-- PulsePass E2E Fixtures Seed (Exclusivamente para ambiente de pruebas E2E)
-- Precondición:
--   1. Migraciones Flyway V1, V2, V3 y V4 aplicadas por la aplicación.
--   2. Cuentas base admin_e2e, laura_staff y miguel_inactivo registradas vía
--      00_setup_fixtures.sh (para que Spring Security genere sus hashes BCrypt
--      a partir de las variables locales de .env.e2e sin hardcodear secretos).
-- =============================================================================

-- 1. Venues de prueba (Activo con capacidad controlada de 3 e Inactivo)
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-SMR-01', 'Marina Convention Center', 'Santa Marta', 'Calle 22 # 1-10', 3, true)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    city = EXCLUDED.city,
    address = EXCLUDED.address,
    capacity = EXCLUDED.capacity,
    active = EXCLUDED.active;

INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-INACTIVE-01', 'Recinto Cerrado en Remodelación', 'Bogotá', 'Av 68 # 40', 100, false)
ON CONFLICT (code) DO UPDATE
SET active = false;

-- 2. Eventos iniciales de referencia (Borrador y Publicado)
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT 'EVT-SEED-DRAFT', 'Festival Borrador Oculto', 'No debe ser visible por público anónimo ni USER', 'MUSIC', 'DRAFT', TIMESTAMP '2027-06-15 20:00:00', 18, v.id
FROM venues v WHERE v.code = 'VEN-SMR-01'
ON CONFLICT (event_code) DO UPDATE
SET category = 'MUSIC',
    status = 'DRAFT',
    event_date = TIMESTAMP '2027-06-15 20:00:00',
    minimum_age = 18;

INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT 'CMF-2026', 'Caribbean Music Fest 2026', 'Festival principal publicado', 'MUSIC', 'PUBLISHED', TIMESTAMP '2027-08-20 20:00:00', 18, v.id
FROM venues v WHERE v.code = 'VEN-SMR-01'
ON CONFLICT (event_code) DO UPDATE
SET category = 'MUSIC',
    status = 'PUBLISHED',
    event_date = TIMESTAMP '2027-08-20 20:00:00',
    minimum_age = 18;

-- 3. Asociar Solar Beat a ambos eventos para probar filtrado de artista (público vs STAFF/ADMIN)
INSERT INTO event_artists (event_id, artist_id)
SELECT e.id, a.id FROM events e, artists a
WHERE e.event_code IN ('EVT-SEED-DRAFT', 'CMF-2026') AND a.stage_name = 'Solar Beat'
ON CONFLICT (event_id, artist_id) DO NOTHING;

-- 4. Promover roles de cuentas preprovisionadas (ADMIN, STAFF) e inactivar INACTIVE_USER
DELETE FROM user_roles
WHERE user_id IN (SELECT id FROM users WHERE username IN ('admin_e2e', 'laura_staff'));

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE username = 'admin_e2e'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'STAFF' FROM users WHERE username = 'laura_staff'
ON CONFLICT (user_id, role) DO NOTHING;

UPDATE users
SET active = false
WHERE username = 'miguel_inactivo';
