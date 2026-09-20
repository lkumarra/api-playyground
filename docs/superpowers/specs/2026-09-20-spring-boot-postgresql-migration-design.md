# Spring Boot and PostgreSQL Migration Design

## Purpose

Create a standalone Java Spring Boot replacement for the legacy Node/Feathers API Playground. The new project lives at `/Users/lkumarrajput/Developer/api-playground-spring-boot`; the existing Node project remains unchanged. The replacement must run with a PostgreSQL container through Docker Compose, preserve the public REST API contract, and seed the bundled legacy SQLite dataset automatically on each application-container start when the target database has not already been seeded.

## Scope

The replacement exposes the following legacy routes at the root path:

* `GET`, `POST` `/products`, `/categories`, `/stores`, and `/services`
* `GET`, `PATCH`, `DELETE` `/products/{id}`, `/categories/{id}`, `/stores/{id}`, and `/services/{id}`
* `GET /version` and `GET /healthcheck`

The Spring application preserves legacy collection responses:

```json
{
  "total": 51957,
  "limit": 10,
  "skip": 0,
  "data": []
}
```

It accepts the Feathers-compatible query syntax used by the current API:

* pagination: `$limit`, `$skip`
* sorting: `$sort[field]=1` or `$sort[field]=-1`
* projection: `$select[]=field`
* equality and comparisons: direct field equality and `$eq`, `$lt`, `$lte`, `$gt`, `$gte`, `$in`, `$nin`
* wildcard comparisons: `$like` and `$notLike`, converting `*` to SQL `%`
* product category filters: `category.name`, `category[name]`, `category.id`, `category[id]`
* store service filters: `service.name`, `service[name]`, `service.id`, `service[id]`
* nearby-store lookup: `near=<ZIP>` plus optional `miles=<radius>`

The project also serves the existing public and Markdown documentation assets and exposes OpenAPI/Swagger documentation at the existing documentation-facing routes where Springdoc supports them. The API version remains `1.1.0` unless deliberately changed in the new project metadata.

## Application architecture

The project uses Maven, Java 21, Spring Boot, Spring Web MVC, Spring Data JPA, Bean Validation, Flyway, PostgreSQL JDBC, SQLite JDBC, and Springdoc OpenAPI.

Package responsibilities are:

* `api`: REST controllers, request and response DTOs, and legacy query-parameter binding.
* `domain`: JPA entities and enums/value types where appropriate.
* `persistence`: repositories and Criteria API specifications.
* `service`: CRUD orchestration, legacy-query translation, collection pagination/projection, proximity calculations, and health reporting.
* `seed`: source-data reader, batched PostgreSQL writer, seed-state verification, and startup command support.
* `config`: CORS, static assets, OpenAPI route configuration, read-only mode, and application properties.
* `error`: one global exception handler that produces legacy-compatible JSON error fields (`name`, `message`, and `code`).

Controllers use DTOs rather than serializing JPA entities. The shared query translator validates fields and operators before building Criteria API predicates, applies only supported projection fields, and rejects malformed legacy parameters with a 400 response. Resource services enforce the `APP_READONLY` setting for all mutating methods with a 405 response. Missing records result in a 404 response.

`/healthcheck` reports process uptime, the read-only flag, and the product, store, and category counts. It returns a 500 response if the database cannot be read.

## PostgreSQL data model

Flyway owns the schema. It creates these main tables:

* `products`: legacy numeric primary key, catalog fields, price/shipping decimals, and legacy timestamps.
* `categories`: legacy string primary key, name, and legacy timestamps.
* `stores`: legacy numeric primary key, address/location fields, and legacy timestamps.
* `services`: legacy numeric primary key, name, and legacy timestamps.
* `zipcodes`: ZIP-code string primary key, longitude, latitude, city, state, and legacy timestamps.

It creates relationship tables with composite primary keys and foreign keys:

* `product_categories` for products to categories.
* `store_services` for stores to services.
* `sub_categories` for directed category-to-subcategory links.
* `category_paths` for directed category path links.

