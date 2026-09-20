# Spring Boot PostgreSQL Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a standalone, Docker-deployable Java Spring Boot API Playground that preserves the legacy REST contract and automatically copies the bundled SQLite dataset into PostgreSQL on first startup.

**Architecture:** A Spring Web MVC application exposes resource-specific controllers backed by JPA services and a common, safe translator for the legacy Feathers query syntax. Flyway owns the PostgreSQL schema; an explicit seed-only startup command reads the bundled SQLite snapshot through JDBC, inserts records in dependency order, and records a validated completion marker before the web process starts.

**Tech Stack:** Java 21, Maven, Spring Boot, Spring Web MVC, Spring Data JPA, Bean Validation, Flyway, PostgreSQL JDBC, SQLite JDBC, Springdoc OpenAPI, JUnit 5, AssertJ, Testcontainers PostgreSQL, Docker Compose.

**Spec:** `docs/superpowers/specs/2026-09-20-spring-boot-postgresql-migration-design.md`

## Global Constraints

* Implement only in `/Users/lkumarrajput/Developer/api-playground-spring-boot`; do not modify the legacy Node repository.
* Use Java 21 and Maven; PostgreSQL is the only runtime database.
* Keep the API on port 3030 and retain root-level legacy paths and Feathers-style response/error formats.
* Support `GET`, `POST`, `PUT`, `PATCH`, and `DELETE` resource APIs exactly as specified; `$select[]` suppresses relationship embedding.
* Preserve imported primary keys and timestamps, and never reseed or overwrite a verified, populated database.
* Use Testcontainers PostgreSQL for integration tests; do not use H2.
* Write each production behavior test first, observe its expected failure, then implement the smallest change to pass it.
* Commit each task only after its focused tests and `mvn verify` pass.

---

## Planned file structure

```text
pom.xml
.gitignore
.env.example
Dockerfile
docker-compose.yml
bin/entrypoint.sh
bin/seed-database.sh
scripts/verify-compose.sh
src/main/java/com/bestbuy/apiplayground/
  ApiPlaygroundApplication.java
  config/{ApiProperties,WebConfiguration,OpenApiConfiguration}.java
  domain/{Product,Category,Store,StoreService,ServiceOffering,Zipcode}.java
  persistence/{ProductRepository,CategoryRepository,StoreRepository,ServiceOfferingRepository,ZipcodeRepository}.java
  query/{LegacyQuery,LegacyQueryParser,LegacySpecificationFactory,LegacySort}.java
  api/{ProductController,CategoryController,StoreController,ServiceController,UtilityController,
       PageResponse,LegacyResponseProjector,write request records}.java
  service/{ProductService,CategoryService,StoreService,ServiceOfferingService,ReadOnlyGuard,HealthService}.java
  seed/{SeedProperties,SeedCoordinator,SqliteDatasetReader,PostgresDatasetWriter,SeedCommandRunner}.java
  error/{ApiError,ApiExceptionHandler,NotFoundException,BadRequestException}.java
src/main/resources/
  application.yml
  db/migration/V1__create_schema.sql
  static/                          # copied public assets
  pages/{index.md,queries.md}      # copied Markdown sources
  seed/dataset.sqlite              # copied 58 MB legacy source snapshot
src/test/java/com/bestbuy/apiplayground/
  support/PostgresIntegrationTest.java
  query/LegacyQueryParserTest.java
  api/{ProductApiIT,CategoryApiIT,StoreApiIT,ServiceApiIT,UtilityApiIT,ApiExceptionHandlerTest}.java
  persistence/SchemaMigrationIT.java
  seed/SeedCoordinatorIT.java
src/test/resources/seed/fixture.sqlite
README.md
```

### Task 1: Establish the Maven Spring Boot baseline

**Files:**
- Create: `pom.xml`, `.gitignore`, `src/main/java/com/bestbuy/apiplayground/ApiPlaygroundApplication.java`
- Create: `src/main/java/com/bestbuy/apiplayground/config/BuildInfo.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/bestbuy/apiplayground/config/BuildInfoTest.java`

**Interfaces:**
- Produces: `BuildInfo.VERSION`, fixed at `"1.1.0"`; `ApiPlaygroundApplication` is the package root for every later Spring component.
- Produces: test dependencies and `mvn verify` baseline for all later tasks.

- [ ] **Step 1: Create the Maven test harness and runtime configuration**

Create `pom.xml` with Spring Boot parent/dependency management, Java release 21, and these dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `flyway-core`, `flyway-database-postgresql`, `postgresql`, `sqlite-jdbc`, `springdoc-openapi-starter-webmvc-ui`, `org.commonmark:commonmark`, `spring-boot-starter-test`, `org.testcontainers:junit-jupiter`, and `org.testcontainers:postgresql`. Configure Surefire and Failsafe so classes ending `IT` run during `verify`.

Create `application.yml` with these exact defaults:

