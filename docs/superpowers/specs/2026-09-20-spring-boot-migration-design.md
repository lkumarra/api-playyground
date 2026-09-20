# Best Buy API Playground — Spring Boot Migration Design

**Date:** 2026-09-20
**Source:** Node.js/Feathers API Playground
**Target:** Java 21 + Spring Boot 3.x + PostgreSQL + Maven

---

## Overview

Migrate the Best Buy API Playground from Node.js (Feathers/Express/Sequelize/SQLite) to Java Spring Boot (Spring Data JPA/Hibernate/PostgreSQL). Preserve all REST endpoints, query features, and the "just run it" developer experience.

## Architecture

Standard Spring Boot layered architecture:

```
Controller → Service → Repository (JPA) → PostgreSQL
```

- **Controllers**: REST endpoints matching original routes
- **Services**: Business logic (geolocation, filtering, pagination)
- **Repositories**: Spring Data JPA with custom queries
- **Entities**: JPA entities with Hibernate-managed relationships

## Entities

### Product
- Fields: id (auto), name, type, price, upc, shipping, description, manufacturer, model, url, image, createdAt, updatedAt
- Relationships: ManyToMany → Category (join table: `product_category`)

### Store
- Fields: id (auto), name, type, address, address2, city, state, zip, lat, lng, hours, createdAt, updatedAt
- Relationships: ManyToMany → Service (join table: `store_services`)

### Category
- Fields: id (String PK), name, createdAt, updatedAt
- Relationships:
  - ManyToMany self-referential: subcategories
  - ManyToMany self-referential: categoryPath
  - ManyToMany → Product (inverse side)

### Service (InStoreService)
- Fields: id (auto), name, createdAt, updatedAt
- Relationships: ManyToMany → Store (inverse side)

### Zipcode
- Fields: zip (String PK), lat, lng, city, state
- Purpose: Geolocation lookup for store proximity search

## REST Endpoints

### Products (`/products`)
- `GET /products` — paginated list, filter by fields, category.id, category.name, $like, $gt/$lt etc.
- `GET /products/{id}` — single product with categories
- `POST /products` — create (validated)
- `PATCH /products/{id}` — partial update
- `DELETE /products/{id}` — delete

### Stores (`/stores`)
- `GET /stores` — paginated list, filter by fields, service.id, service.name
- `GET /stores?near={zip}&miles={N}` — geolocation search (bounding box)
- `GET /stores/{id}` — single store with services
- `POST /stores` — create (validated)
- `PATCH /stores/{id}` — partial update
- `DELETE /stores/{id}` — delete

### Categories (`/categories`)
- `GET /categories` — paginated list with subcategories/path
- `GET /categories/{id}` — single category with subcategories/path
- `POST /categories` — create (validated)
- `PATCH /categories/{id}` — partial update
- `DELETE /categories/{id}` — delete

### Services (`/services`)
- `GET /services` — paginated list
- `GET /services/{id}` — single service
- `POST /services` — create (validated)
- `PATCH /services/{id}` — partial update
- `DELETE /services/{id}` — delete

### Utilities
- `GET /version` — returns `{ version: "1.1.0" }`
- `GET /healthcheck` — uptime, readonly status, document counts

## Query Features

- **Pagination**: `$limit` (default 10, max 25) and `$skip` mapped to Spring Pageable
- **Filtering**: field-level filters on all entity fields
- **Operators**: `$like`/`$notLike` (wildcard `*` → `%`), `$gt`, `$gte`, `$lt`, `$lte`, `$ne`
- **Field selection**: `$select` for column projection
- **Sorting**: `$sort` parameter
- **Geolocation**: bounding-box search using spherical geometry (Earth radius 6,371,000m)

## Cross-cutting Concerns

- **Validation**: Jakarta Bean Validation on request DTOs
- **Read-only mode**: `app.readonly` property, enforced via interceptor on mutating endpoints
- **Error handling**: `@RestControllerAdvice` returning standardized error JSON
- **API docs**: SpringDoc OpenAPI 3 at `/swagger-ui.html`
- **CORS**: globally enabled
- **Logging**: SLF4J/Logback
- **Timestamps**: `createdAt`/`updatedAt` auto-managed by JPA

## Data Seeding

- `schema.sql` for table creation
- `data.sql` for seed data (exported from existing SQLite dataset)
- Auto-executed on startup via Spring Boot

## Not Migrated

- Socket.IO/WebSocket support (unused by consumers)
- Markdown page rendering (replaced by SpringDoc)
- Feathers-specific internals (adapted to Spring conventions)

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI 3 |
| Logging | SLF4J / Logback |
