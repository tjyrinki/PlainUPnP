package com.m3sv.plainupnp.interfaces

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.StateFlow

enum class LifecycleState {
    UNINITIALIZED, START, RESUME, PAUSE, FINISH, CLOSE
}

interface LifecycleManager {
    val lifecycleState: StateFlow<LifecycleState>

    val isFinishing: Boolean
    val isClosed: Boolean

    suspend fun doOnStart(block: suspend () -> Unit)
    suspend fun doOnResume(block: suspend () -> Unit)
    suspend fun doOnPause(block: suspend () -> Unit)
    suspend fun doOnFinish(block: suspend () -> Unit)
    suspend fun doOnClose(block: suspend () -> Unit)

    fun start()
    fun resume()
    fun pause()
    fun finish()
    fun close()
}

fun LifecycleManager.manageAppLifecycle() {
    ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onMoveToForeground() {
            resume()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onMoveToBackground() {
            pause()
        }
    })
}
