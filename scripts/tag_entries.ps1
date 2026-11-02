# Tags git commits and exported verse files. Requires Windows PowerShell 5.1 or newer.
# Uses only built-in PowerShell. No Python, pip packages, modules, or environment variables.
param(
    [ValidateSet('git', 'exports', 'all')]
    [string]$Source = 'all',
    [string]$Repo = '.',
    [string]$Exports,
    [string]$Rules,
    [int]$Limit = 50,
    [string]$Output,
    [switch]$ApplyGitTags,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
if ($Limit -lt 1) {
    $Limit = 1
}

function Read-Rules([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Rules file not found: $Path"
    }
    $utf8 = New-Object System.Text.UTF8Encoding $false
    $text = [System.IO.File]::ReadAllText((Resolve-Path -LiteralPath $Path), $utf8)
    return ($text | ConvertFrom-Json)
}

function New-CommitEntry($Commit, $RuleSet) {
    return [ordered]@{
        id = $Commit.id
        kind = 'commit'
        title = $Commit.title
        tags = @(Get-CommitTags $Commit.title $Commit.paths $RuleSet)
    }
}

function Get-CommitTags([string]$Subject, $Paths, $RuleSet) {
    $haystack = ($Subject + "`n" + ($Paths -join "`n")).ToLowerInvariant()
    $tags = New-Object System.Collections.Generic.List[string]
    foreach ($rule in @($RuleSet.commitTags)) {
        $matched = $false
        foreach ($keyword in @($rule.keywords)) {
            if ($keyword -and $haystack.Contains($keyword.ToLowerInvariant())) {
                $matched = $true
            }
        }
        foreach ($marker in @($rule.paths)) {
            if ($marker -and $haystack.Contains($marker.ToLowerInvariant())) {
                $matched = $true
            }
        }
        if ($matched) {
            $tags.Add([string]$rule.tag)
        }
    }
    if ($tags.Count -eq 0) {
        $tags.Add('uncategorized')
    }
    return $tags
}

function Add-GitEntries($Entries, [string]$Repository, [int]$MaxCommits, $RuleSet) {
    $pretty = '---%H%x09%s'
    $output = & git -C $Repository log "-n$MaxCommits" --name-only --pretty=format:$pretty
    if ($LASTEXITCODE -ne 0) {
        throw 'Reading git history failed.'
    }
    if (-not $output) {
        return
    }
    $current = $null
    foreach ($line in @($output)) {
        if ($line.StartsWith('---')) {
            if ($null -ne $current) {
                $Entries.Add((New-CommitEntry $current $RuleSet))
            }
            $payload = $line.Substring(3)
            $splitAt = $payload.IndexOf("`t")
            if ($splitAt -lt 0) {
                $current = @{ id = $payload; title = ''; paths = (New-Object System.Collections.Generic.List[string]) }
            } else {
                $current = @{
                    id = $payload.Substring(0, $splitAt)
                    title = $payload.Substring($splitAt + 1)
                    paths = (New-Object System.Collections.Generic.List[string])
                }
            }
            continue
        }
        if ($null -ne $current -and -not [string]::IsNullOrWhiteSpace($line)) {
            $current.paths.Add($line.Trim())
        }
    }
    if ($null -ne $current) {
        $Entries.Add((New-CommitEntry $current $RuleSet))
    }
}

function Get-ExportTags([string]$Text, $RuleSet) {
    $tags = New-Object System.Collections.Generic.List[string]
    foreach ($translation in @($RuleSet.translations)) {
        if ($translation -and $Text.ToLowerInvariant().Contains($translation.ToLowerInvariant())) {
            $tags.Add(('translation:' + (Convert-ToSlug $translation)))
        }
    }
    foreach ($property in $RuleSet.books.PSObject.Properties) {
        foreach ($name in @($property.Value)) {
            if ($name -and $Text -match ('\b' + [regex]::Escape($name) + '\b')) {
                $tags.Add(('testament:' + $property.Name))
                $tags.Add(('book:' + (Convert-ToSlug $name)))
                break
            }
        }
    }
    $pattern = '\b([1-3]?\.?\s?[A-Za-z][\w .()-]{1,40}?)\s+(\d+):(\d+)(?:\s*-\s*(\d+))?'
    $referenceCount = 0
    foreach ($match in [regex]::Matches($Text, $pattern)) {
        $book = ([regex]::Replace($match.Groups[1].Value, '\s+', ' ')).Trim()
        $versePart = $match.Groups[3].Value
        if ($match.Groups[4].Success) {
            $versePart = $versePart + '-' + $match.Groups[4].Value
        }
        $tags.Add(('reference:' + $book + ' ' + $match.Groups[2].Value + ':' + $versePart))
        $referenceCount++
        if ($referenceCount -ge 8) {
            break
        }
    }
    if ($tags.Count -eq 0) {
        $tags.Add('uncategorized')
    }
    return $tags
}

function Add-ExportEntries($Entries, [string]$ExportDir, $RuleSet) {
    if (-not (Test-Path -LiteralPath $ExportDir -PathType Container)) {
        throw "Export folder not found: $ExportDir"
    }
    $files = Get-ChildItem -LiteralPath $ExportDir -Recurse -File | Where-Object {
        $_.Extension -match '^\.(txt|md|csv)$'
    }
    $utf8 = New-Object System.Text.UTF8Encoding $false
    foreach ($file in @($files)) {
        $text = [System.IO.File]::ReadAllText($file.FullName, $utf8)
        $Entries.Add([ordered]@{
            id = $file.FullName
            kind = 'export'
            title = $file.Name
            tags = @(Get-ExportTags $text $RuleSet)
        })
    }
}

function Get-Summary($Entries) {
    $counts = @{}
    foreach ($entry in $Entries) {
        foreach ($tag in @($entry.tags)) {
            if ($tag.StartsWith('reference:')) {
                continue
            }
            if (-not $counts.ContainsKey($tag)) {
                $counts[$tag] = 0
            }
            $counts[$tag] = $counts[$tag] + 1
        }
    }
    $ordered = [ordered]@{}
    foreach ($key in ($counts.Keys | Sort-Object)) {
        $ordered[$key] = $counts[$key]
    }
    return $ordered
}

function Add-LocalGitTags([string]$Repository, $Summary, [bool]$MoveExisting) {
    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git) {
        Write-Host 'Git is not on PATH, so local topic tags were not created.'
        return
    }
    $today = Get-Date -Format 'yyyy-MM-dd'
    foreach ($key in @($Summary.Keys)) {
        $tagName = 'topic/' + (Convert-ToSlug $key)
        $message = "Automatic tag $key ($($Summary[$key]) entries) on $today"
        if ($MoveExisting) {
            & git -C $Repository tag -f -a $tagName -m $message
        } else {
            & git -C $Repository tag -a $tagName -m $message
        }
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Could not create $tagName"
        }
    }
}

