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

## Most common Windows release command

If you just want to build the desktop app and create the portable Windows package, run this from the repository root:

```powershell
.\build-windows-package.cmd
```

This creates:

- the portable app folder under `app\dist\Bible Verse Extractor\`
- the ZIP archive under `app\dist\Bible Verse Extractor-portable.zip`

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

On first start, new users can click the `Útmutató` button in the top-right area of the app for a short quick-start guide.

## Build and run the packaged desktop app JAR

```powershell
.\mvnw.cmd -pl app package
java -jar .\app\target\app-<version>.jar
```

- The `package` build creates an executable Spring Boot desktop JAR for the `app` module.
- Replace `<version>` with the current app version, for example `1.0.1-SNAPSHOT`.
- If Spring Boot also leaves an `app-<version>.jar.original` file in `target`, use the plain `app-<version>.jar` file when running or packaging.

## Default SQLite database location

- By default, both modules now use a shared SQLite file at `${user.home}/bible-verses.db`.
- You can override the desktop app path with the `BIBLE_VERSE_DB_PATH` environment variable.
- You can override the scraper output path with the `scraper.outputDatabasePath` JVM system property.

## Package the app module for Windows

Use `jpackage --type app-image` when you want a portable Windows build that already contains its own Java runtime.

### Fastest option: use the packaging script

The repository now contains a Windows packaging script at:

- `build-windows-package.ps1`
- `build-windows-package.cmd`

Simplest Windows command:

```powershell
.\build-windows-package.cmd
```

The `.cmd` file is only a small wrapper around the PowerShell script, so you can use whichever is more convenient.

Run it from the repository root like this:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-windows-package.ps1
```

What the script does:

- builds the `app` module
- finds the freshly packaged app JAR automatically
- creates a clean temporary `jpackage` input folder
- creates a portable Windows `app-image`
- uses `app\src\main\resources\bible-verse-app-icon.ico` automatically if that file exists
- creates a ZIP archive next to the packaged folder

Optional:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-windows-package.ps1 -SkipBuild
```

Or through the wrapper:

```powershell
.\build-windows-package.cmd -SkipBuild
```

Use `-SkipBuild` only if you already built the app and just want to rerun the packaging step.

Important:

- this does **not** create one single standalone `.exe` file
- it creates a portable folder that contains:
  - `Bible Verse Extractor.exe`
  - the bundled Java runtime under `runtime/`
  - the packaged app files under `app/`
- this folder is the real deliverable; you can zip it and copy it to another Windows machine
- this portable flow does **not** require WiX Toolset

### Step-by-step: create a portable EXE + ZIP from a fresh project state

#### 1. Prerequisites

- Windows
- JDK 25 installed and available on `PATH`
- `jpackage` available on `PATH` (it ships with the JDK)

Quick check:

```powershell
java -version
jpackage --version
```

#### 2. Build the desktop application JAR

Run this from the repository root:

```powershell
.\mvnw.cmd -pl app clean package
```

After that, the runnable JAR should be here:

- `app\target\app-<version>.jar`

If Spring Boot also creates `app-<version>.jar.original`, use the plain `app-<version>.jar` file for packaging.

#### 3. Prepare a clean `jpackage` input folder

```powershell
Remove-Item -Recurse -Force .\app\dist, .\app\target\jpackage-input -ErrorAction SilentlyContinue
New-Item -ItemType Directory .\app\target\jpackage-input | Out-Null
Copy-Item .\app\target\app-<version>.jar .\app\target\jpackage-input\
```

#### 4. Create the portable Windows app-image

Basic command:

```powershell
jpackage --type app-image --name "Bible Verse Extractor" --input ".\app\target\jpackage-input" --main-jar "app-<version>.jar" --dest ".\app\dist" --app-version <numeric-version> --vendor "Bible Verse Tool"
```

If you later add a Windows `.ico` file, you can include it like this:

```powershell
jpackage --type app-image --name "Bible Verse Extractor" --input ".\app\target\jpackage-input" --main-jar "app-<version>.jar" --dest ".\app\dist" --app-version <numeric-version> --vendor "Bible Verse Tool" --icon ".\app\src\main\resources\bible-verse-app-icon.ico"
```

#### 5. Find the portable EXE

The generated launcher will be here:

- `app\dist\Bible Verse Extractor\Bible Verse Extractor.exe`

You can start the portable app directly from that folder.

Quick sanity check after packaging:

- `Bible Verse Extractor.exe` should exist
- the `runtime\` folder should also exist next to it

If `runtime\` is present, the portable build already contains Java, so the target Windows machine does not need a separate JRE installation.

#### 6. Create a ZIP for release/upload

```powershell
Compress-Archive -Path ".\app\dist\Bible Verse Extractor" -DestinationPath ".\app\dist\Bible Verse Extractor-portable.zip" -Force
```

The ZIP file will be created here:

- `app\dist\Bible Verse Extractor-portable.zip`

### Optional installer EXE

If you later want an installer-style EXE, use `jpackage --type exe` instead. That path requires WiX Toolset on `PATH`, so it is **not needed** for the portable no-JRE distribution described above.

