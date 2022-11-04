package com.m3sv.plainupnp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.common.util.pass
import com.m3sv.plainupnp.data.upnp.UpnpRendererState
import com.m3sv.plainupnp.presentation.SpinnerItem
import com.m3sv.plainupnp.upnp.folder.Folder
import com.m3sv.plainupnp.upnp.manager.Result
import com.m3sv.plainupnp.upnp.manager.UpnpManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VolumeUpdate(val volume: Int) {
    class Show(volume: Int) : VolumeUpdate(volume)
    class Hide(volume: Int) : VolumeUpdate(volume)
}

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val upnpManager: UpnpManager,
    private val volumeManager: BufferedVolumeManager,
) : ViewModel() {

    val isConnectedToRenderer: Flow<Boolean> = upnpManager.isConnectedToRenderer

    val volume: StateFlow<VolumeUpdate> = volumeManager
        .volumeFlow
        .map { volume -> VolumeUpdate.Show(volume) }
        .transformLatest { volumeUpdate ->
            emit(volumeUpdate)
            delay(HIDE_VOLUME_INDICATOR_DELAY)
            emit(VolumeUpdate.Hide(volumeUpdate.volume))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = VolumeUpdate.Hide(-1)
        )

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val loading: StateFlow<Boolean> = _loading

    private val _filterText: MutableStateFlow<String> = MutableStateFlow("")

    val filterText: StateFlow<String> = _filterText

    val navigation: StateFlow<List<Folder>> = upnpManager
        .navigationStack
        .filterNot { it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = listOf()
        )

    val folderContents: StateFlow<FolderContents> = combine(
        navigation.filterNot { it.isEmpty() },
        filterText
    ) { folders, filterText ->
        val newContents = folders
            .last()
            .folderModel
            .contents
            .filter { it.title.contains(filterText, ignoreCase = true) }
            .map { clingObject ->
                ItemViewModel(
                    id = clingObject.id,
                    title = clingObject.title,
                    type = clingObject.toItemType(),
                    uri = clingObject.uri
                )
            }

        FolderContents.Contents(newContents)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            FolderContents.Empty
        )

    val upnpState: StateFlow<UpnpRendererState> = upnpManager
        .upnpRendererState
        .stateIn(viewModelScope, SharingStarted.Eagerly, UpnpRendererState.Empty)

    val renderers: StateFlow<List<SpinnerItem>> = upnpManager
        .renderers
        .map { devices -> devices.map { SpinnerItem(it.upnpDevice.friendlyName, it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf())

    val finishActivityFlow: Flow<Unit> = upnpManager
        .navigationStack
        .filter { it.isEmpty() }
        .map { }

    val showThumbnails: StateFlow<Boolean> = preferencesRepository
        .preferences
        .map { it.enableThumbnails }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    private val _isSelectRendererButtonExpanded: MutableSharedFlow<Boolean> = MutableSharedFlow(1)

    val isSelectRendererButtonExpanded: StateFlow<Boolean> = _isSelectRendererButtonExpanded
        .transformLatest { visible ->
            emit(visible)
            if (visible) {
                delay(5000)
                emit(false)
                collapseSelectRendererDialog()
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isSelectRendererDialogExpanded = MutableStateFlow(false)

    val isSelectRendererDialogExpanded: StateFlow<Boolean> = _isSelectRendererDialogExpanded

    private val _isSettingsDialogExpanded = MutableStateFlow(false)

    val isSettingsDialogExpanded: StateFlow<Boolean> = _isSettingsDialogExpanded

    fun itemClick(id: String) {
        viewModelScope.launch {
            _loading.value = true

            when (upnpManager.itemClick(id)) {
                Result.Error.RENDERER_NOT_SELECTED -> _isSelectRendererButtonExpanded.emit(true)
                Result.Success,
                Result.Error.AV_SERVICE_NOT_FOUND,
                Result.Error.GENERIC -> pass
            }

            _loading.value = false
        }
    }

    suspend fun expandSelectRendererButton() {
        _isSelectRendererButtonExpanded.emit(true)
    }

    suspend fun collapseSelectRendererButton() {
        _isSelectRendererButtonExpanded.emit(false)
    }

    fun expandSelectRendererDialog() {
        _isSelectRendererDialogExpanded.value = true
    }

    fun collapseSelectRendererDialog() {
        _isSelectRendererDialogExpanded.value = false
    }

    fun expandSettingsDialog() {
        _isSettingsDialogExpanded.value = true
    }

    fun collapseSettingsDialog() {
        _isSettingsDialogExpanded.value = false
    }

    fun moveTo(progress: Int) {
        viewModelScope.launch {
            upnpManager.seekTo(progress)
        }
    }

    fun selectRenderer(position: SpinnerItem) {
        viewModelScope.launch { upnpManager.selectRenderer(position) }
    }

    fun playerButtonClick(button: PlayerButton) {
        viewModelScope.launch {
            when (button) {
                PlayerButton.PLAY -> upnpManager.togglePlayback()
                PlayerButton.PREVIOUS -> upnpManager.playPrevious()
                PlayerButton.NEXT -> upnpManager.playNext()
                PlayerButton.RAISE_VOLUME -> volumeManager.raiseVolume()
                PlayerButton.LOWER_VOLUME -> volumeManager.lowerVolume()
                PlayerButton.STOP -> upnpManager.stopPlayback()
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch { upnpManager.navigateBack() }
    }

    fun navigateTo(folder: Folder) {
        viewModelScope.launch { upnpManager.navigateTo(folder) }
    }

    fun filterInput(text: String) {
        viewModelScope.launch {
            _filterText.emit(text)
        }
    }

    fun clearFilterText() {
        filterInput("")
    }

    companion object {
        private const val HIDE_VOLUME_INDICATOR_DELAY: Long = 2500
    }
}
