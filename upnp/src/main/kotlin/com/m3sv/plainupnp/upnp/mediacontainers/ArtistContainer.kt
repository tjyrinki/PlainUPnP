/**
 * Copyright (C) 2013 Aur?lien Chabot <aurelien></aurelien>@chabot.fr>
 *
 *
 * This file is part of DroidUPNP.
 *
 *
 * DroidUPNP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *
 * DroidUPNP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with DroidUPNP.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package com.m3sv.plainupnp.upnp.mediacontainers

import android.content.ContentResolver
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.m3sv.plainupnp.logging.Logger
import org.fourthline.cling.support.model.container.Container

class ArtistContainer(
    id: String,
    parentID: String,
    title: String,
    creator: String,
    private val logger: Logger,
    private val baseUrl: String,
    private val contentResolver: ContentResolver
) : BaseContainer(
    id,
    parentID,
    title,
    creator
) {
    private val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

    override fun getChildCount(): Int {
        val columns = arrayOf(MediaStore.Audio.Artists._ID)

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
        val columns = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST
        )

        contentResolver
            .query(
                uri,
                columns,
                null,
                null,
                null
            )?.use { cursor ->
                val artistsIdColumn = cursor.getColumnIndex(MediaStore.Audio.Artists._ID)
                val artistsColumn = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)

                while (cursor.moveToNext()) {
                    val artistId = artistsIdColumn.ifExists(cursor::getLongOrNull)?.toString() ?: continue
                    val artist = artistsColumn.ifExists(cursor::getStringOrNull) ?: continue

                    containers.add(
                        AlbumContainer(
                            artistId,
                            id,
                            artist,
                            artist,
                            logger,
                            baseUrl,
                            contentResolver,
                            artistId
                        )
                    )
                }
            }

        return containers
    }
}
