package com.mitanshm.fitfindr.data.inference

/**
 * Abstraction over "run the vision-language model on an image and return raw
 * text output". The real implementation ([MediaPipeVlmEngine], milestone 5)
 * runs Gemma 3n E4B on-device via MediaPipe Tasks GenAI / LiteRT-LM. Tests
 * and UI development use [FakeVlmEngine].
 *
 * [imageBytes] is expected to be a JPEG/PNG-encoded photo. The engine does
 * not interpret it further than handing it to the model; [describe] returns
 * the model's raw text response, which [com.mitanshm.fitfindr.domain.ResultParser]
 * is responsible for turning into structured data. Keeping the boundary here
 * (raw text in, raw text out) is what lets [FakeVlmEngine] stand in for the
 * real engine without any MediaPipe/Android dependency.
 */
interface VlmEngine {
    suspend fun describe(imageBytes: ByteArray): String
}