```yaml
server:
  port: ${SERVER_PORT:3030}
spring:
  mvc:
    throw-exception-if-no-handler-found: true
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/api_playground}
    username: ${SPRING_DATASOURCE_USERNAME:api_playground}
    password: ${SPRING_DATASOURCE_PASSWORD:api_playground}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
app:
  read-only: ${APP_READONLY:false}
  seed:
    source: ${APP_SEED_SOURCE:classpath:seed/dataset.sqlite}
    mode: ${APP_SEED_MODE:disabled}
```

Add `.gitignore` entries for `target/`, `.idea/`, `.vscode/`, `.env`, and Docker-generated test logs.

- [ ] **Step 2: Write the failing version contract test**

```java
class BuildInfoTest {
  @Test
  void exposes_the_legacy_api_version() {
    assertThat(BuildInfo.VERSION).isEqualTo("1.1.0");
  }
}
```

- [ ] **Step 3: Run the focused test and verify it fails because `BuildInfo` is absent**

Run: `mvn -Dtest=BuildInfoTest test`

Expected: compilation failure identifying the missing `BuildInfo` type or `VERSION` member.

- [ ] **Step 4: Add the minimum application bootstrap and version constant**

```java
@SpringBootApplication
public class ApiPlaygroundApplication {
  public static void main(String[] args) {
    SpringApplication.run(ApiPlaygroundApplication.class, args);
  }
}

public final class BuildInfo {
  public static final String VERSION = "1.1.0";
  private BuildInfo() {}
}
```

- [ ] **Step 5: Verify the baseline and commit it**

Run: `mvn -Dtest=BuildInfoTest test`

Expected: PASS.

Run: `mvn verify`

Expected: PASS with no integration tests yet.

Commit:

```bash
git add pom.xml .gitignore src
git commit -m "build: bootstrap Spring Boot application"
```

### Task 2: Create and verify the PostgreSQL schema

**Files:**
- Create: `src/main/resources/db/migration/V1__create_schema.sql`
- Create: `src/test/java/com/bestbuy/apiplayground/support/PostgresIntegrationTest.java`
- Create: `src/test/java/com/bestbuy/apiplayground/persistence/SchemaMigrationIT.java`

**Interfaces:**
- Produces PostgreSQL tables `products`, `categories`, `stores`, `services`, `zipcodes`, `product_categories`, `store_services`, `sub_categories`, `category_paths`, and `seed_history`.
- Later seed and JPA code relies on the names and columns declared in this migration.

- [ ] **Step 1: Write the failing Flyway schema integration test**

Create an abstract Testcontainers base with a static `PostgreSQLContainer<>("postgres:16-alpine")`, `@DynamicPropertySource` binding its JDBC URL/credentials, and `@SpringBootTest`. Its `@BeforeEach` truncates all ten project tables with `TRUNCATE ... RESTART IDENTITY CASCADE` so each API test gets isolated data. Then write:

```java
class SchemaMigrationIT extends PostgresIntegrationTest {
  @Autowired JdbcTemplate jdbc;

  @Test
  void creates_legacy_data_and_seed_state_tables() {
    assertThat(jdbc.queryForObject("select to_regclass('products')", String.class)).isEqualTo("products");
    assertThat(jdbc.queryForObject("select to_regclass('product_categories')", String.class)).isEqualTo("product_categories");
    assertThat(jdbc.queryForObject("select to_regclass('seed_history')", String.class)).isEqualTo("seed_history");
  }
}
```

- [ ] **Step 2: Run the integration test and verify it fails because the tables do not exist**

Run: `mvn -Dit.test=SchemaMigrationIT verify`

Expected: FAIL with a `null` `to_regclass` result or a missing-table assertion.

- [ ] **Step 3: Implement the Flyway migration**

In `V1__create_schema.sql`, create all five main tables with legacy ID types and `TIMESTAMPTZ` `created_at`/`updated_at` columns. Use `NUMERIC(12,2)` for price/shipping and coordinate values, `BIGINT` IDs for product/store/service, and a `VARCHAR(255)` category/ZIP key. Create relationship tables with composite keys and foreign keys:

```sql
CREATE TABLE product_categories (
  product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  category_id VARCHAR(255) NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (product_id, category_id)
);
```

Repeat that shape for `store_services`, `sub_categories`, and `category_paths`; `store_services` retains its timestamps for the legacy embedded `storeservices` response. Add `seed_history(dataset_id VARCHAR(80) PRIMARY KEY, source_checksum VARCHAR(64) NOT NULL, completed_at TIMESTAMPTZ NOT NULL)` and indexes for all filter and relationship paths.

- [ ] **Step 4: Re-run the schema integration test**

