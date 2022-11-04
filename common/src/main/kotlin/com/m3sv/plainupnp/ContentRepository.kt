package com.m3sv.plainupnp

import android.net.Uri
import org.fourthline.cling.support.model.DIDLObject

data class ContentModel(
    val uri: Uri,
    val mimeType: String,
    val displayName: String,
    val upnpObject: DIDLObject
)

interface ContentRepository {
    val contentCache: Map<Long, ContentModel>
    fun refreshContent()
    fun init()
}
