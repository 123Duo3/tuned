package ink.duo3.tuned.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Drives the card-based home. For now the only section is "Subscribed", so the state
 * is just the repository's subscription flow mapped into [HomeUiState]. As more cards
 * land (recently updated, resume) they fold into this single observed state.
 */
class HomeViewModel(
    repository: PodcastRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        repository
            .observeSubscriptions()
            .map { HomeUiState(isLoading = false, subscriptions = it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
