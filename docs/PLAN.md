# PLAN.md — build plan and status

This file tracks real status against the 8-milestone plan in `PRD.md`. It is
updated every session. Nothing here is marked done unless it is actually
done and, where applicable, verified with a command whose output is quoted
below.

## Definition of done (restated from PRD.md)

- [ ] App builds (`assembleDebug`) and passes CI (lint, detekt, gitleaks,
      unit tests) on GitHub Actions. *(Workflow written; not yet observed
      green — no session has had a real GitHub Actions run to watch.)*
- [ ] Capture → on-device inference → structured result works end-to-end on
      a real device with the real `MediaPipeVlmEngine`. *(Not done —
      `MediaPipeVlmEngine` is written but never run, and isn't the bound
      `VlmEngine`; see Milestone 5.)*
- [x] `ResultParser` and `ColorExtractor` have extensive, real, passing
      JUnit5 unit tests with golden fixtures. *(19 tests, all passing —
      verified standalone this session against a plain Kotlin/JVM project
      built from the same source files; not yet verified through
      `./gradlew test` on `:app` itself, see "Cloud-instance constraints")*
- [x] Model download is resumable, checksum-verified, and deletable from
      Settings. *(Code done, real: WorkManager resumable download with
      HTTP Range, SHA-256 checksum verification, storage precheck, delete.
      Pure decision logic unit-tested and independently verified — see
      Milestone 4. NOT verified against a real network/download — there is
      no real model URL configured yet, `ModelConfig.DOWNLOAD_URL` is a
      placeholder, and no device exists here to run it on.)*
- [~] Settings screen shows the active inference accelerator (CPU/GPU/NPU)
      and allows switching delegate preference. *(Settings UI is real and
      shows a live download state + an "active accelerator" line — but
      that line always reads "not applicable" because `MediaPipeVlmEngine`
      is not the bound engine yet, so there is no real accelerator to
      report. Delegate preference (GPU-first, CPU-fallback) is hardcoded
      in `MediaPipeVlmEngine`, not user-switchable yet — PARTIAL, not
      done.)*
- [~] Outfit history persists locally (Room) and can be shared as an image.
      *(Code done: Room entities/DAO/database/repository, real
      `HistoryScreen` backed by it, `ShareImage` renders an `OutfitResult`
      to a PNG and shares it via `ACTION_SEND`. Domain↔entity mapping logic
      is unit-tested and independently verified. NOT verified: Room itself,
      the Compose screens, and the Android graphics/FileProvider code have
      never actually been run — see Milestone 6.)*
- [~] An instrumented eval harness produces real latency/memory/parse-success
      numbers on a real device, recorded in the README. *(Harness code and
      metrics are real, unit-tested, and self-checked against
      `FakeVlmEngine` + existing fixtures — genuinely reproducible numbers
      are in `eval/results.json`'s `harness_self_check` section and this
      README. Real device numbers are NOT measured — no labeled photos, no
      device — `eval/results.json`'s `pending_real_device_numbers` section
      is explicitly null, not guessed. See Milestone 7.)*
- [ ] A signed release APK is attached to a GitHub Release, README has real
      screenshots, `docs/DEMO.md` documents how to record a demo, and the
      repo is tagged `v2.0.0`. *(`docs/DEMO.md` exists and is current.
      `.github/workflows/release.yml` is a real, unexercised skeleton — no
      signing secrets exist, no tag has been pushed, no APK exists. Not
      tagging `v2.0.0` — see "What's left before v2.0.0" below.)*
- [x] Every number in the README is reproducible from a script in this repo,
      or explicitly marked "not measured" with a reason.
- [x] No secrets, model weights, or datasets are committed.

Legend: `[x]` done and verified, `[~]` real code exists but the criterion
is only partially met or unverified, `[ ]` not done.

## This session's scope: Milestones 1–3 (prior session)

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
- `MainActivity.kt` + Navigation Compose graph wiring all four screens:
  written, not run.
- Hilt module binds `VlmEngine` → `FakeVlmEngine` (the real
  `MediaPipeVlmEngine` did not exist yet as of this milestone; see
  Milestone 5 below).

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

### What was verified in the milestone 1-3 session

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
Android imports — were copied verbatim into a standalone `kotlin("jvm")` +
`kotlin("plugin.serialization")` Gradle project (built against Maven
Central only, which is reachable) and run for real: all 19 tests passed.
See the milestone 4-8 session's run below, which supersedes this with a
59-test run covering the same files plus everything added since.

## Milestones 4, 6, 7 — this session (2026, milestone 4-8 session)