Run: `mvn -Dit.test=SchemaMigrationIT verify`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/resources/db src/test/java/com/bestbuy/apiplayground
git commit -m "feat: add PostgreSQL schema migration"
```

### Task 3: Map the domain and relationship response shape

**Files:**
- Create: `src/main/java/com/bestbuy/apiplayground/domain/{Product,Category,Store,StoreService,ServiceOffering,Zipcode}.java`
- Create: `src/main/java/com/bestbuy/apiplayground/persistence/{ProductRepository,CategoryRepository,StoreRepository,ServiceOfferingRepository,ZipcodeRepository}.java`
- Create: `src/main/java/com/bestbuy/apiplayground/api/{PageResponse,LegacyResponseProjector}.java`
- Test: `src/test/java/com/bestbuy/apiplayground/api/LegacyResponseProjectorTest.java`

**Interfaces:**
- Produces `PageResponse(long total, int limit, int skip, List<Map<String,Object>> data)`.
- Produces `LegacyResponseProjector.product`, `.category`, `.store`, and `.service` methods taking an entity, selected fields, and `singleItem` flag.
- Resource controllers in Tasks 6–9 return these maps instead of JPA entities.

- [ ] **Step 1: Write failing response-shape tests with in-memory entity fixtures**

```java
@Test
void projects_a_store_with_legacy_store_services_when_no_select_is_requested() {
  Store store = Fixtures.storeWithService(7L, "Roseville", 2L, "Best Buy Mobile");
  Map<String, Object> response = projector.store(store, List.of(), true);

  assertThat(response).containsEntry("id", 7L).containsKey("services");
  Map<?, ?> nested = (Map<?, ?>) ((List<?>) response.get("services")).getFirst();
  assertThat(nested).containsKey("storeservices");
}

@Test
void collection_projection_contains_only_requested_product_fields() {
  assertThat(projector.product(Fixtures.product(), List.of("name", "price"), false))
      .containsOnlyKeys("name", "price");
}
```

- [ ] **Step 2: Run the test and verify it fails because the projector/domain types are absent**

Run: `mvn -Dtest=LegacyResponseProjectorTest test`

Expected: compilation failure for missing projector and entities.

- [ ] **Step 3: Add JPA entities, repositories, and the projector**

Map fields to the migration’s snake_case names. `Product` has `@ManyToMany Set<Category> categories`; `Category` has two named self-referential read collections for `subCategories` and `categoryPath`; `Store` has `@OneToMany Set<StoreService> storeServices`; `StoreService` contains a `ServiceOffering`, source timestamps, and the composite store/service key. Repositories extend `JpaRepository` and `JpaSpecificationExecutor` where filtering is required.

Implement the projector with these rules:

```java
Map<String, Object> product(Product product, List<String> fields, boolean singleItem);
Map<String, Object> category(Category category, List<String> fields, boolean singleItem);
Map<String, Object> store(Store store, List<String> fields, boolean singleItem);
Map<String, Object> service(ServiceOffering service, List<String> fields, boolean singleItem);
```

With `fields.isEmpty()`, include all scalar values and the specified shallow relationships. With fields present, emit exactly those fields for list rows and add `id` only when `singleItem` is true. Convert timestamp values to ISO-8601 JSON values and decimals to numeric JSON values.

- [ ] **Step 4: Verify the focused projector tests pass**

Run: `mvn -Dtest=LegacyResponseProjectorTest test`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/java src/test/java
git commit -m "feat: map legacy domain relationships"
```

### Task 4: Parse and safely translate legacy query parameters

**Files:**
- Create: `src/main/java/com/bestbuy/apiplayground/query/{LegacyQuery,LegacySort,Comparison,Filter,LegacyQueryParser,LegacySpecificationFactory}.java`
- Test: `src/test/java/com/bestbuy/apiplayground/query/LegacyQueryParserTest.java`

**Interfaces:**
- Produces `LegacyQuery parse(MultiValueMap<String,String> parameters, Set<String> allowedFields, Set<String> relationshipPrefixes)`.
- Produces `Pageable pageable(LegacyQuery query)` and resource-specific `Specification<T>` methods.
- Controllers in Tasks 6–9 use only this parser; no request value is concatenated into SQL.

- [ ] **Step 1: Write failing parser tests for every legacy query form**

```java
@Test
void parses_paging_sort_select_and_wildcard_filter() {
  LegacyQuery query = parser.parse(params(
      "$limit", "15", "$skip", "5", "$sort[price]", "-1",
      "$select[]", "name", "$select[]", "price", "name[$like]", "*TV*"),
      PRODUCT_FIELDS, Set.of("category"));

  assertThat(query.limit()).isEqualTo(15);
  assertThat(query.skip()).isEqualTo(5);
  assertThat(query.sort()).isEqualTo(new LegacySort("price", Sort.Direction.DESC));
  assertThat(query.filters()).contains(new Filter("name", Comparison.LIKE, "%TV%"));
}

@Test
void rejects_an_unknown_filter_field_with_a_bad_request_exception() {
  assertThatThrownBy(() -> parser.parse(params("drop_table", "x"), PRODUCT_FIELDS, Set.of("category")))
      .isInstanceOf(BadRequestException.class);
}
```

- [ ] **Step 2: Run the parser test and verify it fails because parser types are absent**

