package com.m3sv.plainupnp.upnp.manager


import com.m3sv.plainupnp.common.util.formatTime
import com.m3sv.plainupnp.data.upnp.DeviceDisplay
import com.m3sv.plainupnp.data.upnp.UpnpDevice
import com.m3sv.plainupnp.data.upnp.UpnpItemType
import com.m3sv.plainupnp.data.upnp.UpnpRendererState
import com.m3sv.plainupnp.logging.Logger
import com.m3sv.plainupnp.presentation.SpinnerItem
import com.m3sv.plainupnp.upnp.CDevice
import com.m3sv.plainupnp.upnp.ContentUpdateState
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl
import com.m3sv.plainupnp.upnp.UpnpRepository
import com.m3sv.plainupnp.upnp.didl.ClingContainer
import com.m3sv.plainupnp.upnp.didl.ClingDIDLObject
import com.m3sv.plainupnp.upnp.didl.ClingMedia
import com.m3sv.plainupnp.upnp.didl.MiscItem
import com.m3sv.plainupnp.upnp.discovery.device.ContentDirectoryDiscoveryObservable
import com.m3sv.plainupnp.upnp.discovery.device.RendererDiscoveryObservable
import com.m3sv.plainupnp.upnp.folder.Folder
import com.m3sv.plainupnp.upnp.folder.FolderModel
import com.m3sv.plainupnp.upnp.trackmetadata.TrackMetadata
import com.m3sv.plainupnp.upnp.usecase.LaunchLocallyUseCase
import com.m3sv.plainupnp.upnp.util.*
import com.m3sv.plainupnp.upnp.volume.VolumeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.support.model.PositionInfo
import org.fourthline.cling.support.model.TransportInfo
import org.fourthline.cling.support.model.TransportState
import org.fourthline.cling.support.model.item.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject

private const val MAX_PROGRESS = 100
private const val ROOT_FOLDER_ID = "0"
private const val AV_TRANSPORT = "AVTransport"
private const val RENDERING_CONTROL = "RenderingControl"
private const val CONTENT_DIRECTORY = "ContentDirectory"

sealed interface Result {
    object Success : Result
    enum class Error : Result {
        GENERIC, RENDERER_NOT_SELECTED, AV_SERVICE_NOT_FOUND
    }
}

