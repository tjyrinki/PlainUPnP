package com.m3sv.plainupnp.upnp.util

import com.m3sv.plainupnp.upnp.mediacontainers.BaseContainer
import org.fourthline.cling.support.model.DIDLObject
import org.fourthline.cling.support.model.PersonWithRole
import org.fourthline.cling.support.model.Res
import org.fourthline.cling.support.model.item.ImageItem
import org.fourthline.cling.support.model.item.MusicTrack
import org.fourthline.cling.support.model.item.VideoItem
import org.seamless.util.MimeType

fun BaseContainer.addImageItem(
    baseUrl: String,
    id: String,
    name: String,
    mime: String,
    width: Long,
    height: Long,
    size: Long,
): DIDLObject {
    val (type, subtype) = mime.split('/')

    val res = Res(
        MimeType(type, subtype),
        size,
        "http://$baseUrl/$id.$subtype"
    ).apply {
        setResolution(width.toInt(), height.toInt())
    }

    val imageItem = ImageItem(
        id,
        rawId,
        name,
        "",
        res
    )
    addItem(imageItem)
    return imageItem
}

fun BaseContainer.addVideoItem(
    baseUrl: String,
    id: String,
    name: String,
    mime: String,
    width: Long,
    height: Long,
    size: Long,
    duration: Long,
): DIDLObject {
    val (type, subtype) = mime.split('/')

    val res = Res(
        MimeType(type, subtype),
        size,
        "http://$baseUrl/$id.$subtype"
    ).also { res ->
        res.duration =
            "${duration / (1000 * 60 * 60)}:${duration % (1000 * 60 * 60) / (1000 * 60)}:${duration % (1000 * 60) / 1000}"
        res.setResolution(width.toInt(), height.toInt())

    }

    val videoItem = VideoItem(id, rawId, name, "", res)
    addItem(videoItem)
    return videoItem
}

fun BaseContainer.addAudioItem(
    baseUrl: String,
    id: String,
    name: String,
    mime: String,
    width: Long,
    height: Long,
    size: Long?,
    duration: Long,
    album: String?,
    creator: String?,
): DIDLObject {
    val (type, subtype) = mime.split('/')

    val res = Res(
        MimeType(type, subtype),
        size,
        "http://$baseUrl/$id.$subtype"
    ).also { res ->
        res.duration =
            "${duration / (1000 * 60 * 60)}:${duration % (1000 * 60 * 60) / (1000 * 60)}:${duration % (1000 * 60) / 1000}"
        res.setResolution(width.toInt(), height.toInt())
    }

    val musicTrack = MusicTrack(
        id,
        rawId,
        name,
        creator,
        album,
        PersonWithRole(creator, "Performer"),
        res
    )
    addItem(
        musicTrack
    )
    return musicTrack
}

