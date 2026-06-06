# TaskFlow QA Report

Date: 2026-06-06

## Scope

This report records the current local MVP validation evidence for the TaskFlow Android app.

Validated target:

- Android emulator: `emulator-5554`
- App package: `com.taskflow`
- Build mode: local-first MVP, without real Firebase credentials

## Automated Verification

Command executed:

```powershell
npm run verify:local-mvp
```

Result: passed.

Latest run: 2026-06-06 23:06 UTC.

Covered by the command:

- `:app:testDebugUnitTest`
- `:app:compileDebugKotlin`
- `:app:assembleDebug`
- `:app:compileDebugAndroidTestKotlin`
- `:app:assembleRelease`
- `:app:bundleRelease`
- Firebase emulator security rules
- artifact existence checks
- release manifest generation with SHA-256 checksums

The release manifest writer was also executed successfully with both Windows PowerShell and PowerShell Core (`pwsh`) to cover the local Windows workflow and the Ubuntu GitHub Actions workflow.

Artifacts verified:

| Artifact | Size |
| --- | ---: |
| `app/build/outputs/apk/debug/app-debug.apk` | 18,397,193 bytes |
| `app/build/outputs/apk/release/app-release.apk` | 11,911,970 bytes |
| `app/build/outputs/bundle/release/app-release.aab` | 11,534,305 bytes |

Release integrity manifest:

- `docs/qa/release-manifest.json`

PowerShell Core direct verification without Firebase rules is available through:

```powershell
pwsh -NoProfile -File tools/qa/verify-local-mvp.ps1 -SkipFirebaseRules
```

The GitHub Actions workflow now runs the same full verification path and uploads the APK/AAB artifacts.

## Emulator Smoke QA

Commands executed:

```powershell
$adb='C:\TaskFlowAndroidSdk\platform-tools\adb.exe'
& $adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
& $adb -s emulator-5554 shell pm grant com.taskflow android.permission.POST_NOTIFICATIONS
& $adb -s emulator-5554 logcat -c
& $adb -s emulator-5554 shell monkey -p com.taskflow -c android.intent.category.LAUNCHER 1
```

Result: passed.

Observed evidence:

- APK installed successfully on `emulator-5554`.
- Launcher opened `com.taskflow/.MainActivity`.
- App process remained alive after launch.
- Crash buffer was empty after onboarding/home navigation.
- Onboarding/local account screen rendered.
- Home screen rendered after tapping `Comecar`.

Captured screenshots:

- `docs/qa/screenshots/splash-cold-start-emulator-2026-06-06.png`
- `docs/qa/screenshots/home-emulator-ready-2026-06-06.png`
- `docs/qa/screenshots/home-after-login-emulator-2026-06-06.png`
- `docs/qa/screenshots/release-onboarding-emulator-2026-06-06.png`
- `docs/qa/screenshots/release-home-emulator-2026-06-06.png`

Observation:

- The debug cold start on the emulator took about 20 seconds before the first Compose screen was displayed. No crash or ANR was observed. This should be rechecked on release build and physical hardware before public distribution.

## Release Emulator QA

Commands executed:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleRelease :app:bundleRelease --console=plain
$adb='C:\TaskFlowAndroidSdk\platform-tools\adb.exe'
& $adb -s emulator-5554 install -r app\build\outputs\apk\release\app-release.apk
& $adb -s emulator-5554 shell monkey -p com.taskflow -c android.intent.category.LAUNCHER 1
```

Result: passed.

Observed evidence:

- Release APK installed successfully on `emulator-5554`.
- Release build opened `com.taskflow/.MainActivity`.
- Local onboarding/account screen rendered.
- Home screen rendered after tapping `Comecar`.
- App process remained alive after navigation.
- Crash buffer was empty before and after onboarding/home navigation.

## Release Visual QA

Commands executed:

```powershell
$adb='C:\TaskFlowAndroidSdk\platform-tools\adb.exe'
& $adb -s emulator-5554 exec-out screencap -p > docs\qa\screenshots\<screen>.png
& $adb -s emulator-5554 exec-out uiautomator dump /dev/tty > docs\qa\<screen>.xml
```

Result: passed.

Observed evidence:

- Release build navigated through Home, new task, task detail, custom reminder, materials, and sharing/invite flows.
- UI hierarchy dumps confirmed primary labels and controls for each captured screen.
- The visual set matches the primary mockup families for Home, Nova tarefa, Lembrete personalizado, Materiais, Detalhe da tarefa, and Compartilhar.
- App process remained alive after the visual navigation pass.
- Recent logcat sample after the pass did not show a TaskFlow crash or ANR.

Captured screenshots:

- `docs/qa/screenshots/visual-home-filtered-release-2026-06-06.png`
- `docs/qa/screenshots/visual-new-task-release-2026-06-06.png`
- `docs/qa/screenshots/visual-new-task-filled-release-2026-06-06.png`
- `docs/qa/screenshots/visual-home-created-task-release-2026-06-06.png`
- `docs/qa/screenshots/visual-detail-release-2026-06-06.png`
- `docs/qa/screenshots/visual-reminder-release-2026-06-06.png`
- `docs/qa/screenshots/visual-materials-release-2026-06-06.png`
- `docs/qa/screenshots/visual-share-release-2026-06-06.png`
- `docs/qa/screenshots/visual-detail-actions-release-2026-06-06.png`

## Current External Blockers

These items cannot be completed from the local workspace alone:

- GitHub publishing/PR: no remote is configured in `git remote -v`.
- Firebase real integration: no Firebase project credentials or `google-services.json` are present.
- Physical-device QA: no unlocked physical Android device is available for final release acceptance.

## Status

The local-first Android MVP remains buildable, testable, installable on emulator, and ready for the next external integration step once GitHub/Firebase/device inputs are available.
