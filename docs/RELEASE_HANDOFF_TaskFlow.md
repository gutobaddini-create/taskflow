# TaskFlow Release Handoff

Date: 2026-06-07

## Local MVP Status

TaskFlow is ready as a local-first Android MVP. The app builds, tests, installs, and runs on the Android emulator and physical device with Room/DataStore persistence, local reminders, local notifications, materials, sharing/invites, comments, history, and real Firebase boundaries.

Firebase Android configuration is present. Firebase Auth, Firestore, Storage, FCM token registration, production signing, and physical-device acceptance are validated.

GitHub repository:

- `https://github.com/gutobaddini-create/taskflow`
- Published branch: `master`
- Release: `https://github.com/gutobaddini-create/taskflow/releases/tag/v0.1.1`
- GitHub CLI authentication: ready for account `gutobaddini-create`

## Verified Command

```powershell
npm run verify:local-mvp
```

Latest local MVP verified run: 2026-06-06 23:13 UTC.

Latest production signing check: 2026-06-07 14:40 UTC.

Covered checks:

- `:app:testDebugUnitTest`
- `:app:compileDebugKotlin`
- `:app:assembleDebug`
- `:app:compileDebugAndroidTestKotlin`
- `:app:assembleRelease`
- `:app:bundleRelease`
- Firebase emulator security rules
- real Firebase smoke verification
- physical-device Firebase SDK instrumented test
- release artifact existence checks
- SHA-256 release manifest generation

## Release Artifacts

| Artifact | Size | SHA-256 |
| --- | ---: | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | 23,246,182 bytes | `da1fb730517f8738fac2920849879f46d4c65b8e5afa278b3ddb3997fcea4b29` |
| `app/build/outputs/apk/release/app-release.apk` | 16,041,840 bytes | `69e6d9d46bde65a52911ad61a9c43c5161fc22b02b448f60d414f035432adb03` |
| `app/build/outputs/bundle/release/app-release.aab` | 15,573,911 bytes | `65d410333d671dad2d71fa5f82f8d6941c58e64d4acb31e78d3c0057fabafda5` |

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
- `docs/qa/screenshots/v012-release-home-clean-2026-06-07.png`
- `docs/qa/screenshots/v012-release-new-task-draft-2026-06-07.png`
- `docs/qa/screenshots/v012-release-new-task-draft-lower-2026-06-07.png`
- `docs/qa/screenshots/v012-release-https-invite-2026-06-07.png`

## Physical Evidence

- Device: `RQGL203Q53K`, model `SM-S948B`
- `FirebaseRealInstrumentedTest`: passed on physical device using the Android Firebase SDK path.
- Production-signed release APK installed and launched on the physical device.
- Screenshots:
  - `docs/qa/screenshots/physical-production-signed-release-2026-06-07.png`
  - `docs/qa/screenshots/physical-production-signed-home-2026-06-07.png`
  - `docs/qa/screenshots/physical-production-signed-home-after-start-2026-06-07.png`

## Release Notes

- Package: `com.taskflow`
- Version: `0.1.2`
- Release mode: local-first MVP
- Firebase project: `gen-lang-client-0780081219`
- Firebase Android App ID: `1:209004797664:android:ef4fc149b5b033f782ba85`
- Firebase Auth e-mail/senha: validated through `npm run verify:firebase-real`
- Firebase Firestore/Storage rules: validated through `npm run verify:firebase-real`.
- Firebase Firestore/Hosting: invite link rules and fallback web page deployed with `npx firebase deploy --only firestore:rules,hosting --project gen-lang-client-0780081219`.
- Invite URL format: `https://gen-lang-client-0780081219.web.app/invite/{token}` with legacy `taskflow://invite/{token}` parser fallback.
- Firebase runtime smoke: Auth, Firestore pending operation, attachment metadata, Storage upload/delete, and FCM token document validated through REST smoke and physical Android instrumented test.
- Release signing: production keystore at `C:\Users\gutol\.taskflow\release\taskflow-release.keystore`.
- Production signing env file: `C:\Users\gutol\.taskflow\release\taskflow-release-signing.env.ps1`.
- Release APK signer SHA-1: `A4:20:F2:7E:03:B0:BD:B6:A4:8C:9F:DA:5B:73:D5:35:39:2D:C7:AB`.
- Release APK signer SHA-256: `9B:EF:1A:82:0F:6E:5D:71:86:CF:B5:3B:70:04:E8:BB:D5:12:6D:80:50:96:88:9B:A2:C1:A7:DC:E5:6E:B5:10`.
- Android automatic backup is disabled for privacy of local tasks, invites, preferences, and metadata.

## External Inputs Still Required

- Physical-device rerun for `0.1.2` requires the USB device to appear in `adb devices -l`; only `emulator-5554` was visible during the final pass.
- Google Play publication requires access to a Google Play developer account/listing.

## Recommended Next Step

Create a Google Play internal testing release with `app/build/outputs/bundle/release/app-release.aab`, or publish the generated APK/AAB as a GitHub release artifact.
