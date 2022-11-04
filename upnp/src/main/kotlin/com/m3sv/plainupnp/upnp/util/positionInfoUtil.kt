package com.m3sv.plainupnp.upnp.util

import org.fourthline.cling.support.model.PositionInfo
import kotlin.math.abs
import kotlin.time.Duration

inline val PositionInfo.remainingDuration: String
    get() {
        val duration = Duration.seconds(trackRemainingSeconds)
        val seconds = duration.inWholeSeconds
        val absSeconds = abs(seconds)
        val positive = "%d:%02d:%02d".format(
            absSeconds / 3600,
            (absSeconds % 3600) / 60,
            absSeconds % 60
        )

        val sign = if (seconds < 0) "-" else ""

        return "$sign$positive"
    }

inline val PositionInfo.duration: String
    get() {
        val duration = Duration.seconds(trackDurationSeconds)
        val seconds = duration.inWholeSeconds
        val absSeconds = abs(seconds)
        val positive = "%d:%02d:%02d".format(
            absSeconds / 3600,
            (absSeconds % 3600) / 60,
            absSeconds % 60
        )

        val sign = if (seconds < 0) "-" else ""

        return "$sign$positive"
    }

inline val PositionInfo.position: String
    get() {
        val duration = Duration.seconds(trackElapsedSeconds)
        val seconds = duration.inWholeSeconds
        val absSeconds = abs(seconds)
        val positive = "%d:%02d:%02d".format(
            absSeconds / 3600,
            (absSeconds % 3600) / 60,
            absSeconds % 60
        )

        val sign = if (seconds < 0) "-" else ""

        return "$sign$positive"
    }
