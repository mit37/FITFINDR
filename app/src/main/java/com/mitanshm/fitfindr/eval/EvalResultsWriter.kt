package com.mitanshm.fitfindr.eval

import org.json.JSONObject
import java.io.File

/**
 * Writes the on-device eval run's numbers to a JSON file on the device
 * (`getExternalFilesDir(null)/eval_results.json`), to be pulled off with
 * `adb pull` and merged into this repo's `eval/results.json` by hand under
 * its `pending_real_device_numbers` section. Deliberately does not
 * overwrite `eval/results.json` directly -- that file lives in source
 * control and a device-side test process has no business writing to a
 * developer's git working tree.
 */
object EvalResultsWriter {
    fun write(
        outputFile: File,
        labelCount: Int,
        jsonValidity: EvalMetrics.ParseValidity,
        averageLatencyMs: Double,
        peakNativeHeapBytes: Long,
    ) {
        val json =
            JSONObject().apply {
                put("label_count", labelCount)
                put("strict_json_valid_rate", jsonValidity.strictValidRate)
                put("post_repair_json_valid_rate", jsonValidity.postRepairValidRate)
                put("average_latency_ms", averageLatencyMs)
                put("peak_native_heap_bytes", peakNativeHeapBytes)
            }
        outputFile.writeText(json.toString(JSON_INDENT))
    }

    private const val JSON_INDENT = 2
}
