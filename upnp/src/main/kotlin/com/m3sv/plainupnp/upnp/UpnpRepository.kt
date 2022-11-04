package com.m3sv.plainupnp.upnp

import com.m3sv.plainupnp.upnp.actions.avtransport.*
import com.m3sv.plainupnp.upnp.actions.misc.BrowseAction
import com.m3sv.plainupnp.upnp.trackmetadata.TrackMetadata
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.support.model.PositionInfo
import org.fourthline.cling.support.model.TransportInfo
import javax.inject.Inject

class UpnpRepository @Inject constructor(
    private val stopAction: StopAction,
    private val pauseAction: PauseAction,
    private val playAction: PlayAction,
    private val setUriAction: SetUriAction,
    private val seekToAction: SeekAction,
    private val getTransportInfoAction: GetTransportInfoAction,
    private val getPositionInfoAction: GetPositionInfoAction,
    private val browseAction: BrowseAction,
) {
    suspend fun browse(
        service: Service<*, *>,
        directoryID: String,
    ) = browseAction(service, directoryID)

    suspend fun getPositionInfo(service: Service<*, *>): PositionInfo {
        return getPositionInfoAction.getPositionInfo(service)
    }

    suspend fun getTransportInfo(service: Service<*, *>): TransportInfo {
        return getTransportInfoAction.getTransportInfo(service)
    }

    suspend fun seekTo(service: Service<*, *>, time: String) {
        seekToAction.seekTo(service, time)
    }

    suspend fun stop(service: Service<*, *>) {
        stopAction.stop(service)
    }

    suspend fun pause(service: Service<*, *>) {
        pauseAction.pause(service)
    }

    suspend fun play(service: Service<*, *>) {
        playAction.play(service)
    }

    suspend fun setUri(service: Service<*, *>, uri: String, metadata: TrackMetadata) {
        setUriAction.setUri(service, uri, metadata)
    }
}