Run: `mvn -Dtest=LegacyQueryParserTest test`

Expected: compilation failure for missing `LegacyQueryParser`.

- [ ] **Step 3: Implement the immutable query model and parser**

Parse direct equality, `$eq`, `$lt`, `$lte`, `$gt`, `$gte`, repeated `$in`/`$nin`, `$like`, `$notLike`, `$limit`, `$skip`, `$sort[field]`, and `$select[]`. Limit defaults to 10, maximum is 25, and negative skip/limit or unsupported fields throw `BadRequestException`. Normalize `*` only inside LIKE values. Recognize both dotted and bracket relationship aliases and store them as dedicated relationship filters.

Implement `LegacySpecificationFactory` with Criteria API parameters, including `join("categories")` for products and `join("storeServices").join("service")` for stores. For a `near` ZIP, the store service supplies a bounding box predicate rather than parsing a SQL fragment.

- [ ] **Step 4: Re-run parser tests**

Run: `mvn -Dtest=LegacyQueryParserTest test`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/java/com/bestbuy/apiplayground/query src/test/java/com/bestbuy/apiplayground/query
git commit -m "feat: support legacy query parameters"
```

### Task 5: Add shared web errors, validation, and read-only protection

**Files:**
- Create: `src/main/java/com/bestbuy/apiplayground/error/{ApiError,ApiExceptionHandler,BadRequestException,NotFoundException}.java`
- Create: `src/main/java/com/bestbuy/apiplayground/service/ReadOnlyGuard.java`
- Create: `src/main/java/com/bestbuy/apiplayground/config/ApiProperties.java`
- Create: `src/main/java/com/bestbuy/apiplayground/api/{ProductWriteRequest,CategoryWriteRequest,StoreWriteRequest,ServiceWriteRequest}.java`
- Test: `src/test/java/com/bestbuy/apiplayground/api/ApiExceptionHandlerTest.java`

**Interfaces:**
- Produces `ReadOnlyGuard.checkWritable()`; it throws a 405 API exception when `app.read-only` is true.
- Produces request records used by the four controllers.
- Produces an error body with `name`, `message`, `code`, and validation `errors` when relevant.

- [ ] **Step 1: Write failing web tests for validation and read-only behavior**

```java
@Test
void returns_400_with_one_error_for_a_product_price_with_three_fraction_digits() throws Exception {
  mvc.perform(post("/products").contentType(APPLICATION_JSON)
          .content("""{"name":"P","type":"T","upc":"1","description":"D","model":"M","price":1.111}"""))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.name").value("BadRequest"))
      .andExpect(jsonPath("$.errors[0]").value("'price' should be multiple of 0.01"));
}

@Test
void rejects_mutation_when_read_only() throws Exception {
  mvc.perform(post("/products").contentType(APPLICATION_JSON).content("{}"))
      .andExpect(status().isMethodNotAllowed())
      .andExpect(jsonPath("$.message").value(containsString("read-only")));
}
```

- [ ] **Step 2: Run tests and verify they fail because the handler/requests are absent**

Run: `mvn -Dtest=ApiExceptionHandlerTest test`

Expected: compilation failure for missing request and error types.

- [ ] **Step 3: Implement DTO validation and exception mapping**

Use `@JsonIgnoreProperties(ignoreUnknown = false)`, `@NotBlank`, `@Size`, `@Digits(integer = 10, fraction = 2)`, and the legacy field limits from the source schemas. Create `ApiExceptionHandler` methods for `MethodArgumentNotValidException`, `BadRequestException`, `NotFoundException`, `NoHandlerFoundException`, and `HttpRequestMethodNotSupportedException`; map a no-handler exception to `{ "name": "NotFound", "message": "Page not found", "code": 404 }`. Enable throwing no-handler exceptions in `application.yml`. Map malformed/unknown request fields to 400. `ReadOnlyGuard` reads `ApiProperties.readOnly` and is called before every POST/PUT/PATCH/DELETE action.

- [ ] **Step 4: Re-run shared web tests**

Run: `mvn -Dtest=ApiExceptionHandlerTest test`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/java/com/bestbuy/apiplayground/{api,config,error,service} src/test/java/com/bestbuy/apiplayground/api
git commit -m "feat: add API validation and error handling"
```

### Task 6: Implement the products API

**Files:**
- Create: `src/main/java/com/bestbuy/apiplayground/service/ProductService.java`
- Create: `src/main/java/com/bestbuy/apiplayground/api/ProductController.java`
- Test: `src/test/java/com/bestbuy/apiplayground/api/ProductApiIT.java`

**Interfaces:**
- Produces all `/products` GET/POST and `/products/{id}` GET/PUT/PATCH/DELETE handlers.
- Consumes `LegacyQueryParser`, `LegacyResponseProjector`, `ProductRepository`, `ReadOnlyGuard`, and `ProductWriteRequest`.

- [ ] **Step 1: Write failing PostgreSQL API tests**