Scope confirmed at the start of this session: re-ran `git status`/`git log`
(clean, up to date with the pushed milestone 1-3 branch) and re-tried
`./gradlew help`, which failed with the exact same
`com.android.application` plugin resolution error quoted above — this
cloud container's network and Android-SDK constraints are unchanged from
the milestone 1-3 session. All verification in this session uses the same
standalone-JVM-project workaround, extended with the new pure-Kotlin files.

**Milestone 4 — `ModelDownloader`**: DONE (code + pure logic verified;
network/device path unverified — see below).
- `data/model/ChecksumVerifier.kt`: SHA-256 over a `File`/`ByteArray`/
  `InputStream`, using `java.security.MessageDigest` (JDK, not Android —
  zero `android.*` import).
- `data/model/StoragePrecheck.kt`: pure `Long`-byte-count decision (`hasSufficientSpace`,
  `shortfallBytes`), zero Android import.
- `data/model/DownloadPlanner.kt`: pure decision logic for
  fresh-start/resume/restart-as-stale given existing partial-file bytes vs.
  expected total, zero Android import.
- `data/model/ModelConfig.kt`: model URL/checksum/size constants — **all
  placeholders**, explicitly marked as such in the file, since no real
  model is hosted anywhere this repo points to and nobody in any session
  has computed a real checksum.
- `data/model/ModelDownloadWorker.kt` (WorkManager `CoroutineWorker`,
  `@HiltWorker`) and `data/model/ModelDownloader.kt` (the public, injectable
  facade): real HTTP Range-based resume, storage precheck before writing,
  checksum verification after download, delete. **Never run** — no
  network/device here; implemented against documented `HttpURLConnection`/
  WorkManager APIs and reviewed by eye only.
- `FitFindrApplication` now implements `Configuration.Provider` with
  `HiltWorkerFactory` (required for `@HiltWorker` injection); the manifest
  disables WorkManager's default `ContentProvider` initializer accordingly.

**Milestone 6 — History (Room) + share-as-image**: DONE (code + pure
mapping logic verified; Room/UI/graphics path unverified — see below).
- `data/db/OutfitEntity.kt`, `GarmentEntity.kt` (real `@Entity`/
  `@ForeignKey`(CASCADE)/`@Index` Room annotations), `OutfitWithGarments.kt`
  (`@Relation`), `OutfitDao.kt` (`@Dao`), `FitFindrDatabase.kt`
  (`@Database`).
- `data/db/OutfitMapper.kt`: pure domain↔entity conversion functions.
  Deliberately has **zero** `androidx.room`/`android.*` import — it only
  references the entity *classes* (same package, no import statement
  needed), not their Room annotations — so it is unit-testable, and was
  independently re-verified, without Room on the classpath at all.
- `data/db/OutfitRepository.kt`: domain-typed save/observe/get/delete API
  wrapping `OutfitDao` + `OutfitMapper`. Depends on the real `OutfitDao`
  interface (hence Room), so this class itself is unverified here.
- `di/DatabaseModule.kt`: Hilt bindings for `FitFindrDatabase`/`OutfitDao`.
- `ui/HistoryScreen.kt` + `HistoryViewModel.kt`: real Room-backed list
  (expand/delete), replacing the milestone-2 placeholder.
- `ui/ShareImage.kt`: renders an `OutfitResult` to a `Bitmap` via
  `android.graphics.Canvas` and shares it through `ACTION_SEND` +
  `FileProvider`. Wired into both `ResultScreen` and `HistoryScreen`.
  AndroidManifest gets a `FileProvider` entry + `res/xml/file_paths.xml`.
- `ResultViewModel` now calls `OutfitRepository.save()` on every successful
  describe.

**Milestone 7 — Eval harness**: DONE as a harness (code + self-check
verified; real device numbers explicitly not measured).
- `eval/labels.csv`: schema/header only, no data rows — real photos and
  labels are Mitansh's to supply (~100 real photos per PRD), not
  fabricated here. `eval/README.md` documents the schema and both ways the
  harness runs.
- `data/eval... ` — actually `eval/` package under `app/src/main`:
  `EvalMetrics.kt` (garment-type precision/recall, JSON validity pre/post
  repair, color-name-vs-pixel-palette cross-check — pure, zero Android
  import), `LabelsCsv.kt` (pure CSV parser for the schema above),
  `EvalEntryPoint.kt` (Hilt `@EntryPoint` to pull the bound `VlmEngine`
  into a plain instrumented test), `EvalResultsWriter.kt` (writes a device-
  side JSON results file via `org.json.JSONObject`).
- `app/src/androidTest/.../eval/EvalHarnessInstrumentedTest.kt`: real
  instrumented test wired to read `eval/labels.csv` + photos pushed to the
  device, run them through the bound `VlmEngine`, and write results.
  **Never run** — `./gradlew connectedCheck` itself requires a connected
  device/emulator, which does not exist in this container. `-Peval`
  gradle property (in `app/build.gradle.kts`) narrows `connectedCheck` to
  just this test class.
