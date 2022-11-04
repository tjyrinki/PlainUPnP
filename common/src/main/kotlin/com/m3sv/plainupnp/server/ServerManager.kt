package com.m3sv.plainupnp.server

interface ServerManager {
    fun start()
    fun resume()
    fun pause()
    fun shutdown()
}
