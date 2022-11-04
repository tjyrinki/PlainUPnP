package com.m3sv.plainupnp.upnp.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl

inline fun ContentResolver.queryImages(
    uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
    block: (
        id: String,
        title: String,
        mimeType: String,
        size: Long,
        width: Long,
        height: Long,
    ) -> Unit,
) {
    query(
        uri,
        IMAGE_COLUMNS,
        null,
        null,
        null
    )?.use { cursor ->
        val imagesIdColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val imagesTitleColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val imagesMimeTypeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
        val imagesMediaSizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        val imagesHeightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        val imagesWidthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)

        while (cursor.moveToNext()) {
            val id =
                UpnpContentRepositoryImpl.IMAGE_PREFIX + (imagesIdColumn.ifExists(cursor::getLongOrNull) ?: continue)
            val mime = imagesMimeTypeColumn.ifExists(cursor::getStringOrNull) ?: continue
            val title = imagesTitleColumn.ifExists(cursor::getStringOrNull) ?: "-"
            val size = imagesMediaSizeColumn.ifExists(cursor::getLongOrNull) ?: 0L
            val height = imagesHeightColumn.ifExists(cursor::getLongOrNull) ?: 0L
            val width = imagesWidthColumn.ifExists(cursor::getLongOrNull) ?: 0L

            block(id, title, mime, size, width, height)
        }
    }
}

val IMAGE_COLUMNS = arrayOf(
    MediaStore.Images.Media._ID,
    MediaStore.Images.Media.DISPLAY_NAME,
    MediaStore.Images.Media.MIME_TYPE,
    MediaStore.Images.Media.SIZE,
    MediaStore.Images.Media.HEIGHT,
    MediaStore.Images.Media.WIDTH
)

inline fun ContentResolver.queryVideos(
    uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
    block: (
        id: String,
        title: String,
        creator: String?,
        mimeType: String,
        size: Long,
        duration: Long,
        width: Long,
        height: Long,
    ) -> Unit,
) {
    query(
        uri,
        VIDEO_COLUMNS,
        null,
        null,
        null
    )?.use { cursor ->
        val videoIdColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID)
        val videoTitleColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
        val videoArtistColumn = cursor.getColumnIndex(MediaStore.Video.Media.ARTIST)
        val videoMimeTypeColumn =
            cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
        val videoSizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
        val videoDurationColumn =
            cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
        val videoHeightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
        val videoWidthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)

        while (cursor.moveToNext()) {
            val id = UpnpContentRepositoryImpl.VIDEO_PREFIX +
                    (videoIdColumn.ifExists(cursor::getLongOrNull) ?: continue)

            val mimeType = videoMimeTypeColumn.ifExists(cursor::getStringOrNull) ?: continue
            val title = videoTitleColumn.ifExists(cursor::getStringOrNull) ?: "-"
            val creator = videoArtistColumn.ifExists(cursor::getStringOrNull)
            val size = videoSizeColumn.ifExists(cursor::getLongOrNull) ?: 0L
            val videoDuration = videoDurationColumn.ifExists(cursor::getLongOrNull) ?: 0L
            val videoHeight = videoHeightColumn.ifExists(cursor::getLongOrNull) ?: 0L
            val videoWidth = videoWidthColumn.ifExists(cursor::getLongOrNull) ?: 0L

            block(id, title, creator, mimeType, size, videoDuration, videoWidth, videoHeight)
        }
    }
}

val VIDEO_COLUMNS = arrayOf(
    MediaStore.Video.Media._ID,
    MediaStore.Video.Media.DISPLAY_NAME,
    MediaStore.Video.Media.ARTIST,
    MediaStore.Video.Media.MIME_TYPE,
    MediaStore.Video.Media.SIZE,
    MediaStore.Video.Media.DURATION,
    MediaStore.Video.Media.HEIGHT,
    MediaStore.Video.Media.WIDTH
)

inline fun <T> Int.ifExists(block: (Int) -> T): T? {
    if (this == -1)
        return null

    return block(this)
}