```java
@Test
void finds_products_by_category_and_returns_categories_without_a_projection() throws Exception {
  fixtures.persistProductInCategory("abcat0101000", "TVs", 7425383L, "Television");
  mvc.perform(get("/products").param("category.name", "TVs"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.total").value(1))
      .andExpect(jsonPath("$.data[0].categories[0].name").value("TVs"));
}

@Test
void creates_then_patches_and_deletes_a_product() throws Exception {
  String body = """{"name":"New Product","type":"Hard Good","upc":"12345676","description":"A test product","model":"NP12345"}""";
  String location = mvc.perform(post("/products").contentType(APPLICATION_JSON).content(body))
      .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
  mvc.perform(patch(location).contentType(APPLICATION_JSON).content("{\"price\":99.99}"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.price").value(99.99));
  mvc.perform(delete(location)).andExpect(status().isOk());
}
```

Also cover `$limit/$skip`, `$sort[price]`, `$select[]`, direct comparison filters, both category alias forms, 404, and full `PUT` replacement validation.

- [ ] **Step 2: Run product integration tests and verify they fail because the endpoint is absent**

Run: `mvn -Dit.test=ProductApiIT verify`

Expected: FAIL with HTTP 404.

- [ ] **Step 3: Implement product service and controller**

Expose exact handlers:

```java
@GetMapping("/products") PageResponse find(@RequestParam MultiValueMap<String,String> params);
@PostMapping("/products") ResponseEntity<Map<String,Object>> create(@Valid @RequestBody ProductWriteRequest request);
@GetMapping("/products/{id}") Map<String,Object> get(@PathVariable long id, @RequestParam MultiValueMap<String,String> params);
@PutMapping("/products/{id}") Map<String,Object> replace(...);
@PatchMapping("/products/{id}") Map<String,Object> patch(@PathVariable long id, @RequestBody Map<String,Object> changes);
@DeleteMapping("/products/{id}") Map<String,Object> delete(@PathVariable long id);
```

Use `ProductService` to apply parsed Criteria specifications, fetch categories only when no select list exists, and update only known patchable fields. Return `Location: /products/{id}` on creation.

- [ ] **Step 4: Re-run products integration tests**

