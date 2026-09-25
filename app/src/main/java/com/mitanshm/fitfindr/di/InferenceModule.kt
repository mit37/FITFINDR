package com.mitanshm.fitfindr.di

import com.mitanshm.fitfindr.data.inference.FakeVlmEngine
import com.mitanshm.fitfindr.data.inference.VlmEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds [VlmEngine] to [FakeVlmEngine] for now. Milestone 5 introduces
 * `MediaPipeVlmEngine` and this binding will switch to it (likely gated by
 * a build variant or a runtime capability check, since the real engine
 * needs the Android SDK / a real device to function at all).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {
    @Binds
    abstract fun bindVlmEngine(fake: FakeVlmEngine): VlmEngine
}
