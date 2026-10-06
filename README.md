# PRD_PulsePass - V-0.0.1

PulsePass es un caso de estudio académico para el diseño e implementación de la capa de persistencia y servicios de una plataforma de eventos y entradas.

## ¿Qué se espera?
Demostrar la capacidad de transformar requisitos de negocio en un modelo relacional robusto y coordinar la lógica transaccional a través de servicios, utilizando Spring Data JPA, MapStruct, JUnit 5 con Mockito, y Flyway junto a Testcontainers.

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

## Estructura de la Capa de Servicios
1. **DTOs (Data Transfer Objects):** Uso de `records` de Java para establecer el contrato público y evitar la exposición de entidades persistentes.
2. **Mapeos (MapStruct):** Uso de `MapStruct` para conversiones automatizadas y consistentes entre `Entities` y `DTOs`.
3. **Lógica de Negocio:** Interfaces de servicio (ej. `EventService`, `TicketService`) e implementaciones (`@Service`) inyectadas por constructor. Centralizan operaciones atómicas con manejo de transacciones (`@Transactional`), control de reglas del negocio (aforos, estados, edades) y delegación a los repositorios.
4. **Manejo de Errores:** Excepciones de dominio personalizadas y claras (`ResourceNotFoundException`, `BusinessRuleException`, `DuplicateResourceException`).

## Cómo Usar y Ejecutar Pruebas

Para ejecutar las pruebas se requiere tener Docker funcionando en el equipo (utiliza Testcontainers).

### Windows / Linux
Abre una terminal en la raíz del proyecto y ejecuta:

```bash
./mvnw clean test
```

### Importante

# Variables de entorno

copiar del ejemplo en .env.example a un archivo .env en base a la maquina donde se clone el repositorio

### Linux

```bash
 touch .env \ 
 cat .env.example | tee .env
```
