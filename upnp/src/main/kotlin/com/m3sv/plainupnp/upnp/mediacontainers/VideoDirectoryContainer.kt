package com.m3sv.plainupnp.upnp.mediacontainers

import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.m3sv.plainupnp.common.util.isQ
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl
import org.fourthline.cling.support.model.Res
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.item.VideoItem
import org.seamless.util.MimeType

class VideoDirectoryContainer(
    id: String,
    parentID: String?,
    title: String,
    creator: String,
    private val baseUrl: String,
    private val directory: ContentDirectory,
    private val contentResolver: ContentResolver,
) : BaseContainer(
    id,
    parentID,
    title,
    creator
) {
    private val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    private val selection: String = "$VIDEO_DATA_PATH LIKE ?"

    private val selectionArgs: Array<String> = arrayOf("%${directory.name}/${if (isQ) "" else "%"}")

    override fun getChildCount(): Int {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            VIDEO_DATA_PATH
        )

        contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        ).use { cursor ->
            return cursor?.count ?: 0
        }
    }

    override fun getContainers(): List<Container> {
        if (items.isNotEmpty() || containers.isNotEmpty())
            return containers

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            VIDEO_DATA_PATH,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.WIDTH
        )

        contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val videoIdColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID)
            val videoTitleColumn = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
            val videoArtistColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
            val videoMimeTypeColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val videoSizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val videoDurationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val videoHeightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val videoWidthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val dataColumn = cursor.getColumnIndex(VIDEO_DATA_PATH)

            while (cursor.moveToNext()) {
                if (!isQ && !directory.samePath(cursor.getString(dataColumn)))
                    continue

                val id = UpnpContentRepositoryImpl.VIDEO_PREFIX +
                        (videoIdColumn.ifExists(cursor::getLongOrNull) ?: continue)

                val mimeType = videoMimeTypeColumn.ifExists(cursor::getStringOrNull) ?: continue
                val title = videoTitleColumn.ifExists(cursor::getString) ?: DEFAULT_NOT_FOUND
                val creator = videoArtistColumn.ifExists(cursor::getStringOrNull)
                val size = videoSizeColumn.ifExists(cursor::getLongOrNull) ?: 0L
                val videoDuration = videoDurationColumn.ifExists(cursor::getLongOrNull) ?: 0L
                val videoHeight = videoHeightColumn.ifExists(cursor::getLongOrNull) ?: 0L
                val videoWidth = videoWidthColumn.ifExists(cursor::getLongOrNull) ?: 0L

                val mimeTypeType = mimeType.substring(0, mimeType.indexOf('/'))
                val mimeTypeSubType = mimeType.substring(mimeType.indexOf('/') + 1)

                val res = Res(
                    MimeType(
                        mimeTypeType,
                        mimeTypeSubType
                    ),
                    size,
                    "http://$baseUrl/$id.$mimeTypeSubType"
                ).apply {
                    duration =
                        "${videoDuration / (1000 * 60 * 60)}:${videoDuration % (1000 * 60 * 60) / (1000 * 60)}:${videoDuration % (1000 * 60) / 1000}"
                    setResolution(videoWidth.toInt(), videoHeight.toInt())
                }

                addItem(VideoItem(id, parentID, title, creator, res))
            }
        }

        return containers
    }

    companion object {
        val VIDEO_DATA_PATH = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.RELATIVE_PATH
        else
            MediaStore.Video.Media.DATA
    }
}
