# Reads the Maven version, publishes one Git tag, then saves the next snapshot.
# Requires Windows PowerShell 5.1 or newer. No Python or extra installs.
$ErrorActionPreference = 'Stop'

function Read-TextFile([string]$Path) {
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
    $utf8 = New-Object System.Text.UTF8Encoding $hasBom
    return [System.IO.File]::ReadAllText($Path, $utf8)
}

function Write-TextFile([string]$Path, [string]$Text) {
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $hasBom = $bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
    $utf8 = New-Object System.Text.UTF8Encoding $hasBom
    [System.IO.File]::WriteAllText($Path, $Text, $utf8)
}

function Get-ProjectVersion([string]$Path) {
    $match = [regex]::Match((Read-TextFile $Path), '<version>\s*([^<]+?)\s*</version>')
    if (-not $match.Success) {
        throw "Could not find a version in $Path"
    }
    return $match.Groups[1].Value.Trim()
}

function Get-ReleaseVersion([string]$RawVersion) {
    $release = $RawVersion.Trim()
    if ($release.EndsWith('-SNAPSHOT')) {
        $release = $release.Substring(0, $release.Length - '-SNAPSHOT'.Length)
    }
    if ($release -notmatch '^\d+\.\d+\.\d+$') {
        return ''
    }
    return $release
}

function Get-NextSnapshot([string]$ReleaseVersion) {
    $parts = $ReleaseVersion.Split('.')
    $patch = [int]$parts[2] + 1
    return ($parts[0] + '.' + $parts[1] + '.' + $patch + '-SNAPSHOT')
}

function Get-PomPaths([string]$RootPom) {
    $paths = New-Object System.Collections.Generic.List[string]
    $paths.Add($RootPom)
    $rootDir = Split-Path -Parent $RootPom
    $moduleMatches = [regex]::Matches((Read-TextFile $RootPom), '<module>\s*([^<]+?)\s*</module>')
    foreach ($moduleMatch in $moduleMatches) {
        $modulePom = Join-Path (Join-Path $rootDir $moduleMatch.Groups[1].Value.Trim()) 'pom.xml'
        if (Test-Path -LiteralPath $modulePom) {
            $paths.Add($modulePom)
        }
    }
    return $paths
}

function Update-FirstVersion([string]$Path, [string]$CurrentVersion, [string]$NextVersion) {
    $text = Read-TextFile $Path
    $currentToken = "<version>$CurrentVersion</version>"
    $nextToken = "<version>$NextVersion</version>"
    $index = $text.IndexOf($currentToken)
    if ($index -lt 0) {
        return $false
    }
    $updated = $text.Remove($index, $currentToken.Length).Insert($index, $nextToken)
    Write-TextFile $Path $updated
    return $true
}

Set-Location (Join-Path $PSScriptRoot '..')
$rootPom = Join-Path (Get-Location) 'pom.xml'

$git = Get-Command git -ErrorAction SilentlyContinue
if (-not $git) {
    Write-Host "Git is not available. Install Git for Windows, then run this again."
    exit 1
}
if (-not (Test-Path -LiteralPath $rootPom)) {
    Write-Host "Could not find pom.xml."
    exit 1
}

$pending = & git status --porcelain
if ($LASTEXITCODE -ne 0) {
    Write-Host "Could not read the project status."
    exit 1
}
if ($pending) {
    Write-Host "Save your other changes first, then run this again."
    exit 2
}

$rawVersion = Get-ProjectVersion $rootPom
$releaseVersion = Get-ReleaseVersion $rawVersion
if (-not $releaseVersion) {
    Write-Host "The project version must look like 1.3.1 or 1.3.1-SNAPSHOT."
    exit 2
}

$tagName = "bible-verse-extractor-$releaseVersion"
$nextSnapshot = Get-NextSnapshot $releaseVersion

& git rev-parse --verify --quiet "refs/tags/$tagName" | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "The tag $tagName already exists. Nothing was changed."
    exit 2
}

& git tag -a $tagName -m "Release $releaseVersion"
if ($LASTEXITCODE -ne 0) {
    Write-Host "The tag was not created."
    exit 1
}

& git push origin "refs/tags/$tagName"
if ($LASTEXITCODE -ne 0) {
    & git tag -d $tagName | Out-Null
    Write-Host "The tag was not uploaded, so it was removed. Nothing else was changed."
    exit 1
}

$pomPaths = @(Get-PomPaths $rootPom)
if (-not (Update-FirstVersion $rootPom $rawVersion $nextSnapshot)) {
    Write-Host "The tag $tagName was uploaded, but the version in pom.xml was not changed."
    exit 1
}
foreach ($pomPath in $pomPaths) {
    if ($pomPath -ne $rootPom) {
        Update-FirstVersion $pomPath $rawVersion $nextSnapshot | Out-Null
    }
}

& git add -- @pomPaths
if ($LASTEXITCODE -ne 0) {
    Write-Host "The tag $tagName was uploaded, but the version update was not saved."
    exit 1
}
& git commit -m "Bump version to $nextSnapshot"
if ($LASTEXITCODE -ne 0) {
    Write-Host "The tag $tagName was uploaded, but the version update was not saved."
    exit 1
}

Write-Host "Created and uploaded tag $tagName."
Write-Host "Updated the project version to $nextSnapshot."
Write-Host "Saved that change."
exit 0
