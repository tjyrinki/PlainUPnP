package com.m3sv.plainupnp.upnp.actions.avtransport

import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo
import org.fourthline.cling.support.model.TransportInfo
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GetTransportInfoAction @Inject constructor(private val controlPoint: ControlPoint) {

    suspend fun getTransportInfo(service: Service<*, *>): TransportInfo = suspendCoroutine { continuation ->
        val action = object : GetTransportInfo(service) {
            override fun received(
                invocation: ActionInvocation<out Service<*, *>>?,
                transportInfo: TransportInfo
            ) {
                continuation.resume(transportInfo)
            }

            override fun failure(
                p0: ActionInvocation<out Service<*, *>>?,
                p1: UpnpResponse?,
                p2: String?
            ) {
                continuation.resumeWithException(IllegalStateException("Failed to get transport info"))
            }
        }

        controlPoint.execute(action)
    }
}

