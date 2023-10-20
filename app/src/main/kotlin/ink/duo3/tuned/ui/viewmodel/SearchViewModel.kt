package ink.duo3.tuned.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.rssparser.RssParser
import ink.duo3.tuned.ui.state.SearchUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUIState())
    val uiState = _uiState.asStateFlow()

    fun onSearchFieldValueChanged(value: String) {
        _uiState.update {
            it.copy(
                searchFieldValue = value
            )
        }
    }

    fun onSearch() {
//        if (Patterns.WEB_URL.matcher(_uiState.value.searchFieldValue).matches()) {
//            searchWithRSSURL(_uiState.value.searchFieldValue)
//        } else {
//            searchWithText(_uiState.value.searchFieldValue)
//        }
        searchWithRSSURL(_uiState.value.searchFieldValue)
    }

    fun searchWithText(
        query: String
    ) {
        TODO()
    }

    private fun searchWithRSSURL(
        url: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { state ->
                state.copy(
                    isLoading = true
                )
            }

            val podcast = runCatching {
                RssParser().getRssChannel(url)
            }

            podcast.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        rssResult = it,
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        rssNotFound = true
                    )
                }
            }
        }
    }
}