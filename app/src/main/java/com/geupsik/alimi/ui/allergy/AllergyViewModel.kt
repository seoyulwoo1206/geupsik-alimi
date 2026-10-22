package com.geupsik.alimi.ui.allergy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.geupsik.alimi.appContainer
import com.geupsik.alimi.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AllergyUiState(
    val loaded: Boolean = false,
    val selected: Set<Int> = emptySet(),
    val saved: Set<Int> = emptySet(),
    val noticeAccepted: Boolean = false,
    val showNotice: Boolean = false,
    /** 저장이 끝나서 화면을 닫아도 될 때 */
    val done: Boolean = false,
) {
    val changed: Boolean get() = selected != saved
}

class AllergyViewModel(private val prefs: UserPrefs) : ViewModel() {

    private val _state = MutableStateFlow(AllergyUiState())
    val state: StateFlow<AllergyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = prefs.allergies.first()
            val accepted = prefs.allergyNoticeAccepted.first()
            _state.update {
                it.copy(loaded = true, selected = saved, saved = saved, noticeAccepted = accepted)
            }
        }
    }

    fun toggle(number: Int) {
        _state.update {
            val next = if (number in it.selected) it.selected - number else it.selected + number
            it.copy(selected = next)
        }
    }

    /** 처음 알레르기를 등록할 때는 안내를 먼저 보여주고, "확인했어요"를 눌러야 저장한다. */
    fun onSaveClick() {
        val s = _state.value
        if (s.selected.isNotEmpty() && !s.noticeAccepted) {
            _state.update { it.copy(showNotice = true) }
        } else {
            save(noticeAccepted = false)
        }
    }

    fun onNoticeConfirm() {
        _state.update { it.copy(showNotice = false, noticeAccepted = true) }
        save(noticeAccepted = true)
    }

    fun onNoticeDismiss() {
        _state.update { it.copy(showNotice = false) }
    }

    private fun save(noticeAccepted: Boolean) {
        val selected = _state.value.selected
        viewModelScope.launch {
            prefs.setAllergies(selected, noticeAccepted)
            _state.update { it.copy(saved = selected, done = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AllergyViewModel(appContainer.userPrefs) }
        }
    }
}
