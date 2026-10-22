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
import com.geupsik.alimi.data.toAppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SearchResultState {
    data object Idle : SearchResultState
    data object Loading : SearchResultState
    data class Success(val schools: List<School>, val totalCount: Int) : SearchResultState
    data object Empty : SearchResultState
    data class Error(val error: AppError) : SearchResultState
}

data class SchoolSearchUiState(
    val query: String = "",
    val result: SearchResultState = SearchResultState.Idle,
    val queryTooShort: Boolean = false,
    val isSampleMode: Boolean = false,
    val saving: Boolean = false,
)

class SchoolSearchViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _state = MutableStateFlow(SchoolSearchUiState(isSampleMode = repository.isSampleMode))
    val state: StateFlow<SchoolSearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, queryTooShort = false) }
    }

    /** 나이스 호출 한도를 아끼려고 글자를 칠 때마다가 아니라 검색 버튼을 누를 때만 조회한다. */
    fun search() {
        val query = _state.value.query.trim()
        if (query.length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(queryTooShort = true) }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(result = SearchResultState.Loading) }
            val result = try {
                val found = repository.search(query)
                if (found.schools.isEmpty()) SearchResultState.Empty
                else SearchResultState.Success(found.schools, found.totalCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SearchResultState.Error(e.toAppError())
            }
            _state.update { it.copy(result = result) }
        }
    }

    suspend fun select(school: School) {
        _state.update { it.copy(saving = true) }
        repository.select(school)
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { SchoolSearchViewModel(appContainer.schoolRepository) }
        }
    }
}
