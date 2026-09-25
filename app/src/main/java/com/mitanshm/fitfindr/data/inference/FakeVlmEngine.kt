package com.mitanshm.fitfindr.data.inference

import javax.inject.Inject

/**
 * A [VlmEngine] that returns canned fixture JSON instead of running any
 * model. Used for unit tests, Compose previews, and UI development while
 * the real [MediaPipeVlmEngine] (milestone 5) does not exist yet. Never
 * touches [imageBytes] beyond ignoring it — this is intentional: it lets
 * `ui/CaptureScreen.kt` and `ui/ResultScreen.kt` be developed and reasoned
 * about without a device or model weights.
 */
class FakeVlmEngine
    @Inject
    constructor() : VlmEngine {
        override suspend fun describe(imageBytes: ByteArray): String = FIXTURE_RESPONSE

        companion object {
            val FIXTURE_RESPONSE =
                """
                {
                  "styleLabel": "smart casual",
                  "garments": [
                    {
                      "type": "jacket",
                      "color": "olive green",
                      "material": "cotton twill",
                      "fit": "relaxed",
                      "searchQueries": ["olive green cotton twill jacket relaxed fit"]
                    },
                    {
                      "type": "trousers",
                      "color": "charcoal",
                      "material": "wool blend",
                      "fit": "tapered",
                      "searchQueries": ["charcoal wool blend tapered trousers"]
                    },
                    {
                      "type": "shoes",
                      "color": "white",
                      "material": "leather",
                      "fit": "unknown",
                      "searchQueries": ["white leather sneakers"]
                    }
                  ],
                  "palette": ["#5B6B3B", "#2B2B2B", "#EDE6D6"],
                  "notes": "Fixture output from FakeVlmEngine."
                }
                """.trimIndent()
        }
    }
