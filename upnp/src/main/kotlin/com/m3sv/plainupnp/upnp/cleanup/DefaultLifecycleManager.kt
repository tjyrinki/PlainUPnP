package com.m3sv.plainupnp.upnp.cleanup

import com.m3sv.plainupnp.interfaces.LifecycleManager
import com.m3sv.plainupnp.interfaces.LifecycleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import javax.inject.Inject

class DefaultLifecycleManager @Inject constructor() : LifecycleManager {
    private val _lifecycleState = MutableStateFlow(LifecycleState.UNINITIALIZED)

    override val lifecycleState: StateFlow<LifecycleState> = _lifecycleState

    override val isFinishing: Boolean
        get() = lifecycleState.value == LifecycleState.FINISH

    override val isClosed: Boolean
        get() = lifecycleState.value == LifecycleState.CLOSE

    override fun start() {
        if (isFinished)
            return

        _lifecycleState.value = LifecycleState.START
    }

    override fun resume() {
        if (isFinished)
            return

        _lifecycleState.value = LifecycleState.RESUME
    }

    override fun pause() {
        if (isFinished)
            return

        _lifecycleState.value = LifecycleState.PAUSE
    }

    override fun finish() {
        if (isFinished)
            return

        _lifecycleState.value = LifecycleState.FINISH
    }

    override fun close() {
        _lifecycleState.value = LifecycleState.CLOSE
    }

    override suspend fun doOnStart(block: suspend () -> Unit) {
        lifecycleState
            .filter { it == LifecycleState.START }
            .collect { block() }
    }

    override suspend fun doOnResume(block: suspend () -> Unit) {
        lifecycleState
            .filter { it == LifecycleState.RESUME }
            .collect { block() }
    }

    override suspend fun doOnPause(block: suspend () -> Unit) {
        lifecycleState
            .filter { it == LifecycleState.PAUSE }
            .collect { block() }
    }

    override suspend fun doOnFinish(block: suspend () -> Unit) {
        lifecycleState
            .filter { it == LifecycleState.FINISH }
            .collect { block() }
    }

    override suspend fun doOnClose(block: suspend () -> Unit) {
        lifecycleState
            .filter { it == LifecycleState.CLOSE }
            .collect { block() }
    }

    private val isFinished: Boolean
        get() = lifecycleState.value == LifecycleState.FINISH || lifecycleState.value == LifecycleState.CLOSE
}
