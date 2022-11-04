package com.m3sv.plainupnp

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.StrictMode
import com.m3sv.plainupnp.interfaces.LifecycleManager
import com.m3sv.plainupnp.presentation.main.MainActivity
import com.m3sv.plainupnp.presentation.splash.SplashActivity
import com.m3sv.plainupnp.server.ServerManager
import com.m3sv.plainupnp.upnp.UpnpScopeProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import timber.log.Timber
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Router, UpnpScopeProvider {

    @Inject
    lateinit var lifecycleManager: LifecycleManager

    @Inject
    lateinit var serverManager: ServerManager

    override val upnpScope =
        CoroutineScope(SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())

            StrictMode.setThreadPolicy(
                StrictMode
                    .ThreadPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode
                    .VmPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }

    override fun getMainActivityIntent(context: Context): Intent = Intent(context, MainActivity::class.java)

    override fun getSplashActivityIntent(context: Context): Intent = Intent(context, SplashActivity::class.java)
}
