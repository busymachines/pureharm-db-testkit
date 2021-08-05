# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# unreleased

# 0.3.0

This is the first release for a stable Scala 3 version, and with cats-effect 3 support!

### :warning: breaking
- `pureharm-db-testkit` now depends on cats-effect `3.2.1`, and the corresponding dependencies for it.
- `pureharm-db-testkit-ce2` is binary, and source compatible with `pureharm-db-testkit` from version `0.2.0`, so if you haven't migrated to CE3 yet, use the former module.

### Dependency upgrades
- [pureharm-core](https://github.com/busymachines/pureharm-core/releases) `0.3.0`
- [pureharm-effects-cats](https://github.com/busymachines/pureharm-effects-cats/releases) `0.5.0`
- [pureharm-testkit](https://github.com/busymachines/pureharm-testkit/releases) `0.4.0`
- [pureharm-db-core](https://github.com/busymachines/pureharm-db-core/releases) `0.5.0`
- [pureharm-db-flyway](https://github.com/busymachines/pureharm-db-flyway/releases) `0.6.0`

### New Scala versions:
- `2.13.6`
- `3.0.1`
- drop `3.0.0-RC2`, `3.0.0-RC3`

### internals
- bump scalafmt to `3.0.0-RC6` — from `2.7.5`
- bump sbt to `1.5.5`
- bump sbt-spiewak to `0.21.0`
- bump sbt-scalafmt to `2.4.3`

# 0.2.0

### breaking changes:

- upgraded to `pureharm-testkit` 0.3.0` which replaces scalatest w/ munit.
- you have to use `testResource.test("name")` in all your tests to be consistent w/ munit handling of resources.
- `DBTestSetup.flywayConfig` now takes an `implicit logger`, should be source compatible. Updated to copy schema name from the db config by default. Since most likely it's wrong if the schema name is different between the two configs.

### dependency upgrades

- [pureharm-core-anomaly](https://github.com/busymachines/pureharm-core/releases) `0.2.0`
- [pureharm-core-sprout](https://github.com/busymachines/pureharm-core/releases) `0.2.0`
- [pureharm-core-identifiable](https://github.com/busymachines/pureharm-core/releases) `0.2.0`
- [pureharm-effects-cats](https://github.com/busymachines/pureharm-effects-cats/releases) `0.4.0`
- [pureharm-testkit](https://github.com/busymachines/pureharm-testkit/releases) `0.3.0`
- [pureharm-db-core](https://github.com/busymachines/pureharm-db-core/releases) `0.4.0`
- [pureharm-db-flyway](https://github.com/busymachines/pureharm-db-flyway/releases) `0.4.0`

# 0.1.0

Split out from [pureharm](https://github.com/busymachines/pureharm) as of version `0.0.7`.

:warning: Breaking changes :warning:

- renamed module maven artifact ID from `pureharm-db-core-testkit` to `pureharm-db-testkit`

- cross compiled to Scala 2.13 -- pending support for scala 3.0.0-RC1
