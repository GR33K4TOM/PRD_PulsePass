# PRD_PulsePass - V-1.0.0

PulsePass es un caso de estudio académico para el diseño e implementación de la capa de persistencia de una plataforma de eventos y entradas.

## ¿Qué se espera?
Demostrar la capacidad de transformar requisitos de negocio en un modelo relacional robusto, utilizando Flyway para el versionado de base de datos, Spring Data JPA para el acceso a datos y Testcontainers para pruebas de integración reales.

## Sección Técnica
- **Lenguaje:** Java 21
- **Framework:** Spring Boot 3/4
- **ORM:** Spring Data JPA / Hibernate
- **Base de Datos:** PostgreSQL
- **Migraciones:** Flyway
- **Pruebas:** Testcontainers (PostgreSQL)

## Estructura de la Capa de Persistencia
1. **Modelo:** Entidades JPA (`Venue`, `Event`, `Artist`, `User`, `UserProfile`, `Ticket`) mapeadas contra una base de datos segura y estricta. Hibernate está configurado para solo validar el esquema (`ddl-auto=validate`).
2. **Migraciones:** Versionado de la base de datos en `src/main/resources/db/migration/`:
   - `V1`: Esquema base y reglas de negocio (Constraints).
   - `V2`: Carga de datos inicial de artistas.
   - `V3`: Añadida columna de URL de streaming.
3. **Consultas:** Uso de *Query Methods* y *JPQL* en los repositorios para resolver búsquedas, filtros y reportes solicitados por negocio.

## Cómo Usar y Ejecutar Pruebas

Para ejecutar las pruebas se requiere tener Docker funcionando en el equipo (utiliza Testcontainers).

### Windows / Linux
Abre una terminal en la raíz del proyecto y ejecuta:

```bash
./mvnw clean test
```
