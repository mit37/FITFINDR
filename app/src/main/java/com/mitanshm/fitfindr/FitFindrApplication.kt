package com.mitanshm.fitfindr

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * [Configuration.Provider] wiring lets [com.mitanshm.fitfindr.data.model.ModelDownloadWorker]
 * (a `@HiltWorker`) receive its Hilt-injected dependencies. This requires
 * disabling WorkManager's default `ContentProvider`-based initialization in
 * the manifest (see `AndroidManifest.xml`) in favor of on-demand
 * initialization using this factory.
 */
@HiltAndroidApp
class FitFindrApplication :
    Application(),
    Configuration.Provider {
        @Inject lateinit var workerFactory: HiltWorkerFactory

        override val workManagerConfiguration: Configuration
            get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
    }
