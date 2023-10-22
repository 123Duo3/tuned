package ink.duo3.tuned.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ink.duo3.tuned.data.dao.RecentlyUpdatedDao
import ink.duo3.tuned.data.dao.SubscriptionDao
import ink.duo3.tuned.ui.state.HomeUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val recentlyUpdatedDao: RecentlyUpdatedDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUIState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    subscribed = subscriptionDao.getSubscriptions(),
                    recentlyUpdated = recentlyUpdatedDao.getRecentlyUpdatedEpisodes()
                )
            }
        }
    }
}