package com.m3sv.plainupnp.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private fun Throwable.recordException() {
    try {
        FirebaseCrashlytics.getInstance().recordException(this)
    } catch (e: NullPointerException) {
        Timber.e(e, "Failed to initialize Crashlytics component!")
    }
}

@Singleton
class DefaultLogger @Inject constructor() : Logger {
    override fun e(e: Throwable, message: String?, remote: Boolean) {
        if (message == null) {
            Timber.e(e)
            if (remote) {
                e.recordException()
            }
        } else {
            val throwable = IllegalStateException(message, e)
            Timber.e(throwable)
            throwable.recordException()
        }
    }

    override fun e(text: String, remote: Boolean) {
        Timber.e(text)
        if (remote) {
            IllegalStateException(text).recordException()
        }
    }

    override fun d(text: String) {
        Timber.d(text)
    }
}
