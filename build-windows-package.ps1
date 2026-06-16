param(
    [string]$AppName = 'Bible Verse Extractor',
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

    Write-Host 'Preparing jpackage input...' -ForegroundColor Cyan
    Remove-Item -Recurse -Force $distDir, $inputDir -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $inputDir | Out-Null
    Copy-Item -Path $appJar.FullName -Destination (Join-Path $inputDir $appJar.Name)

    $jpackageArgs = @(
        '--type', 'app-image',
        '--name', $AppName,
        '--input', $inputDir,
        '--main-jar', $appJar.Name,
        '--dest', $distDir,
        '--app-version', $appVersion,
        '--vendor', 'Bible Verse Tool'
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

