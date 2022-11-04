package com.m3sv.plainupnp.upnp.discovery.device


import android.app.Application
import com.m3sv.plainupnp.common.R
import com.m3sv.plainupnp.data.upnp.DeviceDisplay
import com.m3sv.plainupnp.data.upnp.DeviceType
import com.m3sv.plainupnp.data.upnp.LocalDevice
import com.m3sv.plainupnp.data.upnp.UpnpDevice
import com.m3sv.plainupnp.data.upnp.UpnpDeviceEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

class RendererDiscoveryObservable @Inject constructor(
    application: Application,
    private val rendererDiscovery: RendererDiscovery,
) {
    private var _selectedRenderer: MutableStateFlow<UpnpDevice?> = MutableStateFlow(null)

    private val renderers =
        LinkedHashSet<DeviceDisplay>(listOf(DeviceDisplay(LocalDevice(application.getString(R.string.play_locally)))))

    val currentRenderers: Set<DeviceDisplay>
        get() = renderers

    val selectedRenderer: StateFlow<UpnpDevice?> = _selectedRenderer

    val isConnectedToRenderer: Boolean
        get() = selectedRenderer.value != null

    fun selectRenderer(upnpDevice: UpnpDevice?) {
        _selectedRenderer.value = upnpDevice
    }

    operator fun invoke() = callbackFlow {
        rendererDiscovery.startObserving()

        val callback = object : DeviceDiscoveryObserver {
            override fun addedDevice(event: UpnpDeviceEvent) {
                handleEvent(event)
                sendRenderers()
            }

            override fun removedDevice(event: UpnpDeviceEvent) {
                handleEvent(event)
                sendRenderers()
            }

            private fun handleEvent(event: UpnpDeviceEvent) {
                when (event) {
                    is UpnpDeviceEvent.Added -> {
                        Timber.d("Renderer added: ${event.upnpDevice.displayString}")
                        renderers += DeviceDisplay(
                            event.upnpDevice,
                            false,
                            DeviceType.RENDERER
                        )
                    }

                    is UpnpDeviceEvent.Removed -> {
                        Timber.d("Renderer removed: ${event.upnpDevice.displayString}")
                        val device = DeviceDisplay(
                            event.upnpDevice,
                            false,
                            DeviceType.RENDERER
                        )

                        if (renderers.contains(device))
                            renderers -= device

                        if (event.upnpDevice == selectedRenderer.value)
                            _selectedRenderer.value = null
                    }
                }
            }

            private fun sendRenderers() {
                if (!isClosedForSend) trySendBlocking(currentRenderers)
            }
        }

        rendererDiscovery.addObserver(callback)
        send(currentRenderers)
        awaitClose { rendererDiscovery.removeObserver(callback) }
    }
}
