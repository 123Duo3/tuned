package ink.duo3.tuned.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.exception.HttpException
import com.prof18.rssparser.exception.RssParsingException
import dagger.hilt.android.lifecycle.HiltViewModel
import ink.duo3.tuned.ui.state.SearchUIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import javax.inject.Inject

enum class SearchState {
    IDLE,
    LOADING,
    SEARCH_SUCCEEDED,
    RSS_FETCH_SUCCEEDED,
    SEARCH_NOT_FOUND,
    RSS_NOT_FOUND,
    NETWORK_ERROR,
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val rssParser: RssParser
) : ViewModel() {

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
                    searchState = SearchState.LOADING,
                )
            }

            val podcast = runCatching {
                rssParser.getRssChannel(url)
            }

            podcast.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        searchState = SearchState.RSS_FETCH_SUCCEEDED,
                        rssResult = it,
                    )
                }
            }.onFailure {
                when (it) {
                    is RssParsingException -> {
                        _uiState.update { state ->
                            state.copy(
                                searchState = SearchState.RSS_NOT_FOUND,
                            )
                        }
                    }

                    is HttpException -> {
                        _uiState.update { state ->
                            state.copy(
                                searchState = SearchState.NETWORK_ERROR,
                                networkErrorMessage = "HTTP Error ${it.code}"
                            )
                        }
                    }

                    is UnknownHostException -> {
                        _uiState.update { state ->
                            state.copy(
                                searchState = SearchState.NETWORK_ERROR,
                                networkErrorMessage = it.localizedMessage ?: "Unknown host"
                            )
                        }
                    }

                    else -> {
                        _uiState.update { state ->
                            state.copy(
                                searchState = SearchState.NETWORK_ERROR,
                                networkErrorMessage = it.localizedMessage ?: "Unknown error"
                            )
                        }
                    }
                }
            }
        }
    }
}