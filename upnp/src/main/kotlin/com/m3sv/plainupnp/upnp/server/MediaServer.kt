package com.m3sv.plainupnp.upnp.server

import android.app.Activity
import android.app.Application
import android.content.ContentUris
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.m3sv.plainupnp.ContentModel
import com.m3sv.plainupnp.ContentRepository
import com.m3sv.plainupnp.logging.Logger
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl.Companion.AUDIO_PREFIX
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl.Companion.IMAGE_PREFIX
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl.Companion.TREE_PREFIX
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl.Companion.VIDEO_PREFIX
import com.m3sv.plainupnp.upnp.util.PORT
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaServer @Inject constructor(
    private val application: Application,
    private val logger: Logger,
    private val contentRepository: ContentRepository,
) : SimpleInputStreamServer(null, PORT, listOf(), true) {

    private val serverScope = CoroutineScope(Executors.newFixedThreadPool(16).asCoroutineDispatcher())

    init {
        setAsyncRunner(object : AsyncRunner {
            private val running = mutableListOf<ClientHandler>()
            private val mutex = Mutex()

            override fun closeAll() {
                serverScope.launch { mutex.withLock { running.forEach { it.close() } } }
            }

            override fun closed(clientHandler: ClientHandler) {
                serverScope.launch {
                    mutex.withLock { running.remove(clientHandler) }
                }
            }

            override fun exec(code: ClientHandler) {
                serverScope.launch {
                    mutex.withLock { running.add(code) }
                    runCatching { code.run() }.onFailure { logger.e(it, "ClientHandler failed") }
                }
            }
        })

    }

    override fun serve(session: IHTTPSession): Response = try {
        Timber.i("Received request: ${session.uri}")
        val obj = getFileServerObject(session.uri)

        Timber.i("Will serve: %s", obj)
        Timber.i("Headers: ${session.headers}")

        serveFile(
            obj.id,
            session.headers,
            obj.inputStream,
            obj.mime
        ).apply {
            addHeader("realTimeInfo.dlna.org", "DLNA.ORG_TLAG=*")
            addHeader("contentFeatures.dlna.org", "")
            addHeader("transferMode.dlna.org", "Streaming")
            addHeader(
                "Server",
                "DLNADOC/1.50 UPnP/1.0 Cling/2.0 PlainUPnP/" + "0.0" + " Android/" + Build.VERSION.RELEASE
            )
        }
    } catch (e: InvalidIdentifierException) {
        val stream = "Error 404, file not found.".byteInputStream(StandardCharsets.UTF_8)
        NanoHTTPD.newFixedLengthResponse(
            Response.Status.NOT_FOUND,
            MIME_PLAINTEXT,
            stream,
            stream.available().toLong()
        )
    }

    override fun start() {
        super.start()
        logger.d("Starting server at http://${getDefaultIpAddresses(application.getSystemService(Activity.CONNECTIVITY_SERVICE) as ConnectivityManager)}:$PORT")
    }

    // Ignore older versions, we're getting rid of them in the future anyway
    private fun getDefaultIpAddresses(cm: ConnectivityManager): String? {
        val prop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.getLinkProperties(cm.activeNetwork)
        } else {
            null
        } ?: return null

        return formatIpAddresses(prop)
    }

    private fun formatIpAddresses(prop: LinkProperties): String? = prop
        .linkAddresses
        .find { linkAddress -> linkAddress.address.hostAddress.contains("192.168") }
        ?.address
        ?.hostAddress

    inner class InvalidIdentifierException(message: String) : java.lang.Exception(message)

    private fun getFileServerObject(uri: String): ServerObject {
        try {
            // Remove extension
            val id = uri.replace("/", "").split(".").first()
            val mediaId = id.substring(2)

            return when {
                id.startsWith(AUDIO_PREFIX) -> getContainerResponse(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mediaId
                )
                id.startsWith(VIDEO_PREFIX) -> getContainerResponse(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    mediaId
                )
                id.startsWith(IMAGE_PREFIX) -> getContainerResponse(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    mediaId
                )
                id.startsWith(TREE_PREFIX) -> getTreeResponse(mediaId)
                else -> error("Unknown content type")
            }

        } catch (e: Exception) {
            logger.e(e, "Error while parsing $uri")
        }

        throw InvalidIdentifierException("$uri was not found in media database").apply(logger::e)
    }

    private fun getTreeResponse(mediaId: String): ServerObject {
        val model: ContentModel = contentRepository.contentCache[mediaId.toLong()] ?: error("Not found")
        val fileInputStream =
            application.contentResolver.openInputStream(model.uri) ?: error("Failed to open file input stream")
        return ServerObject(mediaId, model.mimeType, fileInputStream)
    }

    private fun getContainerResponse(contentUri: Uri, mediaId: String): ServerObject {
        val columns = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.MIME_TYPE
        )

        val whereVal = arrayOf(mediaId)

        application
            .contentResolver
            .query(
                contentUri, columns,
                WHERE_CLAUSE, whereVal, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fileId =
                        cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID))

                    val mime =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))

                    val fileUri = ContentUris.withAppendedId(contentUri, fileId)
                    val inputStream = application.contentResolver.openInputStream(fileUri)

                    return ServerObject(
                        fileUri.toString(),
                        mime,
                        inputStream!!
                    )
                }
            }

        error("Object with id $mediaId not found")
    }

    companion object {
        private const val WHERE_CLAUSE = MediaStore.MediaColumns._ID + "=?"
    }

    data class ServerObject(val id: String, val mime: String, val inputStream: InputStream)
}


