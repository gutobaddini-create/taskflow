# TaskFlow QA Report

Date: 2026-06-07

## Scope

This report records the current local MVP validation evidence for the TaskFlow Android app.

Validated target:

- Android emulator: `emulator-5554`
- App package: `com.taskflow`
- Build mode: local-first MVP with Firebase Android config, real Auth SDK path, and deployed Firestore/Storage rules

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
| `app/build/outputs/apk/debug/app-debug.apk` | 23,158,235 bytes |
| `app/build/outputs/apk/release/app-release.apk` | 16,008,936 bytes |
| `app/build/outputs/bundle/release/app-release.aab` | 15,535,420 bytes |

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

Latest run: 2026-06-07 14:40 UTC.

Result: passed. GitHub remote, GitHub CLI auth, Firebase Android config, physical Android device `RQGL203Q53K`, and production signing keystore are ready.

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

Production release signing verification:

```powershell
. C:\Users\gutol\.taskflow\release\taskflow-release-signing.env.ps1
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleRelease --console=plain
npm run release:manifest
```

Result: passed. The release build used the production keystore at `C:\Users\gutol\.taskflow\release\taskflow-release.keystore`; `apksigner verify --print-certs` confirmed the production certificate.

Real Firebase project checks:

```powershell
npm run verify:firebase-real
npx firebase deploy --only firestore:rules --dry-run --project gen-lang-client-0780081219
npx firebase deploy --only storage --dry-run --project gen-lang-client-0780081219
npx firebase deploy --only firestore:rules,storage --project gen-lang-client-0780081219
```

Result: passed. Firebase CLI auth, Android config, Auth e-mail/senha, Firestore rules dry-run, and Storage rules dry-run are ready; `npm run verify:firebase-real` created, signed in, and deleted a temporary smoke user successfully, wrote/cleaned Firestore user/space/task/pending operation/attachment metadata/FCM token documents, and uploaded/deleted a Storage smoke object. `npx firebase deploy --only firestore:rules,storage --project gen-lang-client-0780081219` released `firebase/firestore.rules` to Cloud Firestore and `firebase/storage.rules` to Firebase Storage.

Real Firebase Android SDK runtime check on physical device:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --console=plain
$adb='C:\TaskFlowAndroidSdk\platform-tools\adb.exe'
& $adb -s RQGL203Q53K install -r app\build\outputs\apk\debug\app-debug.apk
& $adb -s RQGL203Q53K install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb -s RQGL203Q53K shell am instrument -w -r -e class com.taskflow.FirebaseRealInstrumentedTest com.taskflow.test/androidx.test.runner.AndroidJUnitRunner
```

Result: passed on physical device `RQGL203Q53K` (`SM-S948B`). The instrumented test used the Android Firebase SDK path to create a temporary Auth user, write Firestore user/space/task/pending operation documents, upload a `text/plain` attachment to Storage with metadata, save an FCM token document under `users/{uid}/fcmTokens`, verify all documents/metadata, then clean up.

Production signing verification:

```powershell
. C:\Users\gutol\.taskflow\release\taskflow-release-signing.env.ps1
.\gradlew.bat --no-daemon --max-workers=1 :app:assembleRelease :app:bundleRelease --console=plain
npm run release:manifest
apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
```

Result: passed. Release APK signer certificate SHA-1 is `A4:20:F2:7E:03:B0:BD:B6:A4:8C:9F:DA:5B:73:D5:35:39:2D:C7:AB`; SHA-256 is `9B:EF:1A:82:0F:6E:5D:71:86:CF:B5:3B:70:04:E8:BB:D5:12:6D:80:50:96:88:9B:A2:C1:A7:DC:E5:6E:B5:10`.

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

## Physical Device QA

Commands executed:

```powershell
$adb='C:\TaskFlowAndroidSdk\platform-tools\adb.exe'
& $adb -s RQGL203Q53K uninstall com.taskflow
& $adb -s RQGL203Q53K install app\build\outputs\apk\release\app-release.apk
& $adb -s RQGL203Q53K shell monkey -p com.taskflow -c android.intent.category.LAUNCHER 1
& $adb -s RQGL203Q53K exec-out screencap -p > docs\qa\screenshots\physical-production-signed-home-after-start-2026-06-07.png
& $adb -s RQGL203Q53K exec-out uiautomator dump /dev/tty > docs\qa\physical-production-signed-home-after-start-2026-06-07.xml
& $adb -s RQGL203Q53K logcat -b crash -d
```

Result: passed.

Observed evidence:

- Physical device: `RQGL203Q53K`, model `SM-S948B`.
- Production-signed release APK installed successfully after removing the prior development-signed package.
- App launched and process `com.taskflow` remained alive.
- Notification permission dialog appeared and was accepted.
- Home screen rendered after onboarding start; UI dump confirmed labels including `Bom dia, Manuel`, `Hoje`, `Buscar tarefas`, `Nova tarefa`, and bottom navigation.
- Crash buffer did not show a TaskFlow crash after launch/navigation.

Captured screenshots:

- `docs/qa/screenshots/physical-production-signed-release-2026-06-07.png`
- `docs/qa/screenshots/physical-production-signed-home-2026-06-07.png`
- `docs/qa/screenshots/physical-production-signed-home-after-start-2026-06-07.png`

## Current External Blockers

No external blockers remain for the requested Firebase setup, physical QA, and production-signed artifact generation. Play Store publication still requires access to a Google Play developer account/listing, which is outside this repository.

## Status

The local-first Android MVP remains buildable, testable, installable on emulator and physical device, Firebase-validated against the real project, production-signed locally, and published to GitHub.