Run: `mvn -Dit.test=ProductApiIT verify`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/java/com/bestbuy/apiplayground/{api,service} src/test/java/com/bestbuy/apiplayground/api/ProductApiIT.java
git commit -m "feat: migrate products API"
```

### Task 7: Implement categories and services APIs

**Files:**
- Create: `src/main/java/com/bestbuy/apiplayground/service/{CategoryService,ServiceOfferingService}.java`
- Create: `src/main/java/com/bestbuy/apiplayground/api/{CategoryController,ServiceController}.java`
- Test: `src/test/java/com/bestbuy/apiplayground/api/{CategoryApiIT,ServiceApiIT}.java`

**Interfaces:**
- Produces full CRUD controllers for `/categories` and `/services`.
- Category default reads use the projector’s shallow `subCategories` and `categoryPath` relationship representation.

- [ ] **Step 1: Write failing category and service API tests**

```java
@Test
void category_detail_contains_shallow_category_path_but_projection_does_not() throws Exception {
  fixtures.persistCategoryPath("parent", "Parent", "child", "Child");
  mvc.perform(get("/categories/parent"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.subCategories[0].id").value("child"));
  mvc.perform(get("/categories/parent").param("$select[]", "name"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("parent"))
      .andExpect(jsonPath("$.subCategories").doesNotExist());
}

@Test
void service_list_supports_wildcard_name_filter() throws Exception {
  fixtures.persistService(4L, "Kitchen Installation");
  mvc.perform(get("/services").param("name[$like]", "*Kitchen*"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
}
```

Also cover category create/put/patch/delete, service create/put/patch/delete, pagination, projection, read-only rejection, and unknown-ID 404s.

- [ ] **Step 2: Run the two focused test classes and verify 404 failures**

Run: `mvn -Dit.test=CategoryApiIT,ServiceApiIT verify`

Expected: FAIL with HTTP 404 for both resources.

- [ ] **Step 3: Implement category and service CRUD**

Follow the exact handler signatures established for products, changing identifier types to `String` for categories and `long` for services. Category writes require both `id` and `name`; service writes require `name`. Use `LegacyQueryParser` with resource-specific allowed fields and projector output rules.

- [ ] **Step 4: Re-run focused integration tests**

Run: `mvn -Dit.test=CategoryApiIT,ServiceApiIT verify`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/java/com/bestbuy/apiplayground/{api,service} src/test/java/com/bestbuy/apiplayground/api/{CategoryApiIT,ServiceApiIT}.java
git commit -m "feat: migrate categories and services APIs"
```

### Task 8: Implement stores API and nearby search

**Files:**
- Create: `src/main/java/com/bestbuy/apiplayground/service/{StoreService,NearbyStoreSearch}.java`
- Create: `src/main/java/com/bestbuy/apiplayground/api/StoreController.java`
- Test: `src/test/java/com/bestbuy/apiplayground/api/StoreApiIT.java`

**Interfaces:**
- Produces full CRUD handlers for `/stores` and `/stores/{id}`.
- Produces `NearbyStoreSearch.boundsForZip(String zip, BigDecimal miles)` returning latitude/longitude bounds for Criteria filters.

- [ ] **Step 1: Write failing store API and proximity tests**

```java
@Test
void finds_stores_near_a_zip_and_by_service_alias() throws Exception {
  fixtures.persistZip("90210", new BigDecimal("34.0901"), new BigDecimal("-118.4065"));
  fixtures.persistStoreWithService(7L, "Los Angeles", new BigDecimal("34.05"), new BigDecimal("-118.25"), 2L, "Best Buy Mobile");
  mvc.perform(get("/stores").param("near", "90210").param("service[name]", "Best Buy Mobile"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
      .andExpect(jsonPath("$.data[0].services[0].storeservices.storeId").value(7));
}

@Test
void returns_404_when_near_zip_does_not_exist() throws Exception {
  mvc.perform(get("/stores").param("near", "00000")).andExpect(status().isNotFound());
}
```

Also cover both dotted service aliases, ID aliases, store wildcard and state filtering, projection, pagination, CRUD, read-only behavior, and a default 10-mile radius.

- [ ] **Step 2: Run the integration test and verify it fails because `/stores` is absent**

Run: `mvn -Dit.test=StoreApiIT verify`

Expected: FAIL with HTTP 404.

- [ ] **Step 3: Implement store service, geographic bounds, and controller**

Use the legacy approximation: convert miles to meters with `1609`, use Earth radius `6371000`, calculate north/east/south/west positions, then filter latitude/longitude inclusively between the resulting bounds. Resolve ZIPs from `ZipcodeRepository`; return `NotFoundException` when absent. Fetch `StoreService` relationship rows only for unprojected output and serialize the nested key as `storeservices` with `createdAt`, `updatedAt`, `storeId`, and `serviceId`.

- [ ] **Step 4: Re-run store integration tests**

Run: `mvn -Dit.test=StoreApiIT verify`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/java/com/bestbuy/apiplayground/{api,service} src/test/java/com/bestbuy/apiplayground/api/StoreApiIT.java
git commit -m "feat: migrate stores API"
```

### Task 9: Add utility routes, static content, and OpenAPI compatibility

**Files:**
- Create: `src/main/java/com/bestbuy/apiplayground/service/HealthService.java`
- Create: `src/main/java/com/bestbuy/apiplayground/api/UtilityController.java`
- Create: `src/main/java/com/bestbuy/apiplayground/config/{WebConfiguration,OpenApiConfiguration}.java`
- Copy: legacy `public/*` to `src/main/resources/static/`
- Copy: legacy `markdown/index.md` and `markdown/queries.md` to `src/main/resources/pages/`
- Test: `src/test/java/com/bestbuy/apiplayground/api/UtilityApiIT.java`

**Interfaces:**
- Produces `GET /version`, `GET /healthcheck`, `/swagger.json`, `/docs`, `/`, and `/queries`.
- `HealthService.status()` returns version-independent uptime/read-only/count data.

- [ ] **Step 1: Write failing utility and static route tests**

```java
@Test
void returns_legacy_version_and_health_shape() throws Exception {
  mvc.perform(get("/version")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value("1.1.0"));
  mvc.perform(get("/healthcheck"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.uptime").isNumber())
      .andExpect(jsonPath("$.documents.products").isNumber());
}

@Test
void serves_legacy_landing_page_and_openapi_document() throws Exception {
  mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("Best Buy")));
  mvc.perform(get("/swagger.json")).andExpect(status().isOk()).andExpect(jsonPath("$.info.version").value("1.1.0"));
}
```

- [ ] **Step 2: Run the test and verify routes fail with 404**

Run: `mvn -Dit.test=UtilityApiIT verify`

Expected: FAIL with HTTP 404.

- [ ] **Step 3: Implement utilities and copy assets**

`UtilityController` returns `{ "version": BuildInfo.VERSION }` and `{ "uptime": <seconds>, "readonly": <boolean>, "documents": { "products": <count>, "stores": <count>, "categories": <count> } }`. Configure Springdoc OpenAPI metadata and set its API-docs path to `/swagger.json` and UI path to `/docs`. Serve copied CSS/favicon assets statically. Add a controller that reads only the bundled `index.md` and `queries.md`, renders them with CommonMark into the legacy HTML shell, and returns 404 for other page names.

- [ ] **Step 4: Re-run utility tests**

Run: `mvn -Dit.test=UtilityApiIT verify`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/java src/main/resources src/test/java/com/bestbuy/apiplayground/api/UtilityApiIT.java pom.xml
git commit -m "feat: add utility and documentation routes"
```

### Task 10: Implement the idempotent SQLite-to-PostgreSQL seeder

**Files:**
- Create: `src/main/java/com/bestbuy/apiplayground/seed/{SeedProperties,SeedCoordinator,SqliteDatasetReader,PostgresDatasetWriter,SeedCommandRunner}.java`
- Test: `src/test/java/com/bestbuy/apiplayground/seed/SeedCoordinatorIT.java`

**Interfaces:**
- Produces `SeedCoordinator.seedIfRequired(): SeedResult`.
- Produces seed-only application mode when `APP_SEED_MODE=required`; success exits 0, failure exits non-zero.
- Uses dataset id `legacy-dataset-v1` and validates the exact source baseline counts listed in the spec.

- [ ] **Step 1: Write failing Testcontainers seed tests against a compact SQLite fixture**

```java
@Test
void imports_all_rows_relationships_and_original_ids_once() {
  Path fixture = createSqliteFixture(tempDir.resolve("fixture.sqlite"));
  coordinator.setSource(fixture);
  SeedResult first = coordinator.seedIfRequired();
  SeedResult second = coordinator.seedIfRequired();

  assertThat(first.seeded()).isTrue();
  assertThat(jdbc.queryForObject("select count(*) from products", Long.class)).isEqualTo(2L);
  assertThat(jdbc.queryForObject("select count(*) from product_categories", Long.class)).isEqualTo(2L);
  assertThat(second.seeded()).isFalse();
  assertThat(jdbc.queryForObject("select count(*) from seed_history", Long.class)).isEqualTo(1L);
}

@Test
void refuses_to_seed_an_unmarked_nonempty_database() {
  jdbc.update("insert into services(id, name, created_at, updated_at) values (99, 'unrelated', now(), now())");
  assertThatThrownBy(() -> coordinator.seedIfRequired()).isInstanceOf(IllegalStateException.class);
}
```

- [ ] **Step 2: Run seed tests and verify they fail because coordinator types are absent**

Run: `mvn -Dit.test=SeedCoordinatorIT verify`

Expected: compilation failure for missing `SeedCoordinator`.

- [ ] **Step 3: Implement reader, writer, coordinator, and seed-only runner**

In `SeedCoordinatorIT`, implement `createSqliteFixture(Path)` with SQLite JDBC `CREATE TABLE` statements that match the nine legacy source tables and inserts for two products, two categories, one store, one service, one ZIP, and their relationship rows. `SqliteDatasetReader` opens the configured source with SQLite JDBC read-only and streams rows from those nine source tables. `PostgresDatasetWriter` uses parameterized `JdbcTemplate.batchUpdate` batches of 1,000 rows. The coordinator executes inside one transaction in this exact order: categories, products, stores, services, zipcodes, productcategory, subcategories, categorypath, storeservices. Map source camel-case timestamps/foreign-key names to the schema names.

Before writing, SHA-256 the source resource, check `seed_history`, and compare all destination table counts. On a marker mismatch, fail; on matching marker/counts, return `SeedResult.skipped()`. Without a marker, require every data table empty. After writing, reset product/store/service sequences with `setval`, compare all nine source/destination counts, check each relationship table has no broken foreign keys, then insert the marker. Do not insert the marker until every check passes.

`SeedCommandRunner` runs only for `app.seed.mode=required`, calls the coordinator, and exits the non-web Spring application with the coordinator result. The normal web application leaves seeding disabled because the Docker script handles it before starting the server.

- [ ] **Step 4: Re-run seed tests**

Run: `mvn -Dit.test=SeedCoordinatorIT verify`

Expected: PASS.

- [ ] **Step 5: Run all verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add src/main/java/com/bestbuy/apiplayground/seed src/test/java/com/bestbuy/apiplayground/seed
git commit -m "feat: seed PostgreSQL from SQLite snapshot"
```

### Task 11: Package the source dataset and Docker deployment

**Files:**
- Copy: `../api-playground/dataset.sqlite` to `src/main/resources/seed/dataset.sqlite`
- Create: `Dockerfile`, `docker-compose.yml`, `.env.example`, `bin/entrypoint.sh`, `bin/seed-database.sh`, `scripts/verify-compose.sh`
- Test: `scripts/verify-compose.sh`

**Interfaces:**
- Produces `docker compose up --build` startup at `http://localhost:3030`.
- `bin/seed-database.sh` invokes `java -jar app.jar --spring.main.web-application-type=none --app.seed.mode=required`.
- `bin/entrypoint.sh` runs the seed script and then `exec java -jar app.jar`.

- [ ] **Step 1: Write a failing Docker Compose smoke script**

Create `scripts/verify-compose.sh`:

```sh
#!/usr/bin/env sh
set -eu
docker compose up --build -d
trap 'docker compose down -v' EXIT
until curl --fail --silent http://localhost:3030/healthcheck >/tmp/health.json; do sleep 2; done
test "$(jq -r '.documents.products' /tmp/health.json)" = "51957"
test "$(curl --fail --silent 'http://localhost:3030/stores?near=90210' | jq '.total')" -gt 0
docker compose restart app
until curl --fail --silent http://localhost:3030/healthcheck >/dev/null; do sleep 2; done
test "$(docker compose exec -T postgres psql -U api_playground -d api_playground -Atc 'select count(*) from seed_history')" = "1"
```

- [ ] **Step 2: Run the smoke script and verify it fails because Compose files do not exist**

Run: `scripts/verify-compose.sh`

Expected: FAIL with a missing `docker-compose.yml` or unavailable application endpoint.

- [ ] **Step 3: Create the Docker assets and package the dataset**

Use a multi-stage Dockerfile: Maven 3 with Java 21 builds `mvn -DskipTests package`; a Java 21 JRE runtime image copies the jar, shell scripts, and resource files and runs as a non-root user. Use these Compose essentials:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-api_playground}
      POSTGRES_USER: ${POSTGRES_USER:-api_playground}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-api_playground}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
  app:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
    ports: ["3030:3030"]
```

Declare a named `postgres_data` volume and map the `app` datasource variables to the Compose database values. Copy the 58 MB `dataset.sqlite` source into the Spring resources path. Make both scripts executable. Add `.env.example` with only non-secret local-development defaults.

- [ ] **Step 4: Run Docker Compose smoke verification**

Run: `scripts/verify-compose.sh`

Expected: PASS: first start imports all data, the API responds, and restart retains exactly one `seed_history` marker.

- [ ] **Step 5: Run Maven verification and commit**

Run: `mvn verify`

Expected: PASS.

Commit:

```bash
git add Dockerfile docker-compose.yml .env.example bin scripts src/main/resources/seed/dataset.sqlite
git commit -m "feat: add Docker PostgreSQL deployment"
```

### Task 12: Final compatibility sweep and operating documentation

**Files:**
- Create: `README.md`
- Modify: API integration tests from Tasks 6–9 to add one assertion for every legacy route/query contract.
- Modify: `scripts/verify-compose.sh` to include `/version`, `/swagger.json`, `/docs`, and a default collection request.

**Interfaces:**
- Produces complete instructions for local Maven execution, Docker deployment, environment configuration, test commands, seed behavior, and explicit destructive reseed command.

- [ ] **Step 1: Add failing contract assertions for the remaining public surface**

```java
@Test
void rejects_unknown_routes_with_legacy_not_found_shape() throws Exception {
  mvc.perform(get("/path/to/nowhere"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.name").value("NotFound"))
      .andExpect(jsonPath("$.message").value("Page not found"))
      .andExpect(jsonPath("$.code").value(404));
}
```

Add parameterized GET cases for every legacy listed query: product `$in`/`$nin`, price comparisons and sort; category wildcard; stores state, both service alias forms, near/miles; services wildcard; each resource projection; and `/version`, `/healthcheck`, `/swagger.json`, `/docs`, `/`, and `/queries`.

- [ ] **Step 2: Run the full test suite and observe any contract gaps**

Run: `mvn verify`

Expected: FAIL only for an explicit missing compatibility assertion/behavior. Correct the smallest production code required; do not weaken the assertions.

- [ ] **Step 3: Write README and close discovered compatibility gaps**

Document these exact commands:

```bash
docker compose up --build
curl http://localhost:3030/healthcheck
mvn verify
docker compose down -v   # destructive: removes only the local Compose database volume
docker compose up --build
```

Explain all configuration variables, seed marker behavior, source-data licensing, the source SQLite file’s read-only role, and that the normal startup path never overwrites an existing seeded database.

- [ ] **Step 4: Run final verification**

Run: `mvn verify`

Expected: PASS.

Run: `scripts/verify-compose.sh`

Expected: PASS.

Run: `git status --short`

Expected: no unexpected files; all intentional documentation changes are visible before staging.

- [ ] **Step 5: Commit final documentation and review-ready state**

```bash
git add README.md scripts src/test
git commit -m "docs: document Spring Boot PostgreSQL deployment"
git status --short
```

Expected: clean working tree.

## Plan self-review

* **Spec coverage:** Tasks 1–5 establish the application, schema, response/error compatibility, validation, and safe legacy query parsing. Tasks 6–9 deliver every API route, relationships, static content, and documentation routes. Task 10 implements idempotent validated seeding. Task 11 delivers Docker Compose and Task 12 verifies/document the end-to-end result. Every scope item and acceptance criterion has a task.
* **Placeholder scan:** The plan contains no deferred work labels, generic testing instructions, or undefined implementation gaps. Each task names exact files, interfaces, tests, commands, expected failures, expected passes, and a commit.
* **Type consistency:** Controllers consume `LegacyQuery`, `PageResponse`, `LegacyResponseProjector`, request records, and resource services under the same names introduced above. Seeding exposes `SeedCoordinator.seedIfRequired()` and Docker invokes only the documented seed-only mode.
