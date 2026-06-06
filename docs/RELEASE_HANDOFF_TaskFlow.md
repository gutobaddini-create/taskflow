# TaskFlow Release Handoff

Date: 2026-06-06

## Local MVP Status

TaskFlow is ready as a local-first Android MVP. The app builds, tests, installs, and runs on the Android emulator with Room/DataStore persistence, local reminders, local notifications, materials, sharing/invites, comments, history, and prepared Firebase boundaries.

Firebase real integration, GitHub publishing, and physical-device acceptance remain external follow-up items.

## Verified Command

```powershell
npm run verify:local-mvp
```

Latest verified run: 2026-06-06 23:13 UTC.

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
| `app/build/outputs/apk/debug/app-debug.apk` | 18,397,193 bytes | `a63321fb6ad1231e53636099d32da5810c4b19eb8fdbf2c5f43e6301d17d2f80` |
| `app/build/outputs/apk/release/app-release.apk` | 11,911,970 bytes | `331e5ddc8d0ff9390c7660fa6e1f885b80dae1a7854d9ec6c7c39defd7685318` |
| `app/build/outputs/bundle/release/app-release.aab` | 11,534,310 bytes | `e94cca2fdd3b2e7ef7ab8cb4748b47ec5ec0d86e90a4449b508e824707f07bbb` |

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
- Release signing: local development signing through the Android debug keystore, suitable for validation only.
- Production distribution requires a dedicated production keystore.
- Android automatic backup is disabled for privacy of local tasks, invites, preferences, and metadata.

## External Inputs Still Required

- GitHub remote URL to push commits, publish artifacts, or open a PR.
- Firebase project and `google-services.json` to enable real Auth, Firestore, Storage, and FCM.
- Unlocked physical Android device with USB debugging authorized for final physical-device QA.

## Recommended Next Step

Provide the GitHub remote first, then Firebase credentials, then an unlocked physical device. After each input is available, rerun the relevant verification and update `docs/ROADMAP_TaskFlow.md` only after evidence is captured.
