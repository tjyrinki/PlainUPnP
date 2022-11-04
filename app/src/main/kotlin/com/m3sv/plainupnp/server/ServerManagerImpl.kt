package com.m3sv.plainupnp.server

import com.m3sv.plainupnp.common.preferences.Preferences
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.interfaces.LifecycleManager
import com.m3sv.plainupnp.logging.Logger
import com.m3sv.plainupnp.upnp.android.AndroidUpnpServiceImpl
import com.m3sv.plainupnp.upnp.server.MediaServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.fourthline.cling.UpnpService
import org.fourthline.cling.controlpoint.ControlPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

@Singleton
class ServerManagerImpl @Inject constructor(
    private val upnpService: UpnpService,
    private val mediaServer: MediaServer,
    private val preferencesRepository: PreferencesRepository,
    private val lifecycleManager: LifecycleManager,
    private val controlPoint: ControlPoint,
    private val logger: Logger
) : ServerManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val isServerOn: MutableStateFlow<Boolean> = MutableStateFlow(false)

    init {
        scope.launch {
            lifecycleManager.doOnStart { start() }
        }

        scope.launch {
            lifecycleManager.doOnResume { if (preferencesRepository.isStreaming) resume() }
        }

        scope.launch {
            lifecycleManager.doOnFinish { shutdown() }
        }

        scope.launch {
            preferencesRepository
                .preferences
                .collect { preferences ->
                    when (preferences.applicationMode) {
                        Preferences.ApplicationMode.STREAMING -> resume()
                        else -> pause()
                    }
                }
        }

        scope.launch {
            isServerOn.onEach { isOn ->
                if (isOn) {
                    scope.launch {
                        (upnpService as AndroidUpnpServiceImpl).addLocalDevice()
                        runCatching { mediaServer.start() }.onFailure {
                            logger.e(it, "Failed to start media server!")
                        }
                    }
                } else {
                    scope.launch {
                        (upnpService as AndroidUpnpServiceImpl).removeLocalDevice()
                        runCatching { mediaServer.stop() }.onFailure {
                            logger.e(it, "Failed to stop media server!")
                        }
                    }
                }
            }
                .onEach { controlPoint.search() }
                .collect()
        }
    }

    override fun start() {
        (upnpService as AndroidUpnpServiceImpl).start()
    }

    override fun resume() {
        isServerOn.value = true
    }

    override fun pause() {
        isServerOn.value = false
    }

    override fun shutdown() {
        thread {
            upnpService.shutdown()
            mediaServer.stop()
            lifecycleManager.close()
        }
    }
}

