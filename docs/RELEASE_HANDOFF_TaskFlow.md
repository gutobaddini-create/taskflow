# TaskFlow Release Handoff

Date: 2026-06-07

## Local MVP Status

TaskFlow is ready as a local-first Android MVP. The app builds, tests, installs, and runs on the Android emulator with Room/DataStore persistence, local reminders, local notifications, materials, sharing/invites, comments, history, and prepared Firebase boundaries.

Firebase Android configuration is present. Firebase product activation, production signing, and physical-device acceptance remain external follow-up items.

GitHub repository:

- `https://github.com/gutobaddini-create/taskflow`
- Published branch: `master`
- GitHub CLI authentication: ready for account `gutobaddini-create`

## Verified Command

```powershell
npm run verify:local-mvp
```

Latest local MVP verified run: 2026-06-06 23:13 UTC.

Latest release signing fallback check: 2026-06-07 12:12 UTC.

Covered checks:

- `:app:testDebugUnitTest`
- `:app:compileDebugKotlin`
- `:app:assembleDebug`
- `:app:compileDebugAndroidTestKotlin`
- `:app:assembleRelease`
- `:app:bundleRelease`
- Firebase emulator security rules
- release artifact existence checks
- SHA-256 release manifest generation

## Release Artifacts

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | 23,105,158 bytes | `30f28563872826e48b44a2bdfb4895ef2b43f7e0b270311b6816b048d2be7dfc` |
| `app/build/outputs/apk/release/app-release.apk` | 16,004,844 bytes | `e2b77d55e30df8a7f3e3dfd6f988f5092be48fca30978741ae42d9cc902b0303` |
| `app/build/outputs/bundle/release/app-release.aab` | 15,534,629 bytes | `0224b8ffce567e32fb8742a0ffc52211decf9b030ebc8ebae25f1965264eeb95` |

Canonical artifact manifest:

- `docs/qa/release-manifest.json`

## Emulator Evidence

Primary QA report:

- `docs/QA_TaskFlow.md`

Completion audit:

- `docs/COMPLETION_AUDIT_TaskFlow.md`

Screenshots:

- `docs/qa/screenshots/release-onboarding-emulator-2026-06-06.png`
- `docs/qa/screenshots/release-home-emulator-2026-06-06.png`
- `docs/qa/screenshots/visual-home-filtered-release-2026-06-06.png`
- `docs/qa/screenshots/visual-new-task-release-2026-06-06.png`
- `docs/qa/screenshots/visual-detail-release-2026-06-06.png`
- `docs/qa/screenshots/visual-reminder-release-2026-06-06.png`
- `docs/qa/screenshots/visual-materials-release-2026-06-06.png`
- `docs/qa/screenshots/visual-share-release-2026-06-06.png`

## Release Notes

- Package: `com.taskflow`
- Version: `0.1.0`
- Release mode: local-first MVP
- Firebase project: `gen-lang-client-0780081219`
- Firebase Android App ID: `1:209004797664:android:ef4fc149b5b033f782ba85`
- Firebase Auth e-mail/senha: validated through `npm run verify:firebase-real`
- Firebase SDK path: Auth, Firestore, Storage, Messaging, FCM token registration, and pending-operation sync are implemented behind the local-first app.
- Release signing: local development signing through the Android debug keystore, suitable for validation only.
- Production distribution requires a dedicated production keystore.
- Production signing is wired through `TASKFLOW_RELEASE_STORE_FILE`, `TASKFLOW_RELEASE_STORE_PASSWORD`, `TASKFLOW_RELEASE_KEY_ALIAS`, and `TASKFLOW_RELEASE_KEY_PASSWORD`; without those variables the build keeps using local QA signing.
- Android automatic backup is disabled for privacy of local tasks, invites, preferences, and metadata.

## External Inputs Still Required

- Firebase console activation: billing for Firestore, Storage initialization, and runtime FCM/device validation.
- Unlocked physical Android device with USB debugging authorized for final physical-device QA.
- Production keystore and signing passwords for public distribution.

## Recommended Next Step

Enable Firebase billing/Firestore, initialize Storage, enable Auth email/password, then provide an unlocked physical device and production signing credentials when public distribution is required. After each input is available, rerun the relevant verification and update `docs/ROADMAP_TaskFlow.md` only after evidence is captured.
