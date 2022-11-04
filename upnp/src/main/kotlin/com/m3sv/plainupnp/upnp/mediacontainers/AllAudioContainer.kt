package com.m3sv.plainupnp.upnp.mediacontainers

import android.content.ContentResolver
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.m3sv.plainupnp.upnp.UpnpContentRepositoryImpl
import org.fourthline.cling.support.model.PersonWithRole
import org.fourthline.cling.support.model.Res
import org.fourthline.cling.support.model.container.Container
import org.fourthline.cling.support.model.item.MusicTrack
import org.seamless.util.MimeType

class AllAudioContainer(
    id: String,
    parentID: String,
    title: String,
    creator: String?,
    artist: String?,
    albumId: String?,
    private val baseUrl: String,
    private val contentResolver: ContentResolver
) : BaseContainer(id, parentID, title, creator) {

    private val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    private var orderBy: String? = null
    private var where: String? = null
    private var whereVal: Array<String>? = null

    init {
        if (artist != null) {
            where = MediaStore.Audio.Media.ARTIST + "=?"
            whereVal = arrayOf(artist)
            orderBy = MediaStore.Audio.Media.ALBUM
        }

        if (albumId != null) {
            where = MediaStore.Audio.Media.ALBUM_ID + "=?"
            whereVal = arrayOf(albumId)
            orderBy = MediaStore.Audio.Media.TRACK
        }
    }

    override fun getChildCount(): Int? {
        val columns = arrayOf(MediaStore.Audio.Media._ID)

        contentResolver.query(
            uri,
            columns,
            where,
            whereVal,
            orderBy
        ).use { cursor ->
            return cursor?.count ?: 0
        }
    }

    override fun getContainers(): List<Container> {
        val columns = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM
        )

        contentResolver.query(
            uri,
            columns,
            where,
            whereVal,
            orderBy
        )?.use { cursor ->
            val audioIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val audioTitleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val audioArtistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val audioMimeTypeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val audioSizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val audioDurationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val audioAlbumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext()) {
                val id = UpnpContentRepositoryImpl.AUDIO_PREFIX +
                        (audioIdColumn.ifExists(cursor::getLongOrNull) ?: continue)

                val fileMimeType = audioMimeTypeColumn.ifExists(cursor::getStringOrNull) ?: continue
                val title = audioTitleColumn.ifExists(cursor::getStringOrNull) ?: DEFAULT_NOT_FOUND
                val creator = audioArtistColumn.ifExists(cursor::getStringOrNull) ?: DEFAULT_NOT_FOUND
                val size = audioSizeColumn.ifExists(cursor::getLongOrNull) ?: 0L
                val duration = audioDurationColumn.ifExists(cursor::getLongOrNull) ?: 0L
                val album = audioAlbumColumn.ifExists(cursor::getStringOrNull) ?: DEFAULT_NOT_FOUND

                val mimeType = fileMimeType.substring(0, fileMimeType.indexOf('/'))
                val mimeSubType = fileMimeType.substring(fileMimeType.indexOf('/') + 1)

                val res = Res(
                    MimeType(mimeType, mimeSubType),
                    size,
                    "http://$baseUrl/$id.$mimeSubType"
                )

                res.duration =
                    "${(duration / (1000 * 60 * 60))}:${duration % (1000 * 60 * 60) / (1000 * 60)}:${duration % (1000 * 60) / 1000}"

                addItem(
                    MusicTrack(
                        id,
                        parentID,
                        title,
                        creator,
                        album,
                        PersonWithRole(creator, "Performer"),
                        res
                    )
                )
            }
        }

        return containers
    }
}
