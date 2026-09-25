package com.mitanshm.fitfindr.di

import com.mitanshm.fitfindr.data.inference.FakeVlmEngine
import com.mitanshm.fitfindr.data.inference.MediaPipeVlmEngine
import com.mitanshm.fitfindr.data.inference.VlmEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds [VlmEngine] to [FakeVlmEngine].
 *
 * [MediaPipeVlmEngine] (milestone 5) now exists as real code against the
 * documented MediaPipe Tasks GenAI API, but has never been run on a device
 * (see its doc comment and docs/PLAN.md) -- so it deliberately stays
 * unbound here. The Compose UI and all unit tests keep exercising
 * `FakeVlmEngine`, which is the only path that has actually been reasoned
 * through end-to-end in this environment.
 *
 * ## Switching to the real engine on a real device build
 * Once someone with a real device/emulator has verified
 * [MediaPipeVlmEngine] works (loads the model, produces parseable output,
 * doesn't crash on delegate fallback), switch the binding below:
 *
 * ```kotlin
 * @Binds
 * abstract fun bindVlmEngine(real: MediaPipeVlmEngine): VlmEngine
 * ```
 *
 * A longer-term improvement (not done here, to avoid adding complexity
 * that itself can't be verified) would gate this per build variant/flavor
 * instead of a single hardcoded binding, so debug builds can still use
 * `FakeVlmEngine` for fast UI iteration while release builds use the real
 * engine.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {
    @Binds
    abstract fun bindVlmEngine(fake: FakeVlmEngine): VlmEngine
}
