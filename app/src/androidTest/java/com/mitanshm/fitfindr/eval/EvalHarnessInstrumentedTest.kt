package com.mitanshm.fitfindr.eval

import android.os.Debug
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device eval harness: runs every labeled photo in `eval/labels.csv` +
 * `eval/photos/` (see `eval/README.md`) through the app's configured
 * [com.mitanshm.fitfindr.data.inference.VlmEngine], and records
 * garment-type precision/recall, JSON validity pre/post repair, latency,
 * and peak native heap to a results file on the device.
 *
 * HONESTY (see docs/PLAN.md -- the single most important note in this
 * file): this test has **never been run**. This development container has
 * no Android SDK, emulator, or physical device (see docs/PLAN.md,
 * "Cloud-instance constraints"), so there is no way to execute an
 * instrumented test here at all -- `./gradlew connectedCheck` itself
 * requires a connected device/emulator that does not exist in this
 * environment. The logic it calls out to ([EvalMetrics], [LabelsCsv],
 * `ResultParser`, `ColorExtractor`) IS unit tested and independently
 * verified -- see `EvalHarnessSelfCheckTest.kt`. This class is implemented
 * against the documented `androidx.test`/Hilt/`kotlinx.coroutines` APIs and
 * reviewed by eye only.
 *
 * ## Running on a real device
 * 1. `adb push eval/photos <device>/Android/data/com.mitanshm.fitfindr/files/eval_photos`
 *    and `adb push eval/labels.csv <device>/.../files/eval_labels.csv`
 *    (see `eval/README.md` -- photos are Mitansh's own, never committed).
 * 2. `./gradlew connectedCheck -Peval`
 * 3. `adb pull <device>/.../files/eval_results.json` and merge its numbers
 *    into this repo's `eval/results.json` under `pending_real_device_numbers`.
 */
@RunWith(AndroidJUnit4::class)
class EvalHarnessInstrumentedTest {

    @Test
    fun runEvalHarnessAgainstLabeledPhotos() =
        runBlocking {
            val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
            val labelsFile = File(targetContext.filesDir, "eval_labels.csv")
            if (!labelsFile.exists()) {
                // No labels/photos have been pushed to the device yet (see the
                // class doc's "Running on a real device" steps). Nothing to
                // measure -- an honest no-op, not a failure.
                return@runBlocking
            }

            val labelRows = LabelsCsv.parse(labelsFile.readText())
            val vlmEngine = EvalEntryPoint.vlmEngine(targetContext)
            val photosDir = File(targetContext.filesDir, "eval_photos")

            val rawResponses = mutableListOf<String>()
            val perPhotoLatencyMs = mutableListOf<Long>()
            var peakNativeHeapBytes = 0L

            labelRows.forEach { row ->
                val photoBytes = File(photosDir, row.filename).readBytes()

                val startNanos = System.nanoTime()
                val rawResponse = vlmEngine.describe(photoBytes)
                val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

                rawResponses += rawResponse
                perPhotoLatencyMs += elapsedMs
                peakNativeHeapBytes = maxOf(peakNativeHeapBytes, Debug.getNativeHeapAllocatedSize())
            }

            EvalResultsWriter.write(
                outputFile = File(targetContext.getExternalFilesDir(null), "eval_results.json"),
                labelCount = labelRows.size,
                jsonValidity = EvalMetrics.jsonValidity(rawResponses),
                averageLatencyMs = perPhotoLatencyMs.averageOrZero(),
                peakNativeHeapBytes = peakNativeHeapBytes,
            )
        }

    private fun List<Long>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
