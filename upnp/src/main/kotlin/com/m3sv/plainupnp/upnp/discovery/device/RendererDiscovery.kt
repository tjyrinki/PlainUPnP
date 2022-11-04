package com.m3sv.plainupnp.upnp.discovery.device

import com.m3sv.plainupnp.logging.Logger
import com.m3sv.plainupnp.upnp.filters.CallableRendererFilter
import org.fourthline.cling.UpnpService
import javax.inject.Inject

class RendererDiscovery @Inject constructor(upnpService: UpnpService, logger: Logger) :
    DeviceDiscovery(upnpService = upnpService, logger = logger) {
    override val callableFilter: CallableFilter = CallableRendererFilter()
}
