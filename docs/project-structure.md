# HRSphere Project Structure

## Root aggregator

The repository is a Maven multi-module project with a root aggregator POM in `pom.xml`.

- The root project uses `packaging = pom`.
- It pins Java 21 and the Spring Boot BOM so child modules inherit dependency versions.
- It manages build plugins and formatting rules centrally.

## `common` module

The `common` module contains shared cross-cutting code that all future services will depend on.

Current responsibilities:

- `com.hrsphere.common.exception`
  - `BaseException`
  - `ResourceNotFoundException`
  - `ValidationException`
- `com.hrsphere.common.dto`
  - `ApiErrorResponse`
- `com.hrsphere.common.entity`
  - `BaseEntity` with JPA auditing support
- `com.hrsphere.common.util`
  - `DateTimeUtil`

Future service modules such as `auth-service`, `employee-service`, and others will be added as separate Maven modules and can reuse `common` for shared exception handling, DTO shapes, JPA base entities, and utility helpers.

## Formatting and style

Spotless is configured in the root POM using Google Java Format. This means:

- `./mvnw spotless:apply` will format Java source files
- `./mvnw spotless:check` will fail the build when formatting is inconsistent

## Build commands

- `./mvnw clean install`
- `./mvnw test -pl common`
- `./mvnw spotless:check`
- `./mvnw spotless:apply`
