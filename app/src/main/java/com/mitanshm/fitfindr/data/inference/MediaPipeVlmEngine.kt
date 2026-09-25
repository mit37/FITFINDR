package com.mitanshm.fitfindr.data.inference

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.Backend
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.mitanshm.fitfindr.data.model.ModelConfig
import com.mitanshm.fitfindr.data.model.ModelDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real, on-device [VlmEngine] implementation: runs Gemma 3n E4B via
 * MediaPipe Tasks GenAI's `LlmInference` / LiteRT-LM API, against the
 * `.task` bundle [ModelDownloader] has downloaded to app-private storage.
 *
 * ############################################################################
 * # HONESTY (see docs/PLAN.md -- read this before trusting anything below) #
 * ############################################################################
 * This class has **never been run**. This development container has no
 * Android SDK, no emulator, no physical device, and no NPU/GPU delegate to
 * test against (see docs/PLAN.md, "Cloud-instance constraints" -- confirmed
 * with `which sdkmanager`/`adb`, `$ANDROID_HOME`), and even the network
 * host that would serve a real `.task` model bundle is unreachable here.
 * It is written directly against the documented `com.google.mediapipe:
 * tasks-genai` `LlmInference`/`LlmInferenceSession`/`GraphOptions` API
 * surface (delegate selection via `Backend.GPU`/`Backend.CPU`, session
 * creation, `generateResponse`) as of the version pinned in
 * `gradle/libs.versions.toml`, and reviewed by eye only. It is entirely
 * plausible the exact method names, `GraphOptions` fields, or delegate
 * fallback behavior have drifted from what is written here by the time
 * this actually runs on a device -- that is precisely why this cannot be
 * marked "done" (see docs/PLAN.md's milestone-honesty section).
 *
 * It is NOT the DI-bound [VlmEngine] by default -- `InferenceModule` binds
 * [FakeVlmEngine], which IS exercised by the UI and tests. Switching to
 * this engine for a real device build means changing `InferenceModule`'s
 * `@Binds` target (see that file's doc comment) once someone can actually
 * verify this class against real hardware.
 */
@Singleton
class MediaPipeVlmEngine
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val modelDownloader: ModelDownloader,
    ) : VlmEngine {

        @Volatile
        private var cachedSession: LlmInferenceSession? = null

        @Volatile
        private var activeBackend: Backend? = null

        override suspend fun describe(imageBytes: ByteArray): String =
            withContext(Dispatchers.Default) {
                check(modelDownloader.isModelReady()) {
                    "Model is not downloaded/checksum-verified yet -- call ModelDownloader.enqueueDownload() " +
                        "and wait for ModelDownloadState.Complete before using MediaPipeVlmEngine."
                }

                val session = session()
                val bitmap =
                    checkNotNull(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)) {
                        "Could not decode imageBytes as a bitmap"
                    }

                session.addQueryChunk(loadPromptTemplate())
                // NOTE (honesty): `BitmapImageBuilder`/`MPImage` live in MediaPipe's
                // vision framework artifact, which `tasks-genai` may or may not
                // transitively pull in depending on the pinned version -- this
                // import path is the best documentation-based guess, not a
                // verified-working one. See this file's class doc.
                session.addImage(com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build())
                session.generateResponse()
            }

        /**
         * Lazily creates the [LlmInference] engine + a fresh [LlmInferenceSession],
         * picking a delegate per [preferredBackend]. MediaPipe's documented
         * behavior is that requesting [Backend.GPU] on a device/driver
         * combination it doesn't support throws at session-creation time,
         * which is why this falls back to [Backend.CPU] on any exception
         * rather than propagating a delegate-selection failure to the
         * caller -- unverified, since no device/GPU exists here to actually
         * exercise either branch.
         */
        private fun session(): LlmInferenceSession {
            cachedSession?.let { return it }
            synchronized(this) {
                cachedSession?.let { return it }

                val modelPath = File(context.filesDir, ModelConfig.MODEL_FILE_NAME).absolutePath
                val backend = preferredBackend()

                val session =
                    try {
                        buildSession(modelPath, backend).also { activeBackend = backend }
                    } catch (e: IllegalStateException) {
                        if (backend == Backend.GPU) {
                            buildSession(modelPath, Backend.CPU).also { activeBackend = Backend.CPU }
                        } else {
                            throw e
                        }
                    }
                cachedSession = session
                return session
            }
        }

        private fun buildSession(
            modelPath: String,
            backend: Backend,
        ): LlmInferenceSession {
            val llmInference =
                LlmInference.createFromOptions(
                    context,
                    LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(MAX_TOKENS)
                        .setPreferredBackend(backend)
                        .build(),
                )
            return LlmInferenceSession.createFromOptions(
                llmInference,
                LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(TOP_K)
                    .setTemperature(TEMPERATURE)
                    .setGraphOptions(GraphOptions.builder().setEnableVisionModality(true).build())
                    .build(),
            )
        }

        /**
         * Delegate preference: GPU first (fastest on most mid-range phones
         * per the PRD's target), CPU as the guaranteed-available fallback.
         * NPU delegate selection (mentioned in the PRD/README as a goal) is
         * not yet exposed by the pinned `tasks-genai` version's public API
         * as a distinct [Backend] value as of writing -- tracked as a
         * follow-up once verified against a real NPU-capable device and the
         * MediaPipe release notes for whichever version is current then.
         */
        private fun preferredBackend(): Backend = Backend.GPU

        private fun loadPromptTemplate(): String =
            context.assets.open(PROMPT_ASSET_PATH).bufferedReader().use { it.readText() }

        /**
         * Which delegate the last successfully created session actually
         * ended up using (after any GPU->CPU fallback), for
         * [com.mitanshm.fitfindr.ui.SettingsScreen] to display. Null before
         * a session has been created (i.e. before the first [describe]
         * call, or if the model isn't downloaded yet).
         */
        fun activeBackendLabel(): String? = activeBackend?.name

        companion object {
            private const val PROMPT_ASSET_PATH = "prompts/describe_v1.txt"
            private const val MAX_TOKENS = 1024
            private const val TOP_K = 40
            private const val TEMPERATURE = 0.3f
        }
    }
