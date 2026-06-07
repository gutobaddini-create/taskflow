# TaskFlow QA Report

Date: 2026-06-07

## Scope

This report records the current local MVP validation evidence for the TaskFlow Android app.

Validated target:

- Android emulator: `emulator-5554`
- App package: `com.taskflow`
- Build mode: local-first MVP with Firebase Android config and real Auth SDK path prepared

## Automated Verification

Command executed:

```powershell
npm run verify:local-mvp
```

Result: passed.

Latest run: 2026-06-06 23:13 UTC.

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

Release privacy note:

- Android automatic backup is disabled in the manifest for the local-first MVP, avoiding implicit backup/restore of local tasks, invites, preferences, and metadata.
- The processed release manifest was checked after build and contains `android:allowBackup="false"`.

Artifacts verified:

| Artifact | Size |
| --- | ---: |
| `app/build/outputs/apk/debug/app-debug.apk` | 23,105,158 bytes |
| `app/build/outputs/apk/release/app-release.apk` | 16,004,844 bytes |
| `app/build/outputs/bundle/release/app-release.aab` | 15,534,629 bytes |

Release integrity manifest:

- `docs/qa/release-manifest.json`

PowerShell Core direct verification without Firebase rules is available through:

```powershell
pwsh -NoProfile -File tools/qa/verify-local-mvp.ps1 -SkipFirebaseRules
```

The GitHub Actions workflow now runs the same full verification path and uploads the APK/AAB artifacts.

External readiness diagnostics:

```powershell
npm run verify:external-readiness
```

Latest run: 2026-06-07 11:09 UTC.

Result: passed as an informational diagnostic. GitHub remote, GitHub CLI auth, and Firebase Android config are now ready; the diagnostic reported 2 missing external inputs: physical Android device and production signing variables.

Firebase Android configuration:

- Firebase CLI authenticated as `gutobaddini@gmail.com`.
- Active project: `gen-lang-client-0780081219`.
- Android app: `TaskFlow Android`.
- Android App ID: `1:209004797664:android:ef4fc149b5b033f782ba85`.
- Package: `com.taskflow`.
- Config file: `app/google-services.json`.
- Gradle plugin: `com.google.gms.google-services`.
- Firebase SDKs added through BoM `33.7.0`: Auth, Firestore, Storage, and Messaging.
- `FirebaseTaskFlowDataSource` now uses FirebaseAuth, FirebaseFirestore, FirebaseStorage, FirebaseMessaging, and coroutine task awaiters.
- Onboarding now attempts Firebase login/cadastro with password and falls back to local mode when Firebase is unavailable.
- `:app:compileDebugKotlin` passed with `:app:processDebugGoogleServices`.
- `:app:testDebugUnitTest` passed.
- `:app:assembleRelease` passed with `:app:processReleaseGoogleServices`.
- `:app:bundleRelease` passed with `:app:processReleaseGoogleServices`.
- `npm run test:firebase-rules` passed after clearing a stale Firestore emulator process that held port `8180`.

Release signing fallback verification:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleRelease --console=plain
npm run release:manifest
```

Result: passed. The release build still succeeds without production signing variables, using local QA signing.

Real Firebase project checks:

```powershell
npm run verify:firebase-real
npx firebase deploy --only firestore:rules --dry-run --project gen-lang-client-0780081219
npx firebase deploy --only storage --dry-run --project gen-lang-client-0780081219
```

Result: partially passed. Firebase CLI auth, Android config, and Auth e-mail/senha are ready; `npm run verify:firebase-real` created and deleted a temporary smoke user successfully. Firestore real deployment returned HTTP 403 because billing is not enabled for project `gen-lang-client-0780081219`. Storage deployment reported that Firebase Storage has not been set up and must be initialized in the Firebase Console.

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

- Firebase real product activation: Firestore requires billing and Storage requires console initialization before real rules/storage validation.
- Physical-device QA: no unlocked physical Android device is available for final release acceptance.
- Production signing: production keystore and signing variables are not configured.

## Status

The local-first Android MVP remains buildable, testable, installable on emulator, published to GitHub, and ready for the next external integration step once Firebase/device/signing inputs are available.