function Convert-ToSlug([string]$Value) {
    $normalized = $Value.Trim().ToLowerInvariant()
    $normalized = $normalized.Replace([string][char]0x00E1, 'a').Replace([string][char]0x00E9, 'e').Replace([string][char]0x00ED, 'i')
    $normalized = $normalized.Replace([string][char]0x00F3, 'o').Replace([string][char]0x00F6, 'o').Replace([string][char]0x0151, 'o')
    $normalized = $normalized.Replace([string][char]0x00FA, 'u').Replace([string][char]0x00FC, 'u').Replace([string][char]0x0171, 'u')
    $normalized = [regex]::Replace($normalized, '[^a-z0-9]+', '-')
    $normalized = $normalized.Trim('-')
    if (-not $normalized) {
        return 'untagged'
    }
    return $normalized
}

function Resolve-OutputPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return (Join-Path (Get-Location) $Path)
}

$rulesPath = $Rules
if (-not $rulesPath) {
    $rulesPath = Join-Path $PSScriptRoot 'tag-rules.json'
}
$ruleSet = Read-Rules $rulesPath
$entries = New-Object System.Collections.Generic.List[object]

if ($Source -eq 'git' -or $Source -eq 'all') {
    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git) {
        if ($Source -eq 'git') {
            Write-Host 'Git is not on PATH. Commit tagging needs Git. Export tagging does not: use -Source exports.'
            exit 1
        }
        Write-Host 'Git is not on PATH. Commit history was skipped.'
    } else {
        Add-GitEntries $entries $Repo $Limit $ruleSet
    }
}

if ($Source -eq 'exports' -or $Source -eq 'all') {
    if (-not $Exports) {
        Write-Host 'Export tagging needs -Exports <folder>.'
        exit 2
    }
    Add-ExportEntries $entries $Exports $ruleSet
}

$summary = Get-Summary $entries
$report = New-Object psobject -Property @{
    entries = @($entries.ToArray())
    summary = $summary
}
$json = $report | ConvertTo-Json -Depth 6
if ($Output) {
    $outputPath = Resolve-OutputPath $Output
    $parent = Split-Path -Parent $outputPath
    if ($parent -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent | Out-Null
    }
    $utf8 = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($outputPath, $json + "`n", $utf8)
} else {
    Write-Output $json
}

if ($ApplyGitTags) {
    Add-LocalGitTags $Repo $summary $Force.IsPresent
}
exit 0
