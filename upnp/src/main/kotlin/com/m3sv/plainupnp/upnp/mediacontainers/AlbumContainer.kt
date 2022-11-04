package com.m3sv.plainupnp.upnp.mediacontainers

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.core.database.getStringOrNull
import com.m3sv.plainupnp.logging.Logger
import org.fourthline.cling.support.model.container.Container
import timber.log.Timber

class AlbumContainer(
    id: String,
    parentID: String,
    title: String,
    creator: String,
    private val logger: Logger,
    private val baseUrl: String,
    private val contentResolver: ContentResolver,
    private val artistId: String?
) : BaseContainer(id, parentID, title, creator) {

    private val artist: String? = null

    private val uri: Uri = artistId?.let(::getContentUri) ?: MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

    private fun getContentUri(it: String) = MediaStore.Audio.Artists.Albums.getContentUri("external", it.toLong())

    override fun getChildCount(): Int {
        val columns: Array<String> = if (artistId == null)
            arrayOf(MediaStore.Audio.Albums._ID)
        else
            arrayOf(MediaStore.Audio.Artists.Albums.ALBUM)

        contentResolver
            .query(
                uri,
                columns,
                null,
                null,
                null
            ).use { cursor ->
                return cursor?.count ?: 0
            }
    }

    override fun getContainers(): List<Container> {
        Timber.d("Get albums!")

        val containers = mutableListOf<Container>()

        val columns: Array<String> = if (artistId == null)
            arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM)
        else
            arrayOf(MediaStore.Audio.Artists.Albums.ALBUM)

        contentResolver.query(
            uri,
            columns,
            null,
            null,
            null
        )?.use { cursor ->
            val albumFactory: () -> Pair<String?, String?>

            if (artistId == null) {
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Albums._ID)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)

                albumFactory = {
                    val albumId = albumIdColumn.ifExists(cursor::getLong)?.toString()
                    val album = albumColumn.ifExists(cursor::getStringOrNull)

                    albumId to album
                }
            } else {
                val artistsColumn = cursor.getColumnIndex(MediaStore.Audio.Artists.Albums.ALBUM)

                albumFactory = {
                    val albumId = artistsColumn.ifExists(cursor::getStringOrNull)
                    val album = albumId?.let(::resolveAlbumId)

                    albumId to album
                }
            }

            while (cursor.moveToNext()) {
                val (albumId, album) = albumFactory()

                if (albumId != null && album != null) {
                    logger.d(" Adding album $id albumId:$albumId album:$album")
                    containers.add(
                        AllAudioContainer(
                            id = albumId,
                            parentID = id,
                            title = album,
                            creator = artist,
                            baseUrl = baseUrl,
                            contentResolver = contentResolver,
                            artist = null,
                            albumId = albumId
                        )
                    )
                } else {
                    logger.e("Unable to get albumId or album")
                }
            }
        }

        return containers
    }

    private fun resolveAlbumId(album: String): String {
        var result = ""
        val columns = arrayOf(MediaStore.Audio.Albums._ID)
        val where = MediaStore.Audio.Albums.ALBUM + "=?"
        val whereVal = arrayOf(album)

        contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            columns,
            where,
            whereVal, null
        )?.use { cursor ->
            if (cursor.moveToFirst())
                result = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID)).toString()
        }

        return result
    }
}
