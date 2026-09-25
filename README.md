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

This is an in-progress rebuild. **All 8 milestones now have real code**,
but several are honestly incomplete — see the table below and
[`docs/PLAN.md`](docs/PLAN.md) for the authoritative, up-to-date status of
every milestone and exactly what has and has not been verified. The single
biggest gap: **no Android SDK, emulator, physical device, or NPU/GPU has
been available in any development session so far** (see "Cloud-instance
constraints"), so nothing that requires running the app or the Android
Gradle Plugin has been observed to actually work.

| Capability | Status |
|---|---|
| Gradle scaffold + version catalog | Done |
| CI workflow (lint, detekt, unit tests, assembleDebug, gitleaks) | Written, not yet observed green (needs a real run on GitHub) |
| Capture UI (gallery picker) | Done, wired to a real Android Photo Picker |
| Capture UI (live camera preview) | Scaffolded only, not bound to CameraX `PreviewView` |
| Result UI | Done, renders against `FakeVlmEngine` fixtures; "share as image" wired |
| `ResultParser` (JSON parsing + repair) | Done, unit-tested |
| `ColorExtractor` (k-means) | Done, unit-tested |
| `ModelDownloader` (resumable, checksum, storage-checked, deletable) | Code + pure-logic unit tests done; never run against a real network/device |
| `MediaPipeVlmEngine` (real on-device inference) | Code written against the documented API; **never run**, not the bound `VlmEngine` |
| Settings UI (model mgmt + active accelerator) | Real UI, wired to `ModelDownloader`; accelerator always reads "not applicable" since `MediaPipeVlmEngine` isn't bound yet |
| Outfit history (Room) + share-as-image | Code done (entities/DAO/repository/UI); mapping logic unit-tested; Room/UI paths never run |
| On-device eval harness | Harness code + metrics done and self-check-verified; real device numbers not measured (no photos, no device) |
| Signed release | Workflow skeleton only; no keystore, no tag, no APK |

## Results

There are no accuracy, latency, or memory numbers for the real model in
this README, and there will not be until they come from a script in this
repo run on real hardware. What actually is measured, reproducibly, in
this repo:

- **59/59** JUnit5 tests pass across every pure-Kotlin/JVM piece of this
  codebase (`ResultParser`, `ColorExtractor`, `ChecksumVerifier`,
  `StoragePrecheck`, `DownloadPlanner`, `OutfitMapper`, `LabelsCsv`,
  `EvalMetrics`, and the eval harness self-check), verified by compiling
  and running these exact source files in a standalone Kotlin/JVM Gradle
  project (the Android Gradle Plugin itself still cannot be resolved in
  this cloud container — see "Cloud-instance constraints"). See
  `docs/PLAN.md` for the full, dated command output.
- **Eval harness self-check** (`eval/results.json`'s `harness_self_check`
  section): JSON validity rate over the 6 existing golden fixtures is
  2/6 (33%) strict, 5/6 (83%) after one repair pass; `FakeVlmEngine` +
  `ResultParser` round-trips average ~0.3ms/call. This is a real,
  reproducible measurement of the harness's own code — **not** a
  measurement of real model accuracy or latency, since `FakeVlmEngine`
  never runs an actual model. See `eval/README.md`.
- Everything else — real-model inference latency, peak RAM, garment-type
  precision/recall, color accuracy, JSON-validity rate on real model
  output — is **not measured**. `eval/results.json`'s
  `pending_real_device_numbers` section lists exactly what's missing and
  why (no labeled photos yet, no device, `MediaPipeVlmEngine` unverified).

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
             ├── MediaPipeVlmEngine (real code, unverified on-device — see below)
             └── FakeVlmEngine (tests/UI dev — the actual bound implementation today)
  model/     ModelDownloader (WorkManager, resumable + checksum-verified; untested against a real network)
  db/        Room: OutfitEntity, GarmentEntity, OutfitDao, OutfitRepository (real, unverified against real Room)
eval/        EvalMetrics, LabelsCsv (pure, unit-tested); EvalHarnessInstrumentedTest (real, never run)
di/ Hilt (VlmEngine → FakeVlmEngine binding; see InferenceModule.kt for how to switch it)
```

## Cloud-instance constraints

Part of this repository was built inside a cloud container with (re-confirmed
in the milestone 4-8 session, same result as milestones 1-3):
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
any session so far (see above). To run it yourself, on a machine with the
Android SDK:

```bash
git clone <this repo>
cd FitFindr
./gradlew assembleDebug      # build a debug APK
./gradlew test               # run JUnit5 unit tests (domain + data layers)
./gradlew ktlintCheck        # style
./gradlew detekt             # static analysis
./gradlew installDebug       # install on a connected device/emulator
```

The app currently runs entirely against `FakeVlmEngine`'s fixture output —
installing and opening it will show a fixed, hardcoded "outfit result"
regardless of what photo you pick, but history persistence (Room) and
share-as-image are real and will actually work. To try the real on-device
model path once you have a device: supply a real hosted `.task` model URL
and its SHA-256 in `ModelConfig.kt` (the committed values are placeholders
— see that file), use Settings to download it, then switch
`InferenceModule`'s binding to `MediaPipeVlmEngine` (see that file's doc
comment for the exact change) and rebuild. This has not been done or
verified by any agent session; you would be the first to actually run it.

To run the eval harness against real photos once you have both a device
and ~100 labeled photos of your own (see `eval/README.md` — never commit
the photos themselves): `./gradlew connectedCheck -Peval`.

## Testing

Unit tests live under `app/src/test/`, covering:
- `domain/`: `ResultParser` (golden fixtures for valid JSON, trailing-comma/
  markdown-fence/single-quote repair, missing-field defaults, unrecoverable
  input) and `ColorExtractor` (deterministic k-means cases).
- `data/model/`: `ChecksumVerifier`, `StoragePrecheck`, `DownloadPlanner` —
  the pure decision logic behind `ModelDownloader`.
- `data/db/`: `OutfitMapper` — the pure domain↔Room-entity conversion logic
  behind `OutfitRepository`.
- `eval/`: `LabelsCsv`, `EvalMetrics`, and `EvalHarnessSelfCheckTest` — the
  eval harness's own logic, self-checked against `FakeVlmEngine` and the
  existing golden fixtures.

Run them with `./gradlew test` once you have Android SDK access, or
reproduce this session's standalone verification by copying the listed
pure-Kotlin source files (zero `android.*`/`androidx.room.*` imports by
design) plus their test files and `app/src/test/resources/fixtures/` into
a plain `kotlin("jvm")` + `kotlin("plugin.serialization")` Gradle project
with JUnit5, `kotlinx-coroutines-core`, `kotlinx-serialization-json`, and
`javax.inject:javax.inject:1` on the classpath, then run `gradle test`. No
test count in this README is hardcoded independent of a real run; it is
restated from the actual output quoted in `docs/PLAN.md`.

## What this does not do

- **Does not run a real model.** `MediaPipeVlmEngine` exists as real code
  against the documented MediaPipe Tasks GenAI API, but has never been run
  — no Android SDK, device, emulator, or NPU/GPU has been available in any
  development session, and it is not the DI-bound `VlmEngine` (`FakeVlmEngine`
  is). Every "result" the app currently shows is the same hardcoded fixture.
- **Does not actually download a model.** `ModelConfig.DOWNLOAD_URL` and
  `EXPECTED_SHA256` are placeholders (see that file) — no real model is
  hosted anywhere this repo points to yet. `ModelDownloader`'s resumable-
  download/checksum/storage-check *logic* is real and unit-tested, but has
  never completed an actual network download.
- **Does not have a working live camera capture flow.** The gallery picker
  works (Android Photo Picker); live camera preview via CameraX is
  scaffolded but not bound to a `PreviewView`, and has not been run on a
  device.
- **History and share-as-image are unverified end-to-end.** The Room
  entities/DAO/repository and the share-as-image `Bitmap` renderer are real
  code, and the pure domain↔entity mapping logic is unit-tested — but Room
  itself, the Compose `HistoryScreen`, and `ShareImage`'s Android graphics
  calls have never actually been run.
- **The eval harness has no real numbers yet.** It can score garment-type
  precision/recall, JSON validity, and a color/pixel cross-check, and its
  own logic is self-checked against `FakeVlmEngine` — but there are no
  labeled photos (`eval/labels.csv` is header-only) and no device to run
  `EvalHarnessInstrumentedTest` on.
- **Does not have CI proven green.** The workflow is written but has not
  been observed to pass on GitHub Actions as of this commit.
- **Does not have any release.** `.github/workflows/release.yml` is a
  skeleton with no signing secrets configured; no signed APK, no GitHub
  Release, no `v2.0.0` tag exist. See `docs/PLAN.md` for exactly what's
  left before that tag would be honest.
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
  every other layer (UI, use case, DI graph, history persistence) be built
  and reasoned about end-to-end before the real model exists.
- **A standalone JVM project as the verification workaround.** Every
  "unit-tested" claim in this README for pure-Kotlin logic (parser, color
  extractor, checksum, storage precheck, download planner, entity mapper,
  eval metrics) was independently re-run in a standalone `kotlin("jvm")`
  Gradle project built from the exact same source files, because this
  cloud environment cannot resolve the Android Gradle Plugin from Google's
  Maven repo. This is a workaround for this development environment, not
  a permanent testing strategy — `./gradlew test` on `:app` itself is the
  intended path and is expected (not yet confirmed) to also pass in CI.
- **Room entities carry Room annotations even though the mapper is
  Android-free.** `OutfitEntity`/`GarmentEntity` use real `@Entity`/
  `@ForeignKey` annotations (Room needs them to generate code), but
  `OutfitMapper`'s domain↔entity conversion functions only reference the
  entity *classes*, never `androidx.room.*` directly — which is what makes
  the mapper logic itself testable without Room on the classpath at all.

## License

MIT. See [`LICENSE`](LICENSE).

---

Built with AI coding agents (Claude Code) under my direction; design,
specs, review and evaluation are mine.