- `app/src/test/.../eval/EvalHarnessSelfCheckTest.kt`,
  `EvalMetricsTest.kt`, `LabelsCsvTest.kt`: genuine, JVM-runnable tests
  proving the harness's own logic works, against `FakeVlmEngine` and the
  existing golden JSON fixtures. **Not a measurement of real model
  performance** — see `eval/README.md`.
- `eval/results.json`: real `harness_self_check` numbers (see below) +
  an explicitly-null `pending_real_device_numbers` section.
- `domain/ResultParser.kt` gained `parseStrictOnly()` (no repair attempt),
  needed by `EvalMetrics.jsonValidity` to measure the pre-repair rate
  separately from `parse()`'s post-repair rate. Purely additive, does not
  change `parse()`'s existing behavior or its tests.

## Milestone 5 — `MediaPipeVlmEngine` + real Settings UI (this session)

**Status: code written, explicitly and deliberately NOT verified.** This is
the single most important honesty point in this session's work.

- `data/inference/MediaPipeVlmEngine.kt`: implements `VlmEngine` against
  the documented `com.google.mediapipe:tasks-genai` `LlmInference`/
  `LlmInferenceSession`/`GraphOptions` API (version pinned in
  `gradle/libs.versions.toml`) — model-path loading from
  `ModelDownloader`'s app-private file, GPU-preferred/CPU-fallback delegate
  selection (try GPU, catch `IllegalStateException`, retry CPU), an
  `activeBackendLabel()` for the Settings screen.
