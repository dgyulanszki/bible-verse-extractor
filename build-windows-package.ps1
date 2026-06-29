param(
    [string]$AppName = 'Bible Verse Extractor',
    [string]$DatabasePath,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = $scriptRoot
$appModuleDir = Join-Path $repoRoot 'app'
$targetDir = Join-Path $appModuleDir 'target'
$inputDir = Join-Path $targetDir 'jpackage-input'
$distDir = Join-Path $appModuleDir 'dist'
$iconPath = Join-Path $appModuleDir 'src\main\resources\bible-verse-app-icon.ico'
$mavenWrapper = Join-Path $repoRoot 'mvnw.cmd'
$packagedDatabaseRelativePath = 'data\bible-verses.db'

function Require-Command {
    param([string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found on PATH: $Name"
    }
}

function Resolve-AppJar {
    param([string]$SearchDir)

    $jar = Get-ChildItem -Path $SearchDir -Filter 'app-*.jar' |
        Where-Object { $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTimeUtc -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw 'Could not find the packaged app JAR in app\target.'
    }

    return $jar
}

function Resolve-AppVersion {
    param([System.IO.FileInfo]$JarFile)

    $version = $JarFile.BaseName.Substring(4)
    if ($version.EndsWith('-SNAPSHOT')) {
        return $version.Substring(0, $version.Length - '-SNAPSHOT'.Length)
    }

    return $version
}

function Resolve-DatabaseFile {
    param(
        [string]$ConfiguredDatabasePath,
        [string]$AppModuleDirectory
    )

    $candidatePaths = @()

    if (-not [string]::IsNullOrWhiteSpace($ConfiguredDatabasePath)) {
        $candidatePaths += $ConfiguredDatabasePath
    }

    $candidatePaths += (Join-Path $AppModuleDirectory $packagedDatabaseRelativePath)
    $candidatePaths += (Join-Path $HOME 'bible-verses.db')

    foreach ($candidatePath in $candidatePaths) {
        if ([string]::IsNullOrWhiteSpace($candidatePath)) {
            continue
        }

        $expandedPath = [Environment]::ExpandEnvironmentVariables($candidatePath)
        if (Test-Path -LiteralPath $expandedPath -PathType Leaf) {
            return (Resolve-Path -LiteralPath $expandedPath).Path
        }
    }

    throw (
        "Could not find the SQLite database to package. Checked these locations: " +
        ($candidatePaths -join ', ') +
        ". Run the scraper first or pass -DatabasePath with the database file to bundle."
    )
}

Push-Location $repoRoot
try {
    if (-not (Test-Path $mavenWrapper)) {
        throw 'Could not find mvnw.cmd in the repository root.'
    }

    Require-Command 'jpackage'

    if (-not $SkipBuild) {
        Write-Host 'Building the app module...' -ForegroundColor Cyan
        & $mavenWrapper -pl app clean package
        if ($LASTEXITCODE -ne 0) {
            throw 'Maven build failed.'
        }
    }

    $appJar = Resolve-AppJar -SearchDir $targetDir
    $appVersion = Resolve-AppVersion -JarFile $appJar
      $databaseFile = Resolve-DatabaseFile -ConfiguredDatabasePath $DatabasePath -AppModuleDirectory $appModuleDir

    Write-Host 'Preparing jpackage input...' -ForegroundColor Cyan
    Remove-Item -Recurse -Force $distDir, $inputDir -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $inputDir | Out-Null
    Copy-Item -Path $appJar.FullName -Destination (Join-Path $inputDir $appJar.Name)
      New-Item -ItemType Directory -Path (Split-Path -Parent (Join-Path $inputDir $packagedDatabaseRelativePath)) -Force | Out-Null
      Copy-Item -Path $databaseFile -Destination (Join-Path $inputDir $packagedDatabaseRelativePath)
      Write-Host ('Bundling SQLite database: ' + $databaseFile) -ForegroundColor Cyan

    $jpackageArgs = @(
        '--type', 'app-image',
        '--name', $AppName,
        '--input', $inputDir,
        '--main-jar', $appJar.Name,
        '--dest', $distDir,
        '--app-version', $appVersion,
            '--vendor', 'Bible Verse Tool',
            '--java-options', '--enable-native-access=ALL-UNNAMED',
            '--java-options', '-Dapp.database.path=$APPDIR\data\bible-verses.db'
    )

    if (Test-Path $iconPath) {
        Write-Host 'Using Windows icon file...' -ForegroundColor Cyan
        $jpackageArgs += @('--icon', $iconPath)
    }

    Write-Host 'Creating portable Windows app-image...' -ForegroundColor Cyan
    & jpackage @jpackageArgs
    if ($LASTEXITCODE -ne 0) {
        throw 'jpackage failed.'
    }

    $appImageDir = Join-Path $distDir $AppName
    $zipPath = Join-Path $distDir ($AppName + '-portable.zip')

    if (-not (Test-Path $appImageDir)) {
        throw 'jpackage finished, but the app-image folder was not created.'
    }

    Write-Host 'Creating ZIP archive...' -ForegroundColor Cyan
    Compress-Archive -Path $appImageDir -DestinationPath $zipPath -Force

    Write-Host ''
    Write-Host 'Packaging completed successfully.' -ForegroundColor Green
    Write-Host ('Portable folder: ' + $appImageDir)
    Write-Host ('ZIP archive:     ' + $zipPath)
}
finally {
    Pop-Location
}

