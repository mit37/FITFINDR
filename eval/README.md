# Eval harness

This directory is the eval set for the on-device eval harness described in
`PRD.md`'s "Evaluation plan" and milestone 7. It intentionally contains
**no photos and no labels rows yet** — those must come from Mitansh
photographing ~100 real outfits himself (per `PRD.md`); nothing here is
invented or scraped.

## `labels.csv` schema

```
photo_id,filename,expected_garment_types,expected_colors,expected_style_label,device_class,notes
```

| column | meaning |
| --- | --- |
| `photo_id` | Stable integer/string id for the photo. |
| `filename` | File name of the photo under `eval/photos/` (not committed — see below). |
| `expected_garment_types` | `;`-separated list of the garment types a human labeler sees, e.g. `jacket;trousers;shoes`. Compared against the model's `garments[].type` via `EvalMetrics.garmentTypeScore`. |
| `expected_colors` | `;`-separated list of expected color names, one per garment, same order as `expected_garment_types`. |
| `expected_style_label` | The style label a human labeler would give the outfit (e.g. `smart casual`). |
| `device_class` | The phone this photo's latency/RAM numbers should be attributed to when run (e.g. `pixel8_2024`), so results from different phones aren't averaged together misleadingly. |
| `notes` | Free text, e.g. "backlit, hard case". |

Photos themselves go in `eval/photos/` (git-ignored — see `.gitignore`'s
`eval/photos/` entry) since they are Mitansh's own photographs, not
something an AI agent can honestly produce or should commit on his behalf.

## What the harness measures (`PRD.md`'s evaluation plan)

- **Garment-type recall/precision** — `EvalMetrics.garmentTypeScore`,
  comparing the parsed `OutfitResult.garments[].type` against
  `expected_garment_types`.
- **Color accuracy (model-only vs. +pixel cross-check)** —
  `EvalMetrics.colorNameMatchesPixelExtraction`, comparing the model's
  color name against `ColorExtractor`'s pixel-derived palette for the same
  photo.
- **JSON validity rate, pre- and post-repair** — `EvalMetrics.jsonValidity`,
  using `ResultParser.parseStrictOnly` (no repair) vs. `ResultParser.parse`
  (one repair pass allowed) over the raw text the model actually returned
  for each photo.
- **Latency per device class** — wall-clock time from
  `VlmEngine.describe()` call to return, bucketed by the `device_class`
  column.
- **Peak RAM** — `android.os.Debug.getNativeHeapAllocatedSize()` (or
  `ActivityManager.MemoryInfo`) sampled around each inference call.

## Two ways this harness runs

1. **On-device (real numbers, not available in this repo yet)**:
   `app/src/androidTest/java/com/mitanshm/fitfindr/eval/EvalHarnessInstrumentedTest.kt`
   reads `eval/labels.csv` + `eval/photos/`, runs each photo through the
   real `VlmEngine` binding configured for the build (`MediaPipeVlmEngine`
   on a real device build), and writes latency/RAM/parse/accuracy numbers.
   Run it with:

   ```
   ./gradlew connectedCheck -Peval
   ```

   (The `-Peval` project property, set in `app/build.gradle.kts`, narrows
   `connectedCheck` to just this test class so it doesn't run alongside
   unrelated instrumented tests.) **This requires a connected Android
   device or emulator with the Android SDK, and has never been run** — see
   `docs/PLAN.md`, this container has neither.

2. **Self-check (runs today, in this repo, on the JVM)**:
   `app/src/test/java/com/mitanshm/fitfindr/eval/EvalHarnessSelfCheckTest.kt`
   runs the exact same `EvalMetrics` logic against `FakeVlmEngine`'s fixed
   fixture output and the existing `ResultParser`/`ColorExtractor` JSON
   fixtures (`app/src/test/resources/fixtures/`). This proves the harness's
   *mechanism* (parsing, scoring, JSON-validity counting) is correct and
   exercised for real — see `docs/PLAN.md` for the actual run and its
   output. **It is not a measurement of real model performance** —
   `FakeVlmEngine` always returns the same canned string, so its "latency"
   and "accuracy" numbers reflect string-parsing speed and one fixture's
   self-consistency, not Gemma 3n E4B on a phone. `eval/results.json`
   labels this clearly as `"harness_self_check"`, separate from the
   `"pending_real_device_numbers"` section reserved for a real run.