- **This class has never been run.** No Android SDK, device, emulator, or
  NPU/GPU delegate exists in this container (confirmed again this session
  — same as milestone 1-3's finding), and the network host that would
  serve a real `.task` model bundle is also unreachable here. It is
  entirely plausible the exact MediaPipe method/class names or delegate
  fallback behavior have drifted from what's written by the time someone
  with real hardware verifies it — the file's own doc comment says so
  explicitly, and flags one particularly uncertain import
  (`BitmapImageBuilder`'s artifact).
- `di/InferenceModule.kt` **still binds `FakeVlmEngine`**, not
  `MediaPipeVlmEngine` — deliberately, since `FakeVlmEngine` is the only
  path any session has actually been able to reason about end-to-end. The
  module's doc comment documents the exact one-line change to switch the
  binding once someone verifies `MediaPipeVlmEngine` on real hardware.
- `ui/SettingsScreen.kt` + `SettingsViewModel.kt`: **real** Compose UI,
  wired to the real `ModelDownloader` — download/cancel/delete buttons,
  live progress bar from `ModelDownloadState`, and an "active accelerator"
  line. That line currently always reads "Not applicable — running against
  FakeVlmEngine" rather than fabricating a CPU/GPU/NPU value nobody has
  observed — see `SettingsViewModel.activeAcceleratorLabel()`'s doc
  comment.

## Milestone 8 — Release (this session, partial by design)

- `.github/workflows/release.yml`: real skeleton, triggered on `v*.*.*` tag
  push, builds + signs a release APK, uploads it to a GitHub Release.
  **Never run** — no tag has been pushed, and the four secrets it reads
  (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) do not exist in this repo's
  GitHub Secrets. Only Mitansh can add a real signing keystore — an AI
  coding agent must not create or fabricate one (STANDARDS.md rule 7).
- `app/build.gradle.kts` gets a real (currently empty) `signingConfigs`
  block, reading from the same env-var names the workflow sets. Without
  those set, `assembleRelease` produces a plain unsigned APK — normal
  AGP behavior, not a fabricated signature.
- `docs/DEMO.md` updated to describe what's honestly demoable as of
  milestones 1-7 (history, share-as-image, real Settings UI — not just
  the milestone-1-3 UI skeleton it previously described).
- **No tag, no signed APK, no GitHub Release exist.** See "What's left
  before v2.0.0" below.

### What's left before `v2.0.0` can honestly be tagged

1. **Run on a real device.** Get access to a physical Android phone (or a
   properly resourced emulator) with the Android SDK. Run
   `./gradlew assembleDebug`, `./gradlew test`, `ktlintCheck`, `detekt` for
   real on `:app` (not just the standalone-JVM workaround) and fix whatever
   the Android Gradle Plugin's real compiler/lint catches that the
   workaround couldn't.
2. **Verify `MediaPipeVlmEngine` for real.** Host a real `.task` model
   bundle somewhere reachable, fill in `ModelConfig.DOWNLOAD_URL`/
   `EXPECTED_SHA256`/`APPROX_SIZE_BYTES` with real values, run
   `ModelDownloader` against it, switch `InferenceModule`'s binding, and
   confirm `describe()` actually returns model output that
   `ResultParser` can parse. Fix whatever MediaPipe API drift milestone 5's
   honesty notes flagged.
3. **Supply real eval photos.** Mitansh photographs ~100 real outfits,
   labels them into `eval/labels.csv`, and someone runs
   `./gradlew connectedCheck -Peval` on a real device to get the
   `pending_real_device_numbers` this repo is currently missing.
4. **Add a signing keystore to GitHub Secrets** (`ANDROID_KEYSTORE_BASE64`,
   `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`)
   — only Mitansh can do this.
5. **Observe `.github/workflows/ci.yml` green on GitHub**, then observe
   `.github/workflows/release.yml` succeed on a real tag push (start with a
   throwaway pre-release tag, not `v2.0.0` itself, to prove the mechanism).
6. **Update this file and the README** with the real numbers from steps
   1-3, remove every "not measured"/"never run" note that's no longer true,
   and only then tag `v2.0.0`.

None of the above was possible in this cloud container in any session so
far — see "Cloud-instance constraints" below, unchanged since milestone 1.

## Cloud-instance constraints (re-confirmed this session, unchanged)

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
  Android-specific task runs.
- Conclusion: nothing that requires the Android Gradle Plugin (`assembleDebug`,
  `ktlintCheck`, `detekt`, `./gradlew test` on `:app`, instrumented tests,
  Robolectric tests) could be run in any session so far, including this
  one. GitHub Actions CI is expected to have both the SDK and reachable
  Google/Maven repos and should be able to run these — that is noted as an
  expectation to confirm, not a claim of success.
- Room specifically: its Maven artifacts (like all AndroidX artifacts) are
  published only to Google's Maven repo, the same one that is unreachable
  here — so even the standalone-JVM verification workaround cannot pull in
  real Room for a true DAO/database test. `OutfitMapperTest.kt`'s
  standalone-JVM run instead uses plain, unannotated mirror copies of
  `OutfitEntity`/`GarmentEntity` (documented inline in those copies) to
  verify the *mapping logic*, which is honestly a narrower claim than
  verifying Room itself.

### What was verified this session (milestone 4-8), and how

Same workaround as milestone 1-3: copied the new pure-Kotlin/JVM files
(zero `android.*`/`androidx.room.*` import) verbatim into the same
standalone `kotlin("jvm")` + `kotlin("plugin.serialization")` Gradle
project used previously (`javax.inject:javax.inject:1` and
`kotlinx-coroutines-core` added as dependencies, both resolvable from
Maven Central), alongside plain unannotated mirror copies of
`OutfitEntity`/`GarmentEntity` for `OutfitMapperTest`. Ran:

```
$ /opt/gradle/bin/gradle test --rerun --console=plain
```

Result: **59 tests, 59 passed, 0 failed** (JUnit XML: `tests="4"` ×2,
`tests="6"` ×2, `tests="7"`, `tests="9"` ×2, `tests="10"`, `tests="4"` —
`ChecksumVerifierTest` 6, `DownloadPlannerTest` 6, `StoragePrecheckTest` 7,
`ColorExtractorTest` 10, `ResultParserTest` 9, `OutfitMapperTest` 4,
`LabelsCsvTest` 4, `EvalMetricsTest` 9, `EvalHarnessSelfCheckTest` 4). This
supersedes the milestone 1-3 session's 19-test run (those 19 — `ResultParser`
+ `ColorExtractor` — are included in the 59).

Separately, a one-off instrumented run of the exact latency loop in
`EvalHarnessSelfCheckTest` (200 iterations of `FakeVlmEngine.describe()` +
`ResultParser.parse()`, printed rather than asserted, since exact
wall-clock timing shouldn't be a hard test assertion) measured
**~0.32ms/call average** in this container — recorded in
`eval/results.json`'s `harness_self_check` section with the caveat that
run-to-run JIT-warmup variance means this is representative, not exact.

This is real verification of the *logic* those files contain. It is
**not** the same as a green `./gradlew test` run through the Android
Gradle Plugin on `:app` itself (which also runs ktlint/detekt static
analysis, applies the app module's Kotlin compiler options, and would
catch e.g. an Android-side wiring mistake, a Room schema error, or a Hilt
graph problem) — that remains unverified locally in every session so far
and is expected to run in CI, where both the Android SDK and Google's
Maven repo should be reachable.

## GitHub repo settings to apply manually (cannot be done via git)

- Topics: `android`, `kotlin`, `jetpack-compose`, `on-device-ai`,
  `vision-language-model`, `mediapipe`, `litert`, `fashion`.
- Default branch stays `main`; all work across every session has happened
  on `claude/new-session-cz1tti` and was not merged to `main` by any
  session.
- Add the four release-signing secrets listed in "What's left before
  v2.0.0" above, once a real keystore exists.
