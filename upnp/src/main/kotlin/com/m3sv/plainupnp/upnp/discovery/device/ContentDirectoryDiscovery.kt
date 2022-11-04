package com.m3sv.plainupnp.upnp.discovery.device

import com.m3sv.plainupnp.data.upnp.UpnpDevice
import com.m3sv.plainupnp.logging.Logger
import org.fourthline.cling.UpnpService
import javax.inject.Inject

class CallableContentDirectoryFilter : CallableFilter {
    override var device: UpnpDevice? = null

    override fun call(): Boolean? =
        device?.asService("ContentDirectory") ?: false
}

class ContentDirectoryDiscovery @Inject constructor(upnpService: UpnpService, logger: Logger) :
    DeviceDiscovery(upnpService = upnpService, logger = logger) {

    override val callableFilter: CallableFilter = CallableContentDirectoryFilter()
}

