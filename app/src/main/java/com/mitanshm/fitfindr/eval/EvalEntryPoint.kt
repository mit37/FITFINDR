package com.mitanshm.fitfindr.eval

import com.mitanshm.fitfindr.data.inference.VlmEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.content.Context

/**
 * Lets [EvalHarnessInstrumentedTest] (a plain instrumented test, not an
 * `@AndroidEntryPoint` Activity/Fragment) pull the app's configured
 * [VlmEngine] out of the Hilt graph -- whichever binding `InferenceModule`
 * currently points at (`FakeVlmEngine` today; `MediaPipeVlmEngine` once
 * milestone 5's real-device wiring is switched on, see docs/PLAN.md).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface EvalEntryPoint {
    fun vlmEngine(): VlmEngine

    companion object {
        fun vlmEngine(context: Context): VlmEngine =
            EntryPointAccessors.fromApplication(context.applicationContext, EvalEntryPoint::class.java).vlmEngine()
    }
}
