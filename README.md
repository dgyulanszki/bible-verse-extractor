# Bible Verse Tool

Multi-module Maven project for scraping Bible verses into SQLite and browsing/copying them from a desktop app.

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
- The current Maven setup uses Windows JavaFX artifacts (`win` classifiers), so the `app` module is intended to build and run on Windows unless you switch classifiers or add OS-specific Maven profiles

## Build

```powershell
.\mvnw clean install
```

When you use the Maven wrapper from this repository, native access for SQLite is enabled automatically, so Java 25+ does not print the `System::load` warning for normal Maven-based runs.

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
- The `efo` scraper now uses a local **Google Chrome** browser window because bible.com serves a JavaScript client-challenge page to raw HTTP requests.
- For normal local use, leave the browser in its default visible mode. Optional headless mode is available through `-Dscraper.dynamicBrowserHeadless=true`, but the visible browser is the safer default for this site.
- If a dynamic run fails late, you can resume from a specific chapter URL with `-Dscraper.dynamicStartUrl=...`.
- `scraper.dynamicStartUrl` should be used only when exactly one dynamic scraper is selected.
- If a scraper run fails after collecting some verses, the already collected verses are written to SQLite automatically as a fallback before the failure is reported.
- When you run the scraper through `mvnw`, the required SQLite native-access flag is already enabled automatically.

Examples:

```powershell
.\mvnw -pl scraper exec:java -Dscraper.staticScrapingEnabled=false -Dscraper.dynamicTranslations=efo
```

Resume EFO from a later chapter, for example Dániel 12:

```powershell
.\mvnw -pl scraper "-Dscraper.staticScrapingEnabled=false" "-Dscraper.dynamicTranslations=efo" "-Dscraper.dynamicStartUrl=https://www.bible.com/bible/198/DAN.12.EFO" exec:java
```

```powershell
.\mvnw -pl scraper exec:java -Dscraper.staticScrapingEnabled=false -Dscraper.dynamicScrapingEnabled=true -Dscraper.dynamicTranslations=efo
```

## Run the desktop app module

```powershell
.\mvnw -pl app spring-boot:run
```

The current `app` Maven build is Windows-targeted because `app/pom.xml` depends on JavaFX artifacts with the `win` classifier.

On first start, new users can click the `Útmutató` button in the top-right area of the app for a short quick-start guide.

When you run the desktop app through `mvnw` or through the packaged Windows app-image, the required SQLite native-access flag is already enabled automatically.

If you create a plain IntelliJ `Application` run configuration or run the packaged JAR manually with `java -jar`, add this VM option yourself:

```text
--enable-native-access=ALL-UNNAMED
```

## Build and run the packaged desktop app JAR

```powershell
.\mvnw.cmd -pl app package
java -jar .\app\target\app-<version>.jar
```

- The `package` build creates an executable Spring Boot desktop JAR for the `app` module.
- Replace `<version>` with the current app version, for example `1.0.1-SNAPSHOT`.
- If Spring Boot also leaves an `app-<version>.jar.original` file in `target`, use the plain `app-<version>.jar` file when running or packaging.

## Default SQLite database location

- By default, local development now uses a shared SQLite file at `app\data\bible-verses.db` inside the repository.
- This keeps the generated database next to the desktop app module, so the Windows packaging step can bundle it automatically.
- The database content is safe to keep in the repository: it stores Bible verses plus the app's last opened verse-range selections, not passwords, API keys, or machine-specific paths.
- Because the desktop app can save the last opened selection into the same SQLite file, `app\data\bible-verses.db` may sometimes show up as modified after you use the app locally.
- If the repository root cannot be detected, both modules fall back to `${user.home}/bible-verses.db`.
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
- copies the SQLite database into the packaged app automatically
- creates a portable Windows `app-image`
- uses `app\src\main\resources\bible-verse-app-icon.ico` automatically if that file exists
- creates a ZIP archive next to the packaged folder

By default the script looks for the database here, in this order:

- `app\data\bible-verses.db`
- `${user.home}\bible-verses.db` (legacy fallback)

If needed, you can point the script at a specific database file:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-windows-package.ps1 -DatabasePath .\some\other\folder\bible-verses.db
```

The packaged launcher is configured automatically to use the bundled database copy under `app\data\bible-verses.db` inside the portable app-image.

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

### For end users: how to use the ZIP package

If someone receives `Bible Verse Extractor-portable.zip`, they should do this:

1. Copy the ZIP file to any folder where they want to keep the app.
2. Right-click the ZIP file and choose **Extract All...**
3. Open the extracted `Bible Verse Extractor` folder.
4. Double-click `Bible Verse Extractor.exe` to start the app.

Important for non-technical users:

- Do **not** run the app from inside the ZIP preview window. Extract it first.
- Keep the whole extracted `Bible Verse Extractor` folder together. The `.exe`, the `runtime` folder, and the `app` folder belong together.
- The bundled Java runtime is already included, so no separate Java installation is needed on the target Windows machine.
- If desired, create a desktop shortcut to `Bible Verse Extractor.exe` after extraction.

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
New-Item -ItemType Directory .\app\target\jpackage-input\data | Out-Null
Copy-Item .\app\data\bible-verses.db .\app\target\jpackage-input\data\
```

#### 4. Create the portable Windows app-image

Basic command:

```powershell
jpackage --type app-image --name "Bible Verse Extractor" --input ".\app\target\jpackage-input" --main-jar "app-<version>.jar" --dest ".\app\dist" --app-version <numeric-version> --vendor "Bible Verse Tool" --java-options "-Dapp.database.path=$APPDIR\data\bible-verses.db"
```

If you later add a Windows `.ico` file, you can include it like this:

```powershell
jpackage --type app-image --name "Bible Verse Extractor" --input ".\app\target\jpackage-input" --main-jar "app-<version>.jar" --dest ".\app\dist" --app-version <numeric-version> --vendor "Bible Verse Tool" --java-options "-Dapp.database.path=$APPDIR\data\bible-verses.db" --icon ".\app\src\main\resources\bible-verse-app-icon.ico"
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

