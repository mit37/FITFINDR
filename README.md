# FitFindr

**v2 (2026 rebuild).** The original prototype's code was not preserved; this is a clean re-implementation of the same design. Numbers in this README come from this codebase.

Point your phone at an outfit and get back a structured, honest description
of the clothing — garment types, colors, materials, fit, an overall style
label, a color palette, and search queries — computed entirely on-device by
a small vision-language model. No photo ever leaves the phone. No shopping,
no accounts, no cloud, no social feed. FitFindr never comments on the person
wearing the clothes, only on the clothes.

Full product requirements: [`PRD.md`](PRD.md). Binding honesty/process
rules for this repo: [`STANDARDS.md`](STANDARDS.md). Current build status
against all 8 milestones: [`docs/PLAN.md`](docs/PLAN.md).

## Status

This is an early, in-progress rebuild. **Milestones 1–3 of 8** are done or
partially done, as described below — milestones 4–8 (real on-device model
integration, model download management, history persistence, an eval
harness, and a signed release) are **not started**. See
[`docs/PLAN.md`](docs/PLAN.md) for the authoritative, up-to-date status of
every milestone and exactly what has and has not been verified.

| Capability | Status |
|---|---|
| Gradle scaffold + version catalog | Done |
| CI workflow (lint, detekt, unit tests, assembleDebug, gitleaks) | Written, not yet observed green (needs a real run on GitHub) |
| Capture UI (gallery picker) | Done, wired to a real Android Photo Picker |
| Capture UI (live camera preview) | Scaffolded only, not bound to CameraX `PreviewView` |
| Result UI | Done, renders against `FakeVlmEngine` fixtures |
| History / Settings UI | Placeholders only |
| `ResultParser` (JSON parsing + repair) | Done, unit-tested |
| `ColorExtractor` (k-means) | Done, unit-tested |
| Real on-device VLM inference | Not started (milestone 5) |
| Model download/management | Not started (milestone 4) |
| Outfit history (Room) | Not started (milestone 6) |
| On-device eval harness | Not started (milestone 7) |
| Signed release | Not started (milestone 8) |

## Results

There are no accuracy, latency, or memory numbers in this README, and there
will not be until they come from a script in this repo run on real
hardware. This session's only measurable numbers are unit test results for
the pure-Kotlin domain logic:

- **19/19** JUnit5 tests pass for `ResultParser` and `ColorExtractor`,
  verified this session by compiling and running these exact source files
  in a standalone Kotlin/JVM project (Android Gradle Plugin itself could
  not be run in this cloud container — see "Cloud-instance constraints"
  below). Reproduce with the commands in "Running it" once you have an
  environment that can reach Google's Maven repo and/or has the Android
  SDK.
- Everything else — inference latency, memory use, JSON-parse success rate
  on real model output, color-extraction accuracy against real photos — is
  **not measured**. There is no real on-device model integration yet
  (milestone 5), so there is nothing to measure latency or accuracy of.

## Architecture

```
ui/ (Compose, single-activity, Navigation)
  CaptureScreen → ResultScreen → HistoryScreen → SettingsScreen (model mgmt)
domain/
  DescribeOutfitUseCase(image) → OutfitResult
  ColorExtractor (k-means in Kotlin, pure → unit-testable)
  ResultParser (JSON → data classes, strict + repair)
data/
  inference/ VlmEngine interface
             ├── MediaPipeVlmEngine (real, on device — not yet implemented)
             └── FakeVlmEngine (tests/UI dev, implemented)
  model/     ModelDownloader (not yet implemented)
  db/        Room: OutfitEntity, GarmentEntity (not yet implemented)
di/ Hilt (VlmEngine → FakeVlmEngine binding only, so far)
```

## Cloud-instance constraints

Part of this repository was built inside a cloud container with:
- No Android SDK (`sdkmanager`, `adb` not on `PATH`; `$ANDROID_HOME` and
  `$ANDROID_SDK_ROOT` unset).
- No emulator, physical device, or NPU/GPU delegate.
- No network access to `dl.google.com` (Google's Maven repository, which
  the Android Gradle Plugin itself needs to resolve) — confirmed with
  `curl https://dl.google.com/...` returning a `403` through the sandboxed
  proxy, while `curl https://repo.maven.apache.org/` returns `200`.

Practically, this means `./gradlew assembleDebug`, `./gradlew test` on the
`:app` module, `ktlintCheck`, `detekt`, and any instrumented/Robolectric
test could **not** be run in this container this session, and their results
are not claimed here. GitHub Actions runners are expected to have both the
Android SDK and reachable Google/Maven repos, so CI (`.github/workflows/ci.yml`)
should be able to run all of these — that is an expectation, not yet a
confirmed fact, until a real CI run on GitHub is observed green. Full detail
in [`docs/PLAN.md`](docs/PLAN.md).

