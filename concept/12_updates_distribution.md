# Updates and Distribution

## Goal

Commercial desktop product must install and update like normal professional software.

User should not run Gradle commands, unpack zips or manually replace files.

## Packaging Strategy

Use:

- `jpackage` for Java runtime image/package foundation.
- `install4j` for professional installer, signing, shortcuts, uninstall and updates.

Optional:

- evaluate `jDeploy` for simpler distribution channels, but do not depend on it for the main commercial updater until proven.

## Installer Requirements

Installer should:

- install bundled Java runtime;
- create desktop/start menu shortcuts;
- register uninstall;
- support per-user install;
- support system install later;
- preserve user settings on update;
- support code signing.

## Update Flow

Background flow:

```text
1. App checks update metadata.
2. If update exists, app downloads in background.
3. App verifies signature/hash.
4. App shows non-blocking notification:
   "Update ready. Restart to install?"
5. User clicks restart.
6. App closes.
7. Updater installs.
8. App reopens.
```

UI states:

```text
Up to date
Checking
Downloading 42%
Update ready
Update failed
Restart required
```

## Release Channels

- stable;
- beta;
- internal/nightly.

Settings:

```text
Update channel: Stable / Beta
Check automatically: on/off
Download automatically: on/off
Language: English / Русский
```

## Versioning

Use semantic versioning:

```text
MAJOR.MINOR.PATCH
```

Examples:

```text
0.1.0-alpha
0.5.0-beta
1.0.0
1.1.0
```

## Update Safety

Before update:

- ensure no active agent task;
- warn user if task running;
- save UI state;
- flush SQLite;
- close runtime processes cleanly.

## Rollback Strategy

For serious product:

- keep previous version metadata;
- if update fails, app should remain on old version;
- never break user projects during app update.

## Signing

Production builds should be signed:

- Windows code signing certificate;
- macOS notarization later if macOS is targeted;
- signed update metadata.

## Distribution Checklist

- installer built;
- installer signed;
- app starts after clean install;
- update from previous version tested;
- uninstall tested;
- user settings preserved;
- logs available;
- crash on startup recovery path exists.
