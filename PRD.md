# FitFindr — Product Requirements Document (v2 rebuild)

## Pitch

Point your phone at an outfit — yours, or one you saw and want to describe —
and get back a structured, honest description of the clothing: garment
types, colors, materials, fit, an overall style label, a palette of swatches,
and search queries you could paste into any shopping site. All of this runs
entirely on-device using a small vision-language model (Gemma 3n E4B via
MediaPipe LLM Inference / LiteRT-LM). No photo ever leaves the phone. There
is no shopping integration, no account, no cloud, no social feed. FitFindr
never comments on the person wearing the clothes — only on the clothes.

## Goals

- Take one photo (camera or gallery) of an outfit and produce a structured,
  parseable description within a bounded time on a modern mid-range phone.
- Run 100% on-device: no network calls for inference, no telemetry, no photo
  upload.
- Produce output that is *useful*: garment-level detail (type, color,
  material guess, fit), a style label, a color palette, and search-query
  strings a person could actually use.
- Be honest when the model is unsure: fields the model cannot confidently
  determine are set to `"unknown"` rather than guessed.
- Ship a small, well-tested Kotlin core (parsing, color extraction) that is
  unit-testable without any Android dependency or device.

## Non-goals

- No shopping, checkout, price comparison, or affiliate links.
- No user accounts, sign-in, or cloud sync.
- No social feed, sharing to third-party platforms (beyond local
  share-as-image, milestone 6), or public profiles.
- No judgment, commentary, or inference about the person wearing the outfit
  (body, identity, attractiveness, etc.) — the model is instructed to
  describe clothing only.
- No multi-photo outfit history stitching or video capture in v2.

## Architecture

```
ui/ (Compose, single-activity, Navigation Compose)
  CaptureScreen → ResultScreen → HistoryScreen → SettingsScreen (model mgmt)
domain/
  DescribeOutfitUseCase(image) → OutfitResult
  ColorExtractor (k-means in pure Kotlin, unit-testable, no Android deps)
  ResultParser (JSON → data classes, strict parse + one repair attempt)
data/
  inference/ VlmEngine interface
             ├── MediaPipeVlmEngine (real, on-device inference)
             └── FakeVlmEngine (fixtures, for tests/UI dev)
  model/     ModelDownloader (WorkManager, resumable, SHA-256 verify)
  db/        Room: OutfitEntity, GarmentEntity
di/ Hilt modules binding interfaces to implementations
```

Single `app` Gradle module for v2 (not multi-module) — the package structure
above is enforced by directory convention rather than module boundaries.

## Output schema (conceptual)

```json
{
  "styleLabel": "smart casual",
  "garments": [
    {
      "type": "jacket",
      "color": "olive green",
      "material": "cotton twill",
      "fit": "relaxed",
      "searchQueries": ["olive green cotton twill jacket relaxed fit"]
    }
  ],
  "palette": ["#5B6B3B", "#EDE6D6", "#2B2B2B"],
  "notes": "unknown fields are honestly reported as \"unknown\""
}
```

Real schema lives in `domain/ResultParser.kt`; the model is prompted with
this schema via `assets/prompts/describe_v1.txt`.

## Evaluation plan

- **Parser correctness**: golden JSON fixtures (valid output, and broken
  variants — trailing commas, markdown code fences, single-quoted strings,
  missing fields) with JUnit5 tests asserting exact parsed output or exact
  repair behavior.
- **Color extractor correctness**: deterministic k-means test cases (single
  color, two well-separated clusters, all-identical pixels, empty input)
  with JUnit5 tests asserting expected cluster centroids/order.
- **On-device eval harness (milestone 7)**: an instrumented test suite that
  runs the real `MediaPipeVlmEngine` against a small fixed set of outfit
  photos on a real device/emulator with the Android SDK, recording latency,
  memory, and JSON-parse success rate into a results file the README can
  cite. This requires an Android SDK + device/emulator with sufficient RAM
  for Gemma 3n E4B, which this cloud development container does not have
  (see "Cloud-instance constraints" below and `docs/PLAN.md`).
- No accuracy/quality metric (e.g. "is the color name right") is claimed
  anywhere without a script and a fixture set backing it. Absent that, the
  README says "not measured."

## Tech stack

- Kotlin, Jetpack Compose (Material 3), single-activity + Navigation Compose
- CameraX (capture), Android Photo Picker (gallery)
- Hilt (DI)
- Room (local history persistence)
- WorkManager (resumable model download)
- Kotlin Coroutines / Flow
- MediaPipe Tasks GenAI / LiteRT-LM (on-device VLM inference — Gemma 3n E4B)
- minSdk 26, target/compile SDK 36 (latest stable at time of writing)
- Tests: JUnit5 + Turbine + MockK for units, Compose UI tests, Robolectric
  where useful for Android-dependent logic
- ktlint + detekt + gitleaks in CI

## Milestones

1. Scaffold (Gradle version catalogs), CI (assembleDebug, unit tests, lint,
   detekt, gitleaks).
2. Capture/gallery + result UI against `FakeVlmEngine`.
3. `ResultParser` + `ColorExtractor` with extensive unit tests (golden JSON
   fixtures, broken-JSON repair cases).
4. `ModelDownloader` (resumable, checksum, storage checks, delete model).
5. `MediaPipeVlmEngine` integration with delegate selection/fallback,
   settings screen showing active accelerator.
6. History (Room) + share-as-image.
7. Eval harness (instrumented) + README results.
8. Release: signed release APK on GitHub Release, README + screenshots,
   `docs/DEMO.md`, tag `v2.0.0`.

## Cloud-instance constraints

This repository is being developed, in part, inside a cloud container that
has no Android SDK, no emulator, and no physical device or NPU attached
(verified: no `sdkmanager`/`adb` on `PATH`, `$ANDROID_HOME`/`$ANDROID_SDK_ROOT`
unset). Consequences, stated honestly rather than hidden:

- Pure-Kotlin logic (`domain/ResultParser.kt`, `domain/ColorExtractor.kt`)
  can be written and its tests can be reasoned through, but running Android
  Gradle Plugin tasks (`assembleDebug`, instrumented tests, Robolectric)
  requires the Android SDK, which is not available in this container.
- GitHub Actions runners (via `android-actions/setup-android` or
  `actions/setup-java` + Gradle's own SDK provisioning) do have the Android
  SDK preinstalled, so `assembleDebug` and the full test suite are expected
  to run in CI even though they cannot be verified locally in this session.
  This is a real, not-yet-confirmed expectation until a CI run is observed
  green — tracked in `docs/PLAN.md`.
- On-device latency/memory/accuracy numbers for the real `MediaPipeVlmEngine`
  (milestone 5/7) cannot be produced here and will be marked "not measured"
  until a session with real device access runs the eval harness.

## Definition of done (v2, full 8 milestones)

- [ ] App builds (`assembleDebug`) and passes CI (lint, detekt, gitleaks,
      unit tests) on GitHub Actions.
- [ ] Capture → on-device inference → structured result works end-to-end on
      a real device with the real `MediaPipeVlmEngine`.
- [ ] `ResultParser` and `ColorExtractor` have extensive, real, passing
      JUnit5 unit tests with golden fixtures.
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
- [ ] No secrets, model weights, or datasets are committed.
