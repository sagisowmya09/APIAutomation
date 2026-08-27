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
mvn clean test
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


## What I’d do next with more time

- Parallel execution (`junit.jupiter.execution.parallel`) with stronger isolation / retry for the shared hosted API
- Allure/ReportPortal reporting and a small GitHub Actions workflow
- Data-driven tables for auth and validation edge cases
- Explicit cleanup hooks and metrics for flaky date-filter behavior (a known Restful Booker rough edge)

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
