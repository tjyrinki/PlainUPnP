package com.m3sv.plainupnp.upnp.mediacontainers

import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.m3sv.plainupnp.common.util.isQ
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl
import com.m3sv.plainupnp.upnp.util.addAudioItem
import org.fourthline.cling.support.model.container.Container

class AudioDirectoryContainer(
    id: String,
    parentID: String?,
    title: String,
    creator: String?,
    artist: String? = null,
    albumId: String? = null,
    private val baseUrl: String,
    private val directory: ContentDirectory,
    private val contentResolver: ContentResolver,
) : BaseContainer(id, parentID, title, creator) {

    private var orderBy: String? = null

    private var selection: String = "$AUDIO_DATA_PATH LIKE ?"

    private var selectionArgs: Array<String> = arrayOf("%${directory.name}/${if (isQ) "" else "%"}")

    init {
        val directoryClause = " ,$selection"

        if (artist != null) {
            selection = "${MediaStore.Audio.Media.ARTIST} = ?${directoryClause}"
            selectionArgs = arrayOf(artist, "%$directory/")
            orderBy = MediaStore.Audio.Media.ALBUM
        }

        if (albumId != null) {
            selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?${directoryClause}"
            selectionArgs = arrayOf(albumId, "%$directory/")
            orderBy = MediaStore.Audio.Media.TRACK
        }
    }

    override fun getChildCount(): Int {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            AUDIO_DATA_PATH
        )

        contentResolver.query(
            URI,
            projection,
            selection,
            selectionArgs,
            orderBy
        ).use { cursor ->
            return cursor?.count ?: 0
        }
    }

    override fun getContainers(): List<Container> {
        val columns = arrayOf(
            MediaStore.Audio.Media._ID,
            AUDIO_DATA_PATH,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM
        )

        contentResolver.query(
            URI,
            columns,
            selection,
            selectionArgs,
            orderBy
        )?.use { cursor ->
            val audioIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val audioTitleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val audioArtistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val audioMimeTypeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val audioSizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val audioDurationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val audioAlbumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val dataColumn = cursor.getColumnIndex(AUDIO_DATA_PATH)

            while (cursor.moveToNext()) {
                if (!isQ && !directory.samePath(cursor.getString(dataColumn)))
                    continue

                val id = UpnpContentRepositoryImpl.AUDIO_PREFIX + (audioIdColumn.ifExists(cursor::getLong) ?: continue)
                val mimeType = audioMimeTypeColumn.ifExists(cursor::getStringOrNull) ?: continue
                val title = audioTitleColumn.ifExists(cursor::getStringOrNull) ?: TITLE_NOT_FOUND
                val creator = audioArtistColumn.ifExists(cursor::getStringOrNull)
                val size = audioSizeColumn.ifExists(cursor::getLong) ?: 0L
                val duration = audioDurationColumn.ifExists(cursor::getLong) ?: 0L
                val album = audioAlbumColumn.ifExists(cursor::getString)

                addAudioItem(baseUrl, id, title, mimeType, 0L, 0L, size, duration, album, creator)
            }
        }

        return containers
    }

    companion object {
        private const val TITLE_NOT_FOUND = "-"

        private val URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val AUDIO_DATA_PATH = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.RELATIVE_PATH
        else
            MediaStore.Audio.Media.DATA
    }
}