## Running it

This app has not been run in an emulator or on a device by any agent in
this session (see above). To run it yourself, on a machine with the
Android SDK:

```bash
git clone <this repo>
cd FitFindr
./gradlew assembleDebug      # build a debug APK
./gradlew test               # run JUnit5 unit tests (domain layer)
./gradlew ktlintCheck        # style
./gradlew detekt             # static analysis
```

There is no real model integration yet, so the app currently runs entirely
against `FakeVlmEngine`'s fixture output — installing and opening it will
show a fixed, hardcoded "outfit result" regardless of what photo you pick.

## Testing

Unit tests live under `app/src/test/`, covering `ResultParser` (golden
fixtures for valid JSON, trailing-comma/markdown-fence/single-quote repair,
missing-field defaults, and unrecoverable input) and `ColorExtractor`
(deterministic k-means cases: single color, balanced/unbalanced multi-color
clusters, empty/single-pixel input, k-validation). Run them with
`./gradlew test` once you have Android SDK access, or reproduce this
session's standalone verification by copying
`app/src/main/java/com/mitanshm/fitfindr/domain/{OutfitResult,ResultParser,ColorExtractor}.kt`
and `app/src/test/java/com/mitanshm/fitfindr/domain/*Test.kt` (plus
`app/src/test/resources/fixtures/`) into a plain `kotlin("jvm")` +
`kotlin("plugin.serialization")` Gradle project with JUnit5 and
`kotlinx-serialization-json` on the classpath, then run `gradle test`. No
test count in this README is hardcoded independent of a real run; it is
restated from the actual output quoted in `docs/PLAN.md`.

## What this does not do

- **Does not run a real model yet.** `MediaPipeVlmEngine` (the real
  Gemma 3n E4B on-device engine) does not exist in this codebase yet — only
  its interface (`VlmEngine`) and a fixture-returning fake
  (`FakeVlmEngine`). Every "result" the app currently shows is the same
  hardcoded fixture.
- **Does not have a working camera capture flow.** The gallery picker works
  (Android Photo Picker); live camera preview via CameraX is scaffolded but
  not bound to a `PreviewView`, and has not been run on a device.
- **Does not persist any history.** `HistoryScreen` is a placeholder; there
  is no Room database wired up yet.
- **Does not manage model downloads.** `SettingsScreen` is a placeholder;
  there is no `ModelDownloader`, no checksum verification, no delete flow.
- **Does not have CI proven green.** The workflow is written but has not
  been observed to pass on GitHub Actions as of this commit.
- **Does not have any release.** No signed APK, no GitHub Release, no
  `v2.0.0` tag yet.
- **Does not do shopping, checkout, accounts, cloud sync, or social
  sharing to third parties**, and never will — these are explicit
  non-goals of the product, not gaps.
- **Does not comment on the person wearing the clothes.** This is enforced
  only by prompt instruction (`assets/prompts/describe_v1.txt`) right now,
  since there is no real model running yet to verify the instruction is
  actually followed.

## Design decisions

- **Pure-Kotlin domain core.** `ResultParser` and `ColorExtractor` have zero
  Android imports so they can be unit-tested on the plain JVM without an
  emulator, Robolectric, or a device — this mattered directly in this
  session, since it is the only reason any test could be run at all in this
  container.
- **One repair pass, not a general JSON5 parser.** `ResultParser`'s repair
  step is a small, explicit set of regex fixes (strip fences, drop trailing
  commas, convert single quotes) targeted at what a small on-device model
  plausibly produces, not a general-purpose lenient JSON parser. This keeps
  its behavior easy to reason about and test exhaustively, at the cost of
  not repairing more exotic malformed JSON.
- **Single Gradle module for now.** The PRD's package diagram is enforced by
  directory convention (`ui/`, `domain/`, `data/`, `di/`) rather than
  separate Gradle modules, since multi-module separation adds build
  complexity that isn't justified yet for an app this size.
- **`FakeVlmEngine` as the real default binding today.** Hilt currently
  binds `VlmEngine` to `FakeVlmEngine`, not a stub that throws — this lets
  every other layer (UI, use case, DI graph) be built and reasoned about
  end-to-end before the real model exists.

## License

MIT. See [`LICENSE`](LICENSE).

---

Built with AI coding agents (Claude Code) under my direction; design,
specs, review and evaluation are mine.
