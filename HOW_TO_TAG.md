# Tag a version

## Step 1

Double-click this file:

`scripts\tag_entries.cmd`

Or, in the project folder, run:

```powershell
.\scripts\tag_entries.cmd
```

## Step 2

You do not type a version. The script reads it from `pom.xml`.

Right now that version is `1.3.1-SNAPSHOT`. The script uses `1.3.1`.

## Step 3

The script does the rest:

- It creates the tag `bible-verse-extractor-1.3.1`.
- It uploads that tag.
- It changes the project version to `1.3.2-SNAPSHOT`.
- It saves that change with the message `Bump version to 1.3.2-SNAPSHOT`.

Press any key to close the window.

Save any other work before you run it. If the tag already exists, the script stops and leaves the project unchanged.
