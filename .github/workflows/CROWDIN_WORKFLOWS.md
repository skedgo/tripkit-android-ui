# Crowdin Integration Workflows

This directory contains workflows for managing translations with Crowdin.

## Workflows

### 1. `crowdin_upload.yml` - Upload Source Strings
**Purpose**: Automatically uploads source strings (`strings.xml`) to Crowdin when changes are pushed.

**Triggers**:
- ✅ **Automatic**: When `TripKitAndroidUI/src/main/res/values/strings.xml` is pushed to `develop`, `main`, or `master`
- ✅ **Manual**: Can be triggered manually via GitHub Actions UI

**What it does**:
- Uploads the source `strings.xml` file to Crowdin
- Translators can then work on the new/updated strings

### 2. `crowdin_download.yml` - Download Translations
**Purpose**: Downloads completed translations from Crowdin and creates a PR.

**Triggers**:
- ✅ **Manual only**: Triggered manually via GitHub Actions UI when you need updated translations

**What it does**:
- Downloads all translations from Crowdin
- Creates a Pull Request with the updated translation files
- PR is created against the `develop` branch

## Workflow

```
Developer fixes typo/adds string
         ↓
Merge PR to develop branch
         ↓
crowdin_upload.yml runs automatically
         ↓
Source uploaded to Crowdin
         ↓
Translators work on translations
         ↓
crowdin_download.yml runs (manual trigger)
         ↓
PR created with updated translations
         ↓
Review & merge PR
```

## Benefits of New Workflow

1. **Automatic sync**: Source changes automatically upload to Crowdin (after merge to develop)
2. **Manual control**: Translations are pulled on-demand when needed
3. **Separation of concerns**: Upload and download are separate
4. **Better PR descriptions**: More informative PR messages
5. **Path-based triggers**: Only runs when relevant files change
6. **No noise**: Only uploads after merge to avoid WIP strings

## Manual Operations

If you need to manually sync:

1. **Upload sources**: Go to Actions → "Crowdin Upload Sources" → Run workflow
2. **Download translations**: Go to Actions → "Crowdin Download Translations" → Run workflow

## Configuration

Required secrets (set in GitHub repository settings):
- `CROWDIN_PROJECT_ID`
- `CROWDIN_PERSONAL_TOKEN`

Configuration file: `crowdin.yml` in the repository root.

