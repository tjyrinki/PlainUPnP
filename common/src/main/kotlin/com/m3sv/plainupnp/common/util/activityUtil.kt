package com.m3sv.plainupnp.common.util

import android.app.Activity
import kotlin.system.exitProcess

fun Activity.finishApp() {
    finishAffinity()
    exitProcess(0)
}
