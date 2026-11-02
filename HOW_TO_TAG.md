# How to tag entries

`scripts/tag_entries.cmd` scans git history and exported verse files, assigns category tags, and writes a JSON report. It does not modify commits. Local git tags are created only with `-ApplyGitTags`, and they are never pushed.

## Prerequisites

The script is Windows-native. A normal Windows 10 or Windows 11 install already includes everything it needs.

| Requirement | Details |
| --- | --- |
| PowerShell | 5.1 or newer, included with Windows. No module install. |
| Python | Not used. |
| pip packages | None. |
| Environment variables | None. |
| Git hooks | None. |
| Git | Optional. Used only for commit history and `-ApplyGitTags`. Export tagging works without Git. |

No installation step is required. From the repository root:

```powershell
.\scripts\tag_entries.cmd -Source git -Limit 5
```

`tag_entries.cmd` starts PowerShell with `-ExecutionPolicy Bypass` for that process only. You do not need to change the machine execution policy.

## Run a report

Commit history:

```powershell
.\scripts\tag_entries.cmd -Source git -Limit 50
```

Exported `.txt`, `.md`, or `.csv` files:

```powershell
.\scripts\tag_entries.cmd -Source exports -Exports .\exports -Output .\tags-report.json
```

Both:

```powershell
.\scripts\tag_entries.cmd -Source all -Exports .\exports -Output .\tags-report.json
```

Each entry has an `id`, a `kind` (`commit` or `export`), a title, and a `tags` list. Commit tags look like `ui` or `fix`. Export tags look like `translation:karoli`, `testament:nt`, `book:john`, and `reference:John 3:16-18`.

## Customize the rules

Edit `scripts/tag-rules.json`, or pass another file with `-Rules`. A rule matches when a keyword is in the commit subject or a path marker is in the changed files.

```powershell
.\scripts\tag_entries.cmd -Source all -Exports .\exports -Rules .\scripts\tag-rules.json -Output .\tags-report.json
```

## Optional local git tags

This creates one annotated tag per summary category, named `topic/<tag>`, pointing at `HEAD`. Git must already be on `PATH`.

```powershell
.\scripts\tag_entries.cmd -Source git -ApplyGitTags
```

Add `-Force` to move an existing `topic/...` tag. Review them with `git tag -l "topic/*"`. Delete one with `git tag -d topic/ui`.
