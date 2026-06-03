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

### Static translation selection

- By default, the scraper runs all configured static translations.
- You can limit static scraping with the `scraper.staticTranslations` JVM system property.
- Available static translation ids:
  - `revidealt-karoli` (`https://www.online-biblia.ro/bible/4`)
  - `karoli-gaspar` (`https://biblia.hit.hu/bible/karoli`)
  - `revidealt-uj-forditas` (`https://abibliamindenkie.hu/uj`)

Examples:

```powershell
.\mvnw -pl scraper exec:java -Dscraper.dynamicScrapingEnabled=false -Dscraper.staticTranslations=karoli-gaspar
```

```powershell
.\mvnw -pl scraper exec:java -Dscraper.dynamicScrapingEnabled=false -Dscraper.staticTranslations=revidealt-karoli,karoli-gaspar,revidealt-uj-forditas
```

### Dynamic translation selection

- By default, the scraper runs all configured dynamic translations.
- You can limit dynamic scraping with the `scraper.dynamicTranslations` JVM system property.
- Available dynamic translation ids:
  - `efo` (`https://www.bible.com/bible/198/GEN.1.EFO`)

Examples:

```powershell
.\mvnw -pl scraper exec:java -Dscraper.staticScrapingEnabled=false -Dscraper.dynamicTranslations=efo
```

```powershell
.\mvnw -pl scraper exec:java -Dscraper.staticScrapingEnabled=false -Dscraper.dynamicScrapingEnabled=true -Dscraper.dynamicTranslations=efo
```

## Run the desktop app module

```powershell
.\mvnw -pl app spring-boot:run
```

## Default SQLite database location

- By default, both modules now use a shared SQLite file at `${user.home}/bible-verses.db`.
- You can override the desktop app path with the `BIBLE_VERSE_DB_PATH` environment variable.
- You can override the scraper output path with the `scraper.outputDatabasePath` JVM system property.

## Packaging note

The `app` module is prepared as a desktop starter and can later be extended with a `jpackage`-based packaging profile for a portable Windows EXE.

