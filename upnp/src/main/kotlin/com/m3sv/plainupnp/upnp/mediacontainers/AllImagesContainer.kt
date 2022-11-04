package com.m3sv.plainupnp.upnp.mediacontainers

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.m3sv.plainupnp.upnp.util.addImageItem
import com.m3sv.plainupnp.upnp.util.queryImages
import org.fourthline.cling.support.model.container.Container

class AllImagesContainer(
    id: String,
    parentID: String?,
    title: String?,
    creator: String?,
    private val baseUrl: String,
    private val contentResolver: ContentResolver,
) : BaseContainer(id, parentID, title, creator) {

    private val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    override fun getChildCount(): Int = contentResolver.query(
        uri,
        CHILD_COUNT_COLUMNS,
        null,
        null,
        null
    ).use { cursor ->
        return cursor?.count ?: 0
    }

    override fun getContainers(): List<Container> {
        contentResolver.queryImages { id, title, mimeType, size, width, height ->
            addImageItem(baseUrl, id, title, mimeType, width, height, size)
        }

        return containers
    }

    companion object {
        private val CHILD_COUNT_COLUMNS = arrayOf(MediaStore.Images.Media._ID)
    }
}
