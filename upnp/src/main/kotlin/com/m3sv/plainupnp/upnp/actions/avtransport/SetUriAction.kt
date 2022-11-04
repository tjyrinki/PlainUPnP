package com.m3sv.plainupnp.upnp.actions.avtransport

import com.m3sv.plainupnp.logging.Logger
import com.m3sv.plainupnp.upnp.trackmetadata.TrackMetadata
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SetUriAction @Inject constructor(private val controlPoint: ControlPoint, private val logger: Logger) {

    suspend fun setUri(
        service: Service<*, *>,
        uri: String,
        trackMetadata: TrackMetadata,
    ): Boolean = suspendCoroutine { continuation ->
        val tag = "AV"
        Timber.tag(tag).d("Set uri: $uri")
        val action = object : SetAVTransportURI(service, uri, trackMetadata.getXml(logger)) {

            override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                Timber.tag(tag).d("Set uri: $uri success")
                continuation.resume(true)
            }

            override fun failure(
                p0: ActionInvocation<out Service<*, *>>?,
                p1: UpnpResponse?,
                p2: String?,
            ) {
                Timber.tag(tag).e("Failed to set uri: $uri")
                continuation.resume(false)
            }
        }

        controlPoint.execute(action)
    }
}
