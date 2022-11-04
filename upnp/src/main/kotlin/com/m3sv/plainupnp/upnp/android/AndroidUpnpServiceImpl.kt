package com.m3sv.plainupnp.upnp.android

import android.app.Application
import android.content.Context
import com.m3sv.plainupnp.common.ApplicationMode
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.common.util.asApplicationMode
import com.m3sv.plainupnp.logging.Logger
import com.m3sv.plainupnp.upnp.ContentDirectoryService
import com.m3sv.plainupnp.upnp.PlainUpnpServiceConfiguration
import com.m3sv.plainupnp.upnp.R
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl
import com.m3sv.plainupnp.upnp.resourceproviders.LocalServiceResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.fourthline.cling.UpnpService
import org.fourthline.cling.UpnpServiceConfiguration
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder
import org.fourthline.cling.controlpoint.ControlPoint
import org.fourthline.cling.controlpoint.ControlPointImpl
import org.fourthline.cling.model.DefaultServiceManager
import org.fourthline.cling.model.meta.DeviceDetails
import org.fourthline.cling.model.meta.DeviceIdentity
import org.fourthline.cling.model.meta.Icon
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.LocalService
import org.fourthline.cling.model.meta.ManufacturerDetails
import org.fourthline.cling.model.meta.ModelDetails
import org.fourthline.cling.model.types.UDADeviceType
import org.fourthline.cling.protocol.ProtocolFactory
import org.fourthline.cling.protocol.ProtocolFactoryImpl
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryImpl
import org.fourthline.cling.transport.Router
import org.fourthline.cling.transport.RouterException
import org.seamless.util.Exceptions
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidUpnpServiceImpl @Inject constructor(
    application: Application,
    resourceProvider: LocalServiceResourceProvider,
    contentRepository: UpnpContentRepositoryImpl,
    private val logger: Logger,
    private val preferencesRepository: PreferencesRepository,
) : UpnpService {

    private val _protocolFactory: ProtocolFactory by lazy { ProtocolFactoryImpl(this) }
    private val _registry: Registry by lazy { RegistryImpl(this) }
    private val _controlPoint: ControlPoint by lazy {
        ControlPointImpl(configuration, _protocolFactory, _registry)
    }

    private val _configuration = PlainUpnpServiceConfiguration()

    private val router: Router = AndroidRouter(configuration, _protocolFactory, application)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun getProtocolFactory(): ProtocolFactory = _protocolFactory

    init {
        scope.launch {
            preferencesRepository
                .preferences
                .filterNotNull()
                .map { it.applicationMode.asApplicationMode() }
                .collect { applicationMode ->
                    try {
                        when (applicationMode) {
                            ApplicationMode.Streaming -> registry.addDevice(localDevice)
                            ApplicationMode.Player -> registry.removeDevice(localDevice)
                        }
                    } catch (e: Exception) {
                        logger.e(e)
                    }
                }
        }
    }

    fun start() {
        Timber.i(">>> Starting UPnP service...")

        try {
            router.enable()
        } catch (var7: RouterException) {
            throw RuntimeException("Enabling network router failed: $var7", var7)
        }

        Timber.i("<<< UPnP service started successfully")
    }

    override fun getConfiguration(): UpnpServiceConfiguration = _configuration

    override fun getControlPoint(): ControlPoint = _controlPoint

    override fun getRegistry(): Registry = _registry

    override fun getRouter(): Router = router

    @Synchronized
    override fun shutdown() {
        Timber.i(">>> Shutting down UPnP service...")
        shutdownRegistry()
        shutdownRouter()
        shutdownConfiguration()
        Timber.i("<<< UPnP service shutdown completed")
    }

    private val localDevice by lazy {
        getLocalDevice(resourceProvider, application, contentRepository)
    }

    fun addLocalDevice() {
        Timber.d("Resuming upnp service")

        runCatching {
            if (preferencesRepository.isStreaming) {
                registry.addDevice(localDevice)
            }
        }.onFailure(logger::e)
    }

    fun removeLocalDevice() {
        Timber.d("Pause upnp service")

        runCatching {
            if (preferencesRepository.isStreaming) {
                registry.removeDevice(localDevice)
            }
        }.onFailure(logger::e)
    }

    private fun getLocalDevice(
        serviceResourceProvider: LocalServiceResourceProvider,
        context: Context,
        contentRepository: UpnpContentRepositoryImpl,
    ): LocalDevice {
        val details = DeviceDetails(
            serviceResourceProvider.settingContentDirectoryName,
            ManufacturerDetails(
                serviceResourceProvider.appName,
                serviceResourceProvider.appUrl
            ),
            ModelDetails(
                serviceResourceProvider.appName,
                serviceResourceProvider.appUrl,
                serviceResourceProvider.modelNumber,
                serviceResourceProvider.appUrl
            ),
            serviceResourceProvider.appVersion,
            serviceResourceProvider.appVersion
        )

        val validationErrors = details.validate()

        for (error in validationErrors) {
            logger.e("Validation pb for property ${error.propertyName}, error is ${error.message}")
        }

        val type = UDADeviceType("MediaServer", 1)

        val iconInputStream = context.resources.openRawResource(R.raw.ic_launcher_round)

        val icon = Icon(
            "image/png",
            128,
            128,
            32,
            "plainupnp-icon",
            iconInputStream
        )

        return LocalDevice(
            DeviceIdentity(preferencesRepository.getUdn()),
            type,
            details,
            icon,
            getLocalService(contentRepository)
        )
    }

    private fun getLocalService(contentRepository: UpnpContentRepositoryImpl): LocalService<ContentDirectoryService> {
        val serviceBinder = AnnotationLocalServiceBinder()
        val contentDirectoryService =
            serviceBinder.read(ContentDirectoryService::class.java) as LocalService<ContentDirectoryService>

        contentDirectoryService.manager = DefaultServiceManager(
            contentDirectoryService,
            ContentDirectoryService::class.java
        ).also { serviceManager ->
            (serviceManager.implementation as ContentDirectoryService).let { service ->
                service.contentRepository = contentRepository
                service.logger = logger
            }
        }

        return contentDirectoryService
    }


    private fun shutdownRegistry() {
        registry.shutdown()
    }

    private fun shutdownRouter() {
        try {
            getRouter().shutdown()
        } catch (var3: RouterException) {
            val cause = Exceptions.unwrap(var3)
            if (cause is InterruptedException) {
                logger.e(cause, "Router shutdown was interrupted: ${var3.stackTrace}")
            } else {
                logger.e(cause, "Router error on shutdown: $var3")
            }
        }
    }

    private fun shutdownConfiguration() {
        configuration.shutdown()
    }
}

