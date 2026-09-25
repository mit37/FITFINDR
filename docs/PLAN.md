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
      JUnit5 unit tests with golden fixtures. *(19 tests, all passing —
      verified standalone this session against a plain Kotlin/JVM project
      built from the same source files; not yet verified through
      `./gradlew test` on `:app` itself, see "Cloud-instance constraints")*
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

The Android Gradle Plugin itself requires resolving `com.android.application`
from Google's Maven repo to even configure `:app`. In this container that
repo is unreachable: `curl https://dl.google.com/...` returns
`CONNECT tunnel failed, response 403` through the sandboxed proxy (Maven
Central, by contrast, is reachable: `curl https://repo.maven.apache.org/`
returns `200`). Concretely, `./gradlew help` in this repo fails with:

```
Plugin [id: 'com.android.application', version: '8.6.1', apply: false] was not found in any of the following sources:
...
  could not resolve plugin artifact 'com.android.application:com.android.application.gradle.plugin:8.6.1'
```

So `./gradlew test`, `./gradlew ktlintCheck`, `./gradlew detekt`, and
`./gradlew assembleDebug` on the real `:app` module were **not** runnable
here and their output cannot be honestly reported as passing. This is a
different (network) constraint than "no SDK/emulator," but has the same
practical effect in this container.

To get real verification anyway, `ResultParser.kt`, `ColorExtractor.kt`,
`OutfitResult.kt`, and the two test files (`ResultParserTest.kt`,
`ColorExtractorTest.kt`) plus their fixtures — all pure Kotlin/JVM, zero
Android imports (confirmed by inspection: no `android.*` import in any of
the three domain files) — were copied verbatim into a standalone
`kotlin("jvm")` + `kotlin("plugin.serialization")` Gradle project (built
against Maven Central only, which is reachable) and run for real:

```
$ /opt/gradle/bin/gradle test --console=plain
...
ColorExtractorTest > two well-separated clusters of equal size are found with roughly equal share PASSED
ColorExtractorTest > extraction is deterministic for a fixed seed across repeated runs PASSED
ColorExtractorTest > empty input returns an empty list PASSED
ColorExtractorTest > toHex formats a packed RGB int as an uppercase hex string PASSED
ColorExtractorTest > three well-separated clusters are all recovered PASSED
ColorExtractorTest > a single pixel input returns one cluster with 100% share PASSED
ColorExtractorTest > requesting more clusters than distinct colors never invents empty clusters PASSED
ColorExtractorTest > an unbalanced two-cluster input reports the majority cluster first, by share PASSED
ColorExtractorTest > a single repeated color returns exactly one cluster covering 100% of pixels PASSED
ColorExtractorTest > k must be positive PASSED
ResultParserTest > an empty string fails to parse PASSED
ResultParserTest > markdown code fences and surrounding prose are stripped and repaired PASSED
ResultParserTest > missing or blank fields default to the literal string 'unknown' PASSED
ResultParserTest > single-quoted JSON is repaired to double-quoted and parsed PASSED
ResultParserTest > valid JSON parses on the strict (first) attempt PASSED
ResultParserTest > unknown extra keys in the model output are ignored, not fatal PASSED
ResultParserTest > garments and palette default to empty lists when absent, not unknown PASSED
ResultParserTest > trailing commas are repaired and parsed PASSED
ResultParserTest > text with no JSON object at all fails to parse, even after repair PASSED

BUILD SUCCESSFUL in 2s
```

All 19 tests, exercising the exact source files that live under
`app/src/main/java/.../domain/` and `app/src/test/java/.../domain/` in this
repo, passed. This is real verification of the *logic* those files contain.
It is **not** the same as a green `./gradlew test` run through the Android
Gradle Plugin on `:app` itself (which also runs ktlint/detekt static
analysis over these files, applies the app module's Kotlin compiler
options, and would catch e.g. an Android-side wiring mistake) — that
remains unverified locally in this session and is expected to run in CI,
where both the Android SDK and Google's Maven repo should be reachable.

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
- `curl https://dl.google.com/...` → `CONNECT tunnel failed, response 403`
  through this container's sandboxed network proxy — Google's Maven repo,
  which the Android Gradle Plugin itself needs to resolve, is not reachable
  here. (Maven Central *is* reachable: `curl https://repo.maven.apache.org/`
  → `200`.) So even `./gradlew help` fails in this repo, before any
  Android-specific task runs — see "What was actually verified this
  session" under Milestone 3 for the exact failure and the workaround used.
- Conclusion: nothing that requires the Android Gradle Plugin (`assembleDebug`,
  `ktlintCheck`, `detekt`, `./gradlew test` on `:app`, instrumented tests,
  Robolectric tests) could be run in this session. GitHub Actions CI is
  expected to have both the SDK and reachable Google/Maven repos and should
  be able to run these — that is noted as an expectation to confirm, not a
  claim of success.

## GitHub repo settings to apply manually (cannot be done via git)

- Topics: `android`, `kotlin`, `jetpack-compose`, `on-device-ai`,
  `vision-language-model`, `mediapipe`, `litert`, `fashion`.
- Default branch stays `main`; this work happened on
  `claude/new-session-cz1tti` and was not merged to `main` by this session.
