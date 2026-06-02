package ink.duo3.tuned.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunedPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(pullProgress: Float) -> Unit,
) {
    val state = rememberPullToRefreshState()
    var suppressPullProgress by remember { mutableStateOf(false) }

    LaunchedEffect(isRefreshing, suppressPullProgress) {
        if (suppressPullProgress && !isRefreshing) {
            snapshotFlow { state.distanceFraction }
                .first { it == 0f }
            suppressPullProgress = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            suppressPullProgress = true
            onRefresh()
        },
        modifier = modifier,
        state = state,
        indicator = {},
    ) {
        val pullProgress =
            if (isRefreshing || suppressPullProgress || state.isAnimating) {
                0f
            } else {
                state.distanceFraction
            }
        content(pullProgress)
    }
}
