package com.m3sv.plainupnp.logging

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultLogger @Inject constructor() : Logger {

    override fun e(e: Throwable, message: String?, remote: Boolean) {
        Timber.e(e, message)
    }

    override fun e(text: String, remote: Boolean) {
        Timber.e(text)
    }

    override fun d(text: String) {
        Timber.d(text)
    }
}
