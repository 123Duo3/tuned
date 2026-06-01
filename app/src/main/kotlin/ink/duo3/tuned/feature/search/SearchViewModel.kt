package ink.duo3.tuned.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ink.duo3.tuned.core.Outcome
import ink.duo3.tuned.domain.repository.PodcastRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the add-by-URL screen. Holds the text field and, on submit, runs the
 * repository's subscribe pipeline (fetch → parse → persist). Success surfaces the new
 * podcast id as a one-shot signal; failure surfaces a typed [ink.duo3.tuned.core.AppError].
 * The screen collects exactly one [uiState].
 */
class SearchViewModel(
    private val repository: PodcastRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query, error = null) }

    fun subscribe() {
        val url = _uiState.value.query.trim()
        if (url.isEmpty() || _uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            _uiState.update {
                when (val outcome = repository.subscribe(url)) {
                    is Outcome.Success ->
                        it.copy(isSubmitting = false, addedPodcastId = outcome.value, query = "")

                    is Outcome.Failure ->
                        it.copy(isSubmitting = false, error = outcome.error)
                }
            }
        }
    }

    fun consumeAdded() = _uiState.update { it.copy(addedPodcastId = null) }

    fun consumeError() = _uiState.update { it.copy(error = null) }
}
