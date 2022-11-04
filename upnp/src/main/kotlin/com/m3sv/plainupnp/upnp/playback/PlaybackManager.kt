package com.m3sv.plainupnp.upnp.playback

interface PlaybackManager {
    suspend fun resumePlayback()
    suspend fun pausePlayback()
    suspend fun togglePlayback()
    suspend fun stopPlayback()
    suspend fun playNext()
    suspend fun playPrevious()
}
