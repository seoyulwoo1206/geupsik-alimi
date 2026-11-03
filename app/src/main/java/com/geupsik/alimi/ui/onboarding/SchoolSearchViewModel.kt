package com.geupsik.alimi.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.geupsik.alimi.appContainer
import com.geupsik.alimi.data.AppError
import com.geupsik.alimi.data.model.School
import com.geupsik.alimi.data.repository.SchoolRepository
import com.geupsik.alimi.data.repository.SchoolSearchResult
import com.geupsik.alimi.data.toAppError
import com.geupsik.alimi.domain.SchoolQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchResultState {
    data object Idle : SearchResultState
    data class Success(val schools: List<School>, val totalCount: Int) : SearchResultState
    data object Empty : SearchResultState
    data class Error(val error: AppError) : SearchResultState
}

data class SchoolSearchUiState(
    val query: String = "",
    val result: SearchResultState = SearchResultState.Idle,
    /** 찾는 중. 이전 결과는 그대로 두고 위에 얇은 진행 막대만 보여준다. */
    val loading: Boolean = false,
    val queryTooShort: Boolean = false,
    val isSampleMode: Boolean = false,
    val saving: Boolean = false,
)

class SchoolSearchViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _state = MutableStateFlow(SchoolSearchUiState(isSampleMode = repository.isSampleMode))
    val state: StateFlow<SchoolSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    /** 같은 검색어를 다시 칠 때 나이스를 또 부르지 않도록 최근 결과를 기억해 둔다. */
    private val cache = object : LinkedHashMap<String, SchoolSearchResult>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SchoolSearchResult>?) =
            size > CACHE_SIZE
    }

    /**
     * 글자를 칠 때마다 잠깐 기다렸다가 추천 목록을 찾아 준다.
     * 빠르게 이어 치는 동안에는 부르지 않아서 나이스 호출 한도를 아낀다.
     */
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, queryTooShort = false) }
        val normalized = SchoolQuery.normalize(query)
        if (normalized.length < MIN_QUERY_LENGTH) {
            searchJob?.cancel()
            _state.update { it.copy(result = SearchResultState.Idle, loading = false) }
            return
        }
        launchSearch(normalized, delayMillis = SUGGEST_DELAY_MILLIS)
    }

    /** 검색 버튼이나 키보드의 검색 키 */
    fun search() {
        val normalized = SchoolQuery.normalize(_state.value.query)
        if (normalized.length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(queryTooShort = true) }
            return
        }
        launchSearch(normalized, delayMillis = 0)
    }

    private fun launchSearch(query: String, delayMillis: Long) {
        searchJob?.cancel()
        cache[query]?.let { found ->
            _state.update { it.copy(result = found.toState(), loading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(delayMillis)
            _state.update { it.copy(loading = true) }
            val result = try {
                repository.search(query).also { cache[query] = it }.toState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SearchResultState.Error(e.toAppError())
            }
            _state.update { it.copy(result = result, loading = false) }
        }
    }

    private fun SchoolSearchResult.toState(): SearchResultState =
        if (schools.isEmpty()) SearchResultState.Empty else SearchResultState.Success(schools, totalCount)

    suspend fun select(school: School) {
        _state.update { it.copy(saving = true) }
        repository.select(school)
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        private const val SUGGEST_DELAY_MILLIS = 500L
        private const val CACHE_SIZE = 30

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SchoolSearchViewModel(appContainer.schoolRepository) }
        }
    }
}
