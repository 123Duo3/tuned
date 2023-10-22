package ink.duo3.tuned.ui.state

import ink.duo3.tuned.data.entity.RecUpdPodEntity
import ink.duo3.tuned.data.entity.SubscribedPodEntity

data class HomeUIState(
    val subscribed: List<SubscribedPodEntity> = emptyList(),
    val recentlyUpdated: List<RecUpdPodEntity> = emptyList(),
)
