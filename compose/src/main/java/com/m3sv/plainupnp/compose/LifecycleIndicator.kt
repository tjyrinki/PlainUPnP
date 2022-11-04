package com.m3sv.plainupnp.compose

import androidx.compose.runtime.Composable
import com.m3sv.plainupnp.common.util.pass
import com.m3sv.plainupnp.interfaces.LifecycleState

@Composable
fun LifecycleIndicator(lifecycleState: LifecycleState, onCloseClick: () -> Unit) {
    if (!(lifecycleState == LifecycleState.FINISH || lifecycleState == LifecycleState.CLOSE))
        return

    FadedBackground {
        when (lifecycleState) {
            LifecycleState.FINISH -> FinishIndicator()
            LifecycleState.CLOSE -> CloseIndicator(onCloseClick)
            else -> pass
        }
    }
}
