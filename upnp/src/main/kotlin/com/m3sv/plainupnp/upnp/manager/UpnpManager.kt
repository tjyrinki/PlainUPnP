package com.m3sv.plainupnp.upnp.manager

import com.m3sv.plainupnp.data.upnp.DeviceDisplay
import com.m3sv.plainupnp.data.upnp.UpnpDevice
import com.m3sv.plainupnp.data.upnp.UpnpRendererState
import com.m3sv.plainupnp.presentation.SpinnerItem
import com.m3sv.plainupnp.upnp.folder.Folder
import com.m3sv.plainupnp.upnp.playback.PlaybackManager
import com.m3sv.plainupnp.upnp.volume.UpnpVolumeManager
import kotlinx.coroutines.flow.Flow

interface UpnpManager : UpnpVolumeManager, PlaybackManager {
    val isConnectedToRenderer: Flow<Boolean>
    val renderers: Flow<Set<DeviceDisplay>>
    val contentDirectories: Flow<Set<DeviceDisplay>>
    val upnpRendererState: Flow<UpnpRendererState>
    val navigationStack: Flow<List<Folder>>

    suspend fun navigateBack()
    suspend fun navigateTo(folder: Folder)
    suspend fun itemClick(id: String): Result
    suspend fun seekTo(progress: Int)
    suspend fun selectContentDirectory(upnpDevice: UpnpDevice): Result
    suspend fun selectRenderer(spinnerItem: SpinnerItem)
}
