package com.mitanshm.fitfindr.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import com.mitanshm.fitfindr.domain.OutfitResult
import java.io.File
import java.io.FileOutputStream

/**
 * Renders an [OutfitResult] to a PNG [Bitmap] and shares it via Android's
 * standard share sheet (`ACTION_SEND`), for "share as image" (milestone 6).
 *
 * Uses a plain [android.graphics.Canvas] text layout rather than capturing
 * the live Compose card (`graphicsLayer`/`ImageBitmap` capture needs an
 * attached, composed view, which this environment cannot exercise without
 * a device/emulator) -- this keeps the renderer itself unit-testable in
 * principle and, more importantly, actually simple enough to reason about
 * by reading. It is still Android-SDK code (`android.graphics.*`,
 * `FileProvider`) and has NOT been run on a device -- see docs/PLAN.md.
 */
object ShareImage {

    fun render(outfit: OutfitResult): Bitmap {
        val width = CARD_WIDTH_PX
        val lineHeight = 44
        val garmentLines = outfit.garments.sumOf { 3 + it.searchQueries.size }
        val height = 260 + garmentLines * lineHeight + outfit.palette.size.coerceAtLeast(1) * 10 + 120

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 52f; isFakeBoldText = true }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 34f }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 38f; isFakeBoldText = true }

        var y = 80f
        canvas.drawText("FitFindr", 40f, y, titlePaint)
        y += 60f
        canvas.drawText("Style: ${outfit.styleLabel}", 40f, y, bodyPaint)
        y += 60f

        canvas.drawText("Palette", 40f, y, headingPaint)
        y += 50f
        var swatchX = 40f
        outfit.palette.forEach { hex ->
            val swatchPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = runCatching { Color.parseColor(hex) }.getOrDefault(Color.LTGRAY)
                }
            canvas.drawRect(swatchX, y, swatchX + 80f, y + 60f, swatchPaint)
            swatchX += 100f
        }
        y += 100f

        canvas.drawText("Garments", 40f, y, headingPaint)
        y += 50f
        outfit.garments.forEach { garment ->
            canvas.drawText("${garment.type} — ${garment.color}", 40f, y, bodyPaint)
            y += lineHeight
            canvas.drawText("Material: ${garment.material}, fit: ${garment.fit}", 60f, y, bodyPaint)
            y += lineHeight
            garment.searchQueries.forEach { query ->
                canvas.drawText("Search: \"$query\"", 60f, y, bodyPaint)
                y += lineHeight
            }
            y += 10f
        }

        return bitmap
    }

    /** Writes [bitmap] to a cache file and launches the system share sheet for it via [FileProvider]. */
    fun share(
        context: Context,
        bitmap: Bitmap,
    ) {
        val cacheDir = File(context.cacheDir, "shared_outfits").apply { mkdirs() }
        val file = File(cacheDir, "outfit_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(intent, "Share outfit"))
    }

    private const val CARD_WIDTH_PX = 900
    private const val PNG_QUALITY = 100
}
