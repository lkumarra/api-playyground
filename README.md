# Best Buy API Playground (Spring Boot)

A RESTful API training tool powered by Spring Boot, providing realistic e-commerce data including **51,000+ products**, **1,500+ stores**, and **4,300+ categories**. Originally built with Node.js/Feathers, now migrated to Java 21 and Spring Boot 3.

## Table of Contents

- [Quick Start](#quick-start)
- [Running with Docker](#running-with-docker)
- [API Reference](#api-reference)
  - [Products](#products)
  - [Stores](#stores)
  - [Categories](#categories)
  - [Services](#services)
  - [Utilities](#utilities)
- [Query Parameters](#query-parameters)
- [Geolocation Search](#geolocation-search)
- [Configuration](#configuration)
- [Tech Stack](#tech-stack)

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+

### 1. Set up PostgreSQL

```bash
createdb api_playground
```

### 2. Run the application

```bash
mvn spring-boot:run
```

The API starts at **http://localhost:3030**. The database schema and seed data (51K+ products) are loaded automatically on first startup.

### 3. Explore the API

- Swagger UI: http://localhost:3030/docs
- OpenAPI spec: http://localhost:3030/swagger.json

---

## Running with Docker

The easiest way to get started — no Java or PostgreSQL installation needed.

```bash
docker compose up
```

This starts both PostgreSQL and the API. The API will be available at **http://localhost:3030** once the seed data finishes loading.

To run in the background:

```bash
docker compose up -d
```

To stop:

```bash
docker compose down
```

To stop and remove all data:

```bash
docker compose down -v
```

---

## API Reference

All endpoints return JSON. List endpoints return paginated results with this structure:

```json
{
  "total": 51957,
  "limit": 10,
  "skip": 0,
  "data": [...]
}
```

### Products

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/products` | List all products (paginated) |
| GET | `/products/{id}` | Get a single product with categories |
| POST | `/products` | Create a new product |
| PATCH | `/products/{id}` | Partially update a product |
| DELETE | `/products/{id}` | Delete a product |

**Example — Get products:**

```bash
curl http://localhost:3030/products
```

**Example — Filter by category:**

```bash
curl "http://localhost:3030/products?category.name=TVs"
curl "http://localhost:3030/products?category.id=abcat0101000"
```

**Example — Create a product:**

```bash
curl -X POST http://localhost:3030/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New Product",
    "type": "Electronics",
    "upc": "123456789",
    "description": "A great product",
    "model": "NP-100",
    "price": 99.99
  }'
```

**Product fields:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| name | string | yes | 1-100 chars |
| type | string | yes | 1-30 chars |
| price | decimal | no | >= 0.00 |
| upc | string | yes | 1-15 chars |
| shipping | decimal | no | >= 0.00 |
| description | string | yes | 1-100 chars |
| manufacturer | string | no | 1-50 chars |
| model | string | yes | 1-25 chars |
| url | string | no | 1-500 chars |
| image | string | no | 1-500 chars |

---

### Stores

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/stores` | List all stores (paginated) |
| GET | `/stores/{id}` | Get a single store with services |
| POST | `/stores` | Create a new store |
| PATCH | `/stores/{id}` | Partially update a store |
| DELETE | `/stores/{id}` | Delete a store |

**Example — Get stores near a zip code:**

```bash
curl "http://localhost:3030/stores?near=55401&miles=15"
```

**Example — Filter by service:**

```bash
curl "http://localhost:3030/stores?service.name=Geek Squad Services"
curl "http://localhost:3030/stores?service.id=4"
```

**Store fields:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| name | string | yes | 1-100 chars |
| type | string | no | 1-30 chars |
| address | string | yes | 1-50 chars |
| address2 | string | no | max 30 chars |
| city | string | yes | 1-50 chars |
| state | string | yes | 1-30 chars |
| zip | string | yes | 1-30 chars |
| lat | decimal | no | |
| lng | decimal | no | |
| hours | string | no | 1-100 chars |

---

### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/categories` | List all categories (paginated) |
| GET | `/categories/{id}` | Get a single category with subcategories |
| POST | `/categories` | Create a new category |
| PATCH | `/categories/{id}` | Partially update a category |
| DELETE | `/categories/{id}` | Delete a category |

**Example — Get a category with its hierarchy:**

```bash
curl http://localhost:3030/categories/abcat0010000
```

Response includes `subCategories` and `categoryPath` arrays.

**Category fields:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| id | string | yes | 1-100 chars |
| name | string | yes | 1-100 chars |

---

### Services

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/services` | List all in-store services (paginated) |
| GET | `/services/{id}` | Get a single service |
| POST | `/services` | Create a new service |
| PATCH | `/services/{id}` | Partially update a service |
| DELETE | `/services/{id}` | Delete a service |

**Service fields:**

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| name | string | yes | 1-100 chars |

---

### Utilities

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/version` | Returns API version |
| GET | `/healthcheck` | Returns uptime, readonly status, document counts |

**Example:**

```bash
curl http://localhost:3030/healthcheck
```

```json
{
  "uptime": 123.456,
  "readonly": false,
  "documents": {
    "products": 51957,
    "stores": 1561,
    "categories": 4307
  }
}
```

---

## Query Parameters

All list endpoints (`GET /products`, `GET /stores`, etc.) support these query parameters:

### Pagination

| Parameter | Default | Max | Description |
|-----------|---------|-----|-------------|
| `$limit` | 10 | 25 | Number of results per page |
| `$skip` | 0 | — | Number of results to skip |

```bash
# Get 5 products, skip the first 10
curl "http://localhost:3030/products?\$limit=5&\$skip=10"
```

### Filtering by related entities

**Products** can be filtered by category:

```bash
# Both syntaxes work:
curl "http://localhost:3030/products?category.id=abcat0208002"
curl "http://localhost:3030/products?category[id]=abcat0208002"
curl "http://localhost:3030/products?category.name=Coffee Pods"
curl "http://localhost:3030/products?category[name]=Coffee Pods"
```

**Stores** can be filtered by service:

```bash
curl "http://localhost:3030/stores?service.id=4"
curl "http://localhost:3030/stores?service.name=Geek Squad Services"
```

---

## Geolocation Search

Find stores near a US zip code:

```bash
# Stores within 10 miles (default) of Minneapolis
curl "http://localhost:3030/stores?near=55401"

# Stores within 25 miles
curl "http://localhost:3030/stores?near=55401&miles=25"
```

The API uses spherical trigonometry to calculate a bounding box around the zip code's coordinates, then returns all stores within that area.

---

## Configuration

Configuration is managed via `application.properties` or environment variables:

| Property | Env Variable | Default | Description |
|----------|-------------|---------|-------------|
| `server.port` | `SERVER_PORT` | 3030 | Server port |
| `app.version` | `APP_VERSION` | 1.1.0 | API version |
| `app.readonly` | `APP_READONLY` | false | Disable write operations |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | jdbc:postgresql://localhost:5432/api_playground | Database URL |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | postgres | Database user |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | postgres | Database password |

### Read-only Mode

Set `app.readonly=true` to disable all POST, PATCH, PUT, and DELETE operations. The API will return `405 Method Not Allowed` for any write attempts.

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Build Tool | Maven |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL 15+ |
| API Docs | SpringDoc OpenAPI 3 |
| Validation | Jakarta Bean Validation |
| Containerization | Docker / Docker Compose |

---

## Project Structure

```
src/main/java/com/bestbuy/api/
├── ApiPlaygroundApplication.java    # Entry point
├── config/                          # CORS, read-only interceptor, OpenAPI
├── controller/                      # REST controllers
├── dto/                             # Request/response DTOs
├── entity/                          # JPA entities
├── exception/                       # Global error handling
├── repository/                      # Spring Data JPA repositories
└── service/                         # Business logic & geolocation
```

---

## License

- **Source code**: MIT
- **Dataset**: [Creative Commons Attribution-NonCommercial 4.0](https://creativecommons.org/licenses/by-nc/4.0/)
