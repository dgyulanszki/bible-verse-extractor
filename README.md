# Bible Verse Tool

Multi-module Maven project generated from `PROJECT_SETUP.md`.

## Modules

### `scraper`
Standalone verse collection utility.
- Uses **Jsoup** for static sites
- Uses **Selenium** for dynamic sites
- Exports collected verses to **SQLite**
- Includes starter classes for rate limiting, scraping coordination, and export

### `app`
Desktop application starter.
- **JavaFX** UI shell
- **Spring Boot** backend wiring and dependency injection
- **SQLite** datasource configuration
- Starter UI with translation, book, chapter, and verse selectors
- Table view, help dialog, and status area for user-friendly diagnostics

## Build

```powershell
.\mvnw clean install
```

## Run the scraper module

```powershell
.\mvnw -pl scraper exec:java
```

## Run the desktop app module

```powershell
.\mvnw -pl app spring-boot:run
```

## Packaging note

The `app` module is prepared as a desktop starter and can later be extended with a `jpackage`-based packaging profile for a portable Windows EXE.

