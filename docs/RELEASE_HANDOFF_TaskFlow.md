# TaskFlow Release Handoff

Date: 2026-06-06

## Local MVP Status

TaskFlow is ready as a local-first Android MVP. The app builds, tests, installs, and runs on the Android emulator with Room/DataStore persistence, local reminders, local notifications, materials, sharing/invites, comments, history, and prepared Firebase boundaries.

Firebase real integration, GitHub CLI/PR automation, production signing, and physical-device acceptance remain external follow-up items.

GitHub repository:

- `https://github.com/gutobaddini-create/taskflow`
- Published branch: `master`

## Verified Command

```powershell
npm run verify:local-mvp
```

Latest local MVP verified run: 2026-06-06 23:13 UTC.

Latest release signing fallback check: 2026-06-06 23:32 UTC.

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
| `app/build/outputs/apk/release/app-release.apk` | 11,911,970 bytes | `8cd18bda8be744c936f627a4424a034791a1068a9f954f481eb957b6bbd87d9f` |
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
- Production signing is wired through `TASKFLOW_RELEASE_STORE_FILE`, `TASKFLOW_RELEASE_STORE_PASSWORD`, `TASKFLOW_RELEASE_KEY_ALIAS`, and `TASKFLOW_RELEASE_KEY_PASSWORD`; without those variables the build keeps using local QA signing.
- Android automatic backup is disabled for privacy of local tasks, invites, preferences, and metadata.

## External Inputs Still Required

- GitHub CLI authentication if a PR, release, or further GitHub automation is required.
- Firebase project and `google-services.json` to enable real Auth, Firestore, Storage, and FCM.
- Unlocked physical Android device with USB debugging authorized for final physical-device QA.
- Production keystore and signing passwords for public distribution.

## Recommended Next Step

Provide Firebase credentials, then an unlocked physical device, then production signing credentials when public distribution is required. After each input is available, rerun the relevant verification and update `docs/ROADMAP_TaskFlow.md` only after evidence is captured.
