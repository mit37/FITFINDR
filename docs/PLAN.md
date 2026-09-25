# PLAN.md — build plan and status

This file tracks real status against the 8-milestone plan in `PRD.md`. It is
updated every session. Nothing here is marked done unless it is actually
done and, where applicable, verified with a command whose output is quoted
below.

## Definition of done (restated from PRD.md)

- [ ] App builds (`assembleDebug`) and passes CI (lint, detekt, gitleaks,
      unit tests) on GitHub Actions.
- [ ] Capture → on-device inference → structured result works end-to-end on
      a real device with the real `MediaPipeVlmEngine`.
- [x] `ResultParser` and `ColorExtractor` have extensive, real, passing
      JUnit5 unit tests with golden fixtures. *(see Milestone 3 below —
      written and verified standalone this session; not yet verified through
      the Android Gradle Plugin test task, see "Cloud-instance constraints")*
- [ ] Model download is resumable, checksum-verified, and deletable from
      Settings.
- [ ] Settings screen shows the active inference accelerator (CPU/GPU/NPU)
      and allows switching delegate preference.
- [ ] Outfit history persists locally (Room) and can be shared as an image.
- [ ] An instrumented eval harness produces real latency/memory/parse-success
      numbers on a real device, recorded in the README.
- [ ] A signed release APK is attached to a GitHub Release, README has real
      screenshots, `docs/DEMO.md` documents how to record a demo, and the
      repo is tagged `v2.0.0`.
- [ ] Every number in the README is reproducible from a script in this repo,
      or explicitly marked "not measured" with a reason.
- [x] No secrets, model weights, or datasets are committed.

## This session's scope: Milestones 1–3

**Milestone 1 — Scaffold + CI**: DONE.
- Gradle Kotlin DSL project with a version catalog (`gradle/libs.versions.toml`),
  single `app` module, package structure matching the architecture diagram
  (`ui/`, `domain/`, `data/inference/`, `data/model/`, `data/db/`, `di/`).
- `.github/workflows/ci.yml` written (lint → detekt → unit test →
  assembleDebug → gitleaks). **Not yet observed green** — this container has
  no Android SDK to run the Android-dependent steps locally. GitHub Actions
  runners provision the Android SDK themselves (via
  `android-actions/setup-android`), so `assembleDebug` is expected to work
  there; this is an expectation, not a verified fact, until a real CI run on
  GitHub is observed after this branch is pushed.
- Gradle wrapper generated with the local Gradle 8.14.3 install
  (`/opt/gradle/bin/gradle wrapper`).

**Milestone 2 — Capture/gallery + result UI against FakeVlmEngine**: PARTIAL,
honestly.
- `ui/CaptureScreen.kt`: structured with a CameraX preview + Android Photo
  Picker gallery entry point. The CameraX preview binding is present but
  **not exercised** — there is no device/emulator in this container to run
  it on, so it should be treated as unverified scaffolding, not a tested
  feature.
- `ui/ResultScreen.kt`: renders an `OutfitResult` (garments, palette,
  search queries) from `FakeVlmEngine`'s fixture — this is real, structured
  Compose UI code, but its correctness has **not** been verified by running
  the app or a Compose UI test (no Android SDK here). It has been read
  through for compile-correctness by eye only.
- `ui/HistoryScreen.kt` and `ui/SettingsScreen.kt` are **placeholders only**
  (a single "coming in milestone 6 / milestone 5" message each) — they are
  not real implementations. Do not count them as done.
- `MainActivity.kt` + Navigation Compose graph wiring all four screens:
  written, not run.
- Hilt module binds `VlmEngine` → `FakeVlmEngine` (the real
  `MediaPipeVlmEngine` is milestone 5 and does not exist yet, only the
  interface).

**Milestone 3 — ResultParser + ColorExtractor with unit tests**: DONE, and
the part that could be verified without Android was actually verified.
- `domain/ResultParser.kt`: strict JSON parse via `kotlinx.serialization`,
  plus one repair pass (strips ```json fences, trailing commas, converts
  single-quoted string values to double-quoted) before a second strict
  parse attempt. Missing/unparseable fields default to `"unknown"` per the
  PRD's prompt contract.
- `domain/ColorExtractor.kt`: pure Kotlin k-means over `IntArray` RGB pixel
  values, no Android import anywhere in the file (verified by grep — see
  below). Deterministic given a fixed seed.
- Golden fixtures: `app/src/test/resources/fixtures/*.json` (valid output,
  trailing-comma, markdown-fenced, single-quoted, missing-fields).
- JUnit5 tests for both in
  `app/src/test/java/com/mitanshm/fitfindr/domain/`.

### What was actually verified this session, and how

The Android Gradle Plugin itself requires the Android SDK to configure a
build (even to run a plain `./gradlew test` on `:app`), and this container
has no SDK (`which sdkmanager adb` → nothing found; `$ANDROID_HOME` and
`$ANDROID_SDK_ROOT` are both unset). So `./gradlew test` was **not** runnable
here and its output cannot be honestly reported.

To get real verification anyway, the core logic of `ResultParser` and
`ColorExtractor` (which have zero Android dependencies) was copied into a
standalone Kotlin script/module and compiled and executed directly with
`kotlinc`/`kotlin` outside Gradle, exercising the same algorithms against
the same fixture data. See the session report for exact commands and their
real output. This is real verification of the *logic*, but it is not the
same as a green `./gradlew test` run through the Android Gradle Plugin — that
remains unverified locally and is expected to run in CI.

## Deferred to future sessions (Milestones 4–8) — NOT started

- Milestone 4: `ModelDownloader` (WorkManager, resumable, SHA-256 verify,
  storage checks, delete). Not started.
- Milestone 5: `MediaPipeVlmEngine` real integration, delegate
  selection/fallback, Settings screen showing active accelerator. Not
  started — only the `VlmEngine` interface and `FakeVlmEngine` exist.
- Milestone 6: Room-backed History screen (real), share-as-image. Not
  started — `HistoryScreen.kt` is a placeholder.
- Milestone 7: Instrumented eval harness + README results. Not started.
- Milestone 8: Signed release APK, README screenshots, `docs/DEMO.md`
  demo script, tag `v2.0.0`. `docs/DEMO.md` is written this session as a
  script for Mitansh to record a demo of what exists so far (Compose UI
  against the fake engine only) — it does not imply a release exists.

## Cloud-instance constraints (confirmed this session)

- `which sdkmanager` → not found.
- `which adb` → not found.
- `$ANDROID_HOME` → empty.
- `$ANDROID_SDK_ROOT` → empty.
- No emulator, no physical device, no NPU/GPU delegate available.
- Conclusion: nothing that requires the Android Gradle Plugin's SDK-aware
  tasks (`assembleDebug`, instrumented tests, Robolectric tests that need
  the Android jar) could be run in this session. GitHub Actions CI is
  expected to have the SDK and should be able to run these — that is noted
  as an expectation to confirm, not a claim of success.

## GitHub repo settings to apply manually (cannot be done via git)

- Topics: `android`, `kotlin`, `jetpack-compose`, `on-device-ai`,
  `vision-language-model`, `mediapipe`, `litert`, `fashion`.
- Default branch stays `main`; this work happened on
  `claude/new-session-cz1tti` and was not merged to `main` by this session.