class UpnpManagerImpl @Inject constructor(
    private val rendererDiscoveryObservable: RendererDiscoveryObservable,
    private val contentDirectoryObservable: ContentDirectoryDiscoveryObservable,
    private val launchLocally: LaunchLocallyUseCase,
    private val upnpRepository: UpnpRepository,
    private val volumeRepository: VolumeRepository,
    private val contentRepository: UpnpContentRepositoryImpl,
    private val logger: Logger
) : UpnpManager {
    override val volumeFlow: Flow<Int> = volumeRepository.volumeFlow

    override val isConnectedToRenderer: Flow<Boolean> = rendererDiscoveryObservable
        .selectedRenderer
        .map { it != null }

    private val upnpInnerStateChannel = MutableSharedFlow<UpnpRendererState>()
    override val upnpRendererState: Flow<UpnpRendererState> = upnpInnerStateChannel

    override val contentDirectories: Flow<Set<DeviceDisplay>> = contentDirectoryObservable()
    override val renderers: Flow<Set<DeviceDisplay>> = rendererDiscoveryObservable()

    private var isPlayingLocal: Boolean = false
    private val updateChannel = MutableSharedFlow<Pair<Item, Service<*, *>>?>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            updateChannel.scan(launch { }) { accumulator, pair ->
                accumulator.cancel()

                if (pair == null)
                    return@scan launch { }

                Timber.d("update: received new pair: ${pair.first}")
                val didlItem = pair.first
                val service = pair.second

                val type = when (didlItem) {
                    is AudioItem -> UpnpItemType.AUDIO
                    is VideoItem -> UpnpItemType.VIDEO
                    else -> UpnpItemType.UNKNOWN
                }

                val title = didlItem.title
                val artist = didlItem.creator
                val uri = didlItem.firstResource?.value ?: error("no uri")

                launch {
                    while (isActive) {
                        delay(500)

                        val transportInfo = async {
                            runCatching { upnpRepository.getTransportInfo(service) }.getOrNull()
                        }

                        val positionInfo = async {
                            runCatching { upnpRepository.getPositionInfo(service) }.getOrNull()
                        }

                        suspend fun processInfo(transportInfo: TransportInfo, positionInfo: PositionInfo) {
                            remotePaused = transportInfo.currentTransportState == TransportState.PAUSED_PLAYBACK

                            val state = UpnpRendererState.Default(
                                uri = uri,
                                type = type,
                                state = transportInfo.currentTransportState,
                                remainingDuration = positionInfo.remainingDuration,
                                duration = positionInfo.duration,
                                position = positionInfo.position,
                                elapsedPercent = positionInfo.elapsedPercent,
                                durationSeconds = positionInfo.trackDurationSeconds,
                                title = title,
                                artist = artist ?: ""
                            )

                            currentDuration = positionInfo.trackDurationSeconds

                            if (!pauseUpdate) upnpInnerStateChannel.emit(state)

                            Timber.d("Got new state: $state")

                            if (transportInfo.currentTransportState == TransportState.STOPPED) {
                                upnpInnerStateChannel.emit(UpnpRendererState.Empty)
                                cancel()
                            }
                        }

                        processInfo(
                            transportInfo.await() ?: return@launch,
                            positionInfo.await() ?: return@launch
                        )
                    }
                }
            }.collect()
        }

        scope.launch {
            contentRepository.refreshState.collect {
                if (it is ContentUpdateState.Ready) {
                    val contentDirectory = contentDirectoryObservable.selectedContentDirectory

                    if (contentDirectory != null) {
                        safeNavigateTo(
                            folderId = ROOT_FOLDER_ID,
                            folderName = contentDirectory.friendlyName
                        )
                    }
                }
            }
        }
    }

    override suspend fun selectContentDirectory(upnpDevice: UpnpDevice): Result = withContext(Dispatchers.IO) {
        folderStack.value = listOf()
        contentCache.clear()
        contentDirectoryObservable.selectedContentDirectory = upnpDevice

        safeNavigateTo(
            folderId = ROOT_FOLDER_ID,
            folderName = upnpDevice.friendlyName
        )
    }

    override suspend fun selectRenderer(spinnerItem: SpinnerItem) {
        withContext(Dispatchers.IO) {
            val renderer: UpnpDevice = spinnerItem.deviceDisplay.upnpDevice

            isPlayingLocal = renderer.isLocal

            if (isPlayingLocal || renderer != rendererDiscoveryObservable.selectedRenderer.value)
                stopUpdate()

            if (!isPlayingLocal) {
                rendererDiscoveryObservable.selectRenderer(renderer)
            } else {
                rendererDiscoveryObservable.selectRenderer(null)
            }
        }
    }

    private suspend fun renderItem(item: RenderItem): Result = withContext(Dispatchers.IO) {
        stopUpdate()

        if (isPlayingLocal) {
            launchLocally(item)
            return@withContext Result.Success
        }

        if (!rendererDiscoveryObservable.isConnectedToRenderer) {
            return@withContext Result.Error.RENDERER_NOT_SELECTED
        }

        val result = runCatching {
            withAvService { service ->
                val didlItem = item.didlItem.didlObject as Item
                val uri = didlItem.firstResource?.value ?: error("First resource or its value is null!")
                val didlType = when (didlItem) {
                    is AudioItem -> "audioItem"
                    is VideoItem -> "videoItem"
                    is ImageItem -> "imageItem"
                    is PlaylistItem -> "playlistItem"
                    is TextItem -> "textItem"
                    else -> null
                }

                val newMetadata = with(didlItem) {
                    // TODO genre && artURI
                    TrackMetadata(
                        id,
                        title,
                        creator,
                        "",
                        "",
                        firstResource.value,
                        "object.item.$didlType"
                    )
                }

                upnpRepository.setUri(service, uri, newMetadata)
                upnpRepository.play(service)

                when (didlItem) {
                    is AudioItem,
                    is VideoItem,
                    -> updateChannel.emit(didlItem to service)
                    is ImageItem -> upnpInnerStateChannel.emit(UpnpRendererState.Empty)
                }
                Result.Success
            } ?: Result.Error.AV_SERVICE_NOT_FOUND
        }

        if (result.isSuccess)
            result.getOrThrow()
        else
            Result.Error.GENERIC
    }

    private var currentDuration: Long = 0L

    private var pauseUpdate = false

    private var remotePaused = false

    private suspend fun stopUpdate() {
        withContext(Dispatchers.IO) {
            if (rendererDiscoveryObservable.isConnectedToRenderer) {
                stopPlayback()
                updateChannel.emit(null)
            }
        }
    }

    private var currentIndex: Int = -1

    override suspend fun playNext() {
        withContext(Dispatchers.IO) {
            if (currentIndex in 0 until currentContent.value.size - 1)
                renderItem(RenderItem(currentContent.value[++currentIndex]))
        }
    }

    override suspend fun itemClick(id: String): Result = withContext(Dispatchers.IO) {
        val item: ClingDIDLObject = contentCache[id] ?: return@withContext Result.Error.GENERIC

        when (item) {
            is ClingContainer -> safeNavigateTo(folderId = id, folderName = item.title)
            is ClingMedia -> playItem(item)
            is MiscItem -> Result.Error.GENERIC
        }
    }

    private suspend fun playItem(item: ClingDIDLObject): Result {
        currentIndex = currentContent.value.indexOf(item)
        return renderItem(RenderItem(item))
    }

    override suspend fun playPrevious() {
        withContext(Dispatchers.IO) {
            if (currentIndex in 1 until currentContent.value.size)
                renderItem(RenderItem(currentContent.value[--currentIndex]))
        }
    }

    override suspend fun pausePlayback() {
        withContext(Dispatchers.IO) {
            runCatching {
                avService?.let { service -> upnpRepository.pause(service) }
            }.onFailure { logger.e(it, "Failed to pause playback") }
        }
    }

    override suspend fun stopPlayback() {
        withContext(Dispatchers.IO) {
            runCatching {
                avService?.let { service -> upnpRepository.stop(service) }
            }
        }
    }

    override suspend fun resumePlayback() {
        withContext(Dispatchers.IO) {
            runCatching {
                avService?.let { service -> upnpRepository.play(service) }
            }.onFailure { logger.e(it, "Failed to resume playback") }
        }
    }

    override suspend fun seekTo(progress: Int) {
        withContext(Dispatchers.IO) {
            runCatching {
                avService?.let { service ->
                    pauseUpdate = true
                    upnpRepository.seekTo(
                        service = service,
                        time = formatTime(
                            max = MAX_PROGRESS,
                            progress = progress,
                            duration = currentDuration
                        )
                    )
                    pauseUpdate = false
                }
            }.onFailure { logger.e(it, "Failed to seek progress $progress") }
        }
    }

    override suspend fun raiseVolume(step: Int) {
        withContext(Dispatchers.IO) {
            runCatching {
                withRcService { service -> volumeRepository.raiseVolume(service, step) }
            }.onFailure { logger.e(it, "Failed to raise volume with step $step") }
        }
    }

    override suspend fun lowerVolume(step: Int) {
        withContext(Dispatchers.IO) {
            runCatching {
                withRcService { service -> volumeRepository.lowerVolume(service, step) }
            }.onFailure { logger.e(it, "Failed to lower volume with step $step") }
        }
    }

    override suspend fun muteVolume(mute: Boolean) {
        withContext(Dispatchers.IO) {
            runCatching {
                withRcService { service -> volumeRepository.muteVolume(service, mute) }
            }.onFailure { logger.e(it, "Failed to mute volume $mute") }
        }
    }

    override suspend fun setVolume(volume: Int) {
        withContext(Dispatchers.IO) {
            runCatching {
                withRcService { service -> volumeRepository.setVolume(service, volume) }
            }.onFailure { logger.e(it, "Failed to set volume with value $volume") }
        }
    }

    override suspend fun getVolume(): Int = withContext(Dispatchers.IO) {
        val result = runCatching {
            withRcService { service -> volumeRepository.getVolume(service) } ?: 0
        }.onFailure { logger.e(it, "Failed to get volume") }

        if (result.isSuccess)
            result.getOrThrow()
        else
            0
    }

    override suspend fun navigateTo(folder: Folder) {
        withContext(Dispatchers.IO) {
            val index = folderStack.value.indexOf(folder)

            if (index == -1) {
                logger.e("Folder $folder isn't found in navigation stack!")
                return@withContext
            }

            folderStack.value = folderStack.value.subList(0, index + 1)
        }
    }

    override suspend fun navigateBack() {
        folderStack.value = folderStack.value.dropLast(1)
    }

    override suspend fun togglePlayback() {
        withContext(Dispatchers.IO) {
            runCatching {
                withAvService { service ->
                    if (remotePaused)
                        upnpRepository.play(service)
                    else
                        upnpRepository.pause(service)
                }
            }.onFailure { logger.e(it, "Failed to toggle playback, is remote paused? $remotePaused") }
        }
    }

    private val contentCache: MutableMap<String, ClingDIDLObject> = mutableMapOf()

    private var currentContent: MutableStateFlow<List<ClingDIDLObject>> = MutableStateFlow(listOf())

    private val folderStack: MutableStateFlow<List<Folder>> = MutableStateFlow(listOf())

    override val navigationStack: Flow<List<Folder>> = folderStack.onEach { folders ->
        if (folders.isEmpty()) {
            stopUpdate()
            rendererDiscoveryObservable.selectRenderer(null)
        }
    }

    private suspend inline fun safeNavigateTo(
        folderId: String,
        folderName: String,
    ): Result = withContext(Dispatchers.IO) {
        Timber.d("Navigating to $folderId with name $folderName")

        val selectedDevice = contentDirectoryObservable.selectedContentDirectory

        if (selectedDevice == null) {
            logger.e("Selected content directory is null!")
            return@withContext Result.Error.GENERIC
        }

        val service: Service<*, *>? =
            (selectedDevice as CDevice).device.findService(UDAServiceType(CONTENT_DIRECTORY))

        if (service == null || !service.hasActions()) {
            logger.e("Service is null or has no actions")
            return@withContext Result.Error.GENERIC
        }

        try {
            currentContent.value = upnpRepository.browse(service, folderId)
        } catch (e: Exception) {
            logger.e("Failed to browse")
            return@withContext Result.Error.GENERIC
        }

        contentCache.putAll(currentContent.value.associateBy { it.id })
        val currentFolderName = folderName.replace(UpnpContentRepositoryImpl.USER_DEFINED_PREFIX, "")

        val folder = when (folderId) {
            ROOT_FOLDER_ID -> Folder.Root(
                FolderModel(
                    id = folderId,
                    title = currentFolderName,
                    contents = currentContent.value
                )
            )
            else -> Folder.SubFolder(
                FolderModel(
                    id = folderId,
                    title = currentFolderName,
                    contents = currentContent.value
                )
            )
        }

        folderStack.value = when (folder) {
            is Folder.Root -> listOf(folder)
            is Folder.SubFolder -> folderStack
                .value
                .toMutableList()
                .apply { add(folder) }
                .toList()
        }

        Result.Success
    }


    private val avService: Service<*, *>?
        get() = rendererDiscoveryObservable
            .selectedRenderer
            .value
            ?.let { renderer ->
                val service: Service<*, *> = (renderer as CDevice)
                    .device
                    .findService(UDAServiceType(AV_TRANSPORT))
                    ?: return null

                if (service.hasActions()) {
                    service
                } else {
                    null
                }
            }

    private val rcService: Service<*, *>?
        get() = rendererDiscoveryObservable
            .selectedRenderer
            .value?.let { renderer ->
                val service: Service<*, *> = (renderer as CDevice)
                    .device
                    .findService(UDAServiceType(RENDERING_CONTROL))
                    ?: return null

                if (service.hasActions())
                    service
                else {
                    null
                }
            }

    private suspend fun <T> withAvService(block: suspend (Service<*, *>) -> T): T? {
        return when (val avService = avService) {
            null -> {
                logger.e("Av service is not found!")
                null
            }
            else -> block(avService)
        }
    }

    private suspend fun <T> withRcService(block: suspend (Service<*, *>) -> T): T? {
        return when (val rcService = rcService) {
            null -> {
                logger.e("Rc service is not found!")
                null
            }
            else -> block(rcService)
        }
    }
}

@JvmInline
value class RenderItem(val didlItem: ClingDIDLObject)

