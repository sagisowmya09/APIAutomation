# Restful Booker API Test Suite

Automated API tests for [Restful Booker](https://restful-booker.herokuapp.com/) using **Rest Assured**, **JUnit 5**, and **Maven**.

## Prerequisites

- JDK 17 or newer (`java -version`)
- Apache Maven 3.9+ (`mvn -version`)

## Setup and run

```bash
git clone <your-repo-url>
cd RestfulBrokerProject
mvn clean test
```

That is enough on a fresh machine with Maven installed. Surefire runs every test under `src/test/java`.


### Configure the base URI

Precedence (highest first):

1. Maven/system property: `-DbaseUri=...`
2. Environment variable: `RESTFUL_BOOKER_BASE_URI` (or `BASE_URI`)
3. External file `config.properties` (project root)
4. Built-in default: `https://restful-booker.herokuapp.com`

Unset/`null`/empty values and unresolved Maven placeholders like `${baseUri}` are ignored, so the same `mvn clean test` works locally and on Jenkins without extra flags.

Point at another file if needed:

```bash
mvn clean test -Dconfig.file=/path/to/config.properties
# or
export CONFIG_FILE=/path/to/config.properties
mvn clean test
```

Examples:

```bash
# Environment variable (also works when set as a Jenkins job env var)
export RESTFUL_BOOKER_BASE_URI=https://restful-booker.herokuapp.com
mvn clean test

# System property
mvn clean test -DbaseUri=https://restful-booker.herokuapp.com
```

Optional auth / timeout overrides (defaults match the public demo API):

| Setting | Env var | System property | Default |
|---|---|---|---|
| Username | `RESTFUL_BOOKER_USERNAME` | `auth.username` | `admin` |
| Password | `RESTFUL_BOOKER_PASSWORD` | `auth.password` | `password123` |
| Connect timeout (ms) | `CONNECT_TIMEOUT_MS` | `connectTimeoutMs` | `30000` |
| Read timeout (ms) | `READ_TIMEOUT_MS` | `readTimeoutMs` | `60000` |

### Run a single class or method

```bash
mvn test -Dtest=BookingLifecycleTest
mvn test -Dtest=NegativeTest#putBooking_withoutToken_returns403
```

### Jenkins

Same Maven command as local. A sample `Jenkinsfile` is included.

1. Job type: Pipeline from SCM (or Freestyle with “Invoke top-level Maven targets”: `clean test`).
2. Agent needs JDK 17+ and Maven 3.9+ on PATH (or configure Jenkins tools and uncomment the `tools {}` block).
3. Optional job env vars: `RESTFUL_BOOKER_BASE_URI`, `RESTFUL_BOOKER_USERNAME`, `RESTFUL_BOOKER_PASSWORD`.
4. Publish JUnit results from `**/target/failure-logs/TEST-*.xml`.

Surefire uses `forkCount=1` and one automatic re-run of failing tests to reduce flakes from the shared public API / network. It dynamically creates `target/failure-logs/`, which contains XML reports, text reports, and captured test output; the directory is ignored by Git.

The JUnit reporting extension also generates `target/failure-logs/index.html` with pass, failure, skipped, and aborted test results. Open it in a browser after the test run:

```bash
open target/failure-logs/index.html
```
## What the suite covers

| Area | Class | Highlights |
|---|---|---|
| Auth | `AuthTest` | Obtain token; prove it authorizes DELETE |
| Lifecycle | `BookingLifecycleTest` | Create → GET round-trip, full PUT, partial PATCH, DELETE |
| Querying | `BookingQueryTest` | Filter by name and by date; empty-result case |
| Negatives | `NegativeTest` | Bad credentials, missing/forged token, 404s, empty body, patch-after-delete |
Response validation goes beyond status codes: JSON Schema on create, field-level Hamcrest asserts, and POJO `sameAs(...)` round-trip checks so “what we created is what we get back.”

## Design decisions and trade-offs

**JUnit 5 over TestNG** — native Surefire support, `@DisplayName`, and `@TestInstance` without extra plugins. TestNG would be fine; JUnit 5 is the lower-friction default for Maven.

**Layered layout** — helpers live under `src/main` (`config` / `models` / `client` / `support`); `src/test` holds only JUnit test classes. HTTP details stay in `RestfulBookerClient`; tests stay assertion-focused.

**Independence** — every test that needs a booking creates one with a UUID-suffixed name via `BookingDataFactory`. No shared mutable fixtures, no reliance on seed IDs 1–10 (the API resets every ~10 minutes). Tests can run alone or in any order.

**Config precedence** — external `config.properties` for local defaults, env/`-D` for CI or alternate environments without rebuilding.

**Cookie token auth** — Restful Booker accepts Cookie `token=...` or Basic Auth; the suite uses Cookie tokens because that matches the assignment’s “obtain a token and use it” wording.

**Exact `Accept: application/json`** — Rest Assured’s `ContentType.JSON` expands Accept to a multi-value list; Restful Booker responds with `418 I'm a teapot` unless Accept is exactly `application/json`. The client sets that explicitly.

**Known API quirks we encode rather than fight** — DELETE returns `201`, failed auth returns `200` + `{"reason":"Bad credentials"}`, empty create body returns `500`. Tests assert observed behavior so the suite stays green against the public demo.

## What I’d do next with more time

- Parallel execution (`junit.jupiter.execution.parallel`) with stronger isolation / retry for the shared hosted API
- Contract tests (OpenAPI) and richer JSON Schema coverage for every endpoint
- WireMock (or Testcontainers) for deterministic negative paths without depending on demo quirks
- Allure/ReportPortal reporting and a small GitHub Actions workflow
- Data-driven tables for auth and validation edge cases
- Explicit cleanup hooks and metrics for flaky date-filter behavior (a known Restful Booker rough edge)

## Notes on AI tool usage

- **Cursor Agent (Composer)** drafted the Maven layout, Rest Assured client, JUnit tests, and this README from the assignment brief.
- **IDE MCP tools** (`list_directory_tree`) were used to inspect the empty project tree before scaffolding.
- **Live API probes** (`curl` against restful-booker.herokuapp.com) were run to confirm status codes and body shapes (especially auth failures, DELETE `201`, and empty-body create) before locking assertions.
- No MCP-based test-case generators or third-party codegen plugins were used; models and tests were written directly in-repo.
- Human review still needed for: flaky date-filter semantics on the public API, credential handling if pointed at a non-demo environment, and CI secrets policy.

## Project layout

```
config/
  config.properties          # external config (local + Jenkins)
src/main/java/org/example/restfulbooker/
  config/TestConfig.java
  client/RestfulBookerClient.java
  models/...
  support/BookingDataFactory.java
src/test/java/org/example/restfulbooker/tests/
  AuthTest.java
  BookingLifecycleTest.java
  BookingQueryTest.java
  NegativeTest.java
src/main/java/org/example/restfulbooker/support/
  BaseApiTest.java
src/main/java/org/example/restfulbooker/validation/
  ResponseValidator.java
src/main/java/org/example/restfulbooker/reporting/
  HtmlReportExtension.java
src/test/resources/
  schemas/booking-response-schema.json
```