Indexes support the documented filter paths: product name/type/price, category name, store state/name/location, service name, ZIP lookup, and both columns of all relationship tables. PostgreSQL sequences are reset to the maximum imported primary key after a successful seed.

## SQLite-to-PostgreSQL seed process

The current `dataset.sqlite` is copied into the new project as a read-only seed resource. The deployment does not require access to the legacy Node repository after the Docker image has been built.

`seed_history` records one successful source-dataset seed, including a fixed dataset identifier, source checksum, completion timestamp, and expected row counts. The application ships a `bin/seed-database.sh` startup script that starts Spring Boot in a seed-only mode. The script runs on every application-container start; the Java seeder decides whether work is required.

The seed-only command executes this sequence:

1. Flyway applies schema migrations.
2. The seeder checks for a matching successful `seed_history` marker and validates the expected table counts. If both match, it exits successfully without changing data.
3. If no marker exists, the seeder requires all target data tables to be empty. This prevents accidental merging into an unrelated PostgreSQL database.
4. SQLite is opened read-only. The data is written in one transaction using bounded JDBC batches in dependency order: categories, products, stores, services, ZIP codes, then the relationship tables.
5. The seeder resets sequences, verifies every imported table against source counts, verifies relationship foreign keys, and writes `seed_history` only after validation succeeds.
6. Any failure rolls the transaction back and returns a non-zero exit status; the web server does not start.

The source snapshot has these baseline counts, which the seeder validates: 51,957 products, 4,307 categories, 1,561 stores, 40,934 ZIP codes, 21 services, 179,145 product-category rows, 4,511 subcategory rows, 14,263 category-path rows, and 10,613 store-service rows.

The supported reseed procedure is explicit and destructive only to the Docker development volume: `docker compose down -v`, followed by `docker compose up --build`. The normal startup script never deletes or overwrites an existing seeded database.

## Docker deployment

`docker-compose.yml` defines two services:

* `postgres`: an official PostgreSQL image, a named persistent volume, environment-based database credentials, and a `pg_isready` health check.
* `app`: a multi-stage-built Spring Boot image. Compose waits for the PostgreSQL health check, runs `bin/entrypoint.sh`, which invokes `bin/seed-database.sh`, then launches the web application on port 3030.

The Docker image includes the compiled executable jar, the source SQLite seed resource, public and Markdown assets, and shell scripts. It runs as a non-root user. Database settings are supplied through `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`; source control contains only `.env.example`, never a real password. PostgreSQL data persists in the named volume, so restart behavior is idempotent.

## Testing and verification

Production behavior is developed test-first. The test suite uses Testcontainers PostgreSQL for repository and endpoint integration tests; no in-memory database substitutes PostgreSQL behavior.

Tests cover:

* all resource CRUD endpoints, IDs, validation, 404s, and read-only 405s;
* pagination, projection, sorting, comparison, wildcard, category/service relationship, and nearby-store queries;
* utility endpoint response contracts and database-failure behavior;
* schema migrations, foreign-key constraints, and sequence values;
* seed idempotency, failed-seed rollback, source-count validation, and relationship preservation using a compact SQLite fixture;
* Docker Compose startup against the full bundled dataset, including a health endpoint request and expected seeded counts.

`mvn verify` is the normal verification command. The README documents prerequisites, local execution, Docker deployment, configuration, test execution, seed behavior, and the destructive development-only reseed command.

## Acceptance criteria

* A clean checkout of the new project starts with `docker compose up --build` and serves the API on port 3030.
* PostgreSQL is the only runtime database; SQLite is used only as the read-only seed source.
* A fresh named volume receives all source data and relationships with original primary keys and timestamps.
* Restarting the application or Compose stack preserves data and skips a verified seed.
* Every legacy API route and documented query behavior listed in this specification works against PostgreSQL.
* The full Maven test suite and Docker Compose smoke test pass.
* The legacy Node repository, including its untracked `package-lock.json`, is not changed.
