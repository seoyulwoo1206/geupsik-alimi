package com.geupsik.alimi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.geupsik.alimi.appContainer
import com.geupsik.alimi.data.prefs.UserPrefs
import com.geupsik.alimi.data.repository.SchoolRepository
import com.geupsik.alimi.notification.MealAlarmScheduler
import com.geupsik.alimi.update.UpdateChecker
import com.geupsik.alimi.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StartState { Loading, NeedsSchool, Ready }

/** 앱을 켰을 때 할 일: 첫 화면 정하기, 알람 다시 걸기, 새 버전 확인 */
class MainViewModel(
    schoolRepository: SchoolRepository,
    userPrefs: UserPrefs,
    alarmScheduler: MealAlarmScheduler,
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    val startState: StateFlow<StartState> = flow {
        val school = schoolRepository.selectedSchool.first()
        emit(if (school == null) StartState.NeedsSchool else StartState.Ready)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartState.Loading)

    private val _update = MutableStateFlow<UpdateInfo?>(null)
    /** 새 버전이 있으면 안내 팝업을 띄운다 */
    val update: StateFlow<UpdateInfo?> = _update.asStateFlow()

    init {
        // 앱을 강제 종료하면 걸어 둔 알람이 지워지므로, 앱을 열 때마다 다시 건다
        viewModelScope.launch { alarmScheduler.apply(userPrefs.notification.first()) }
        // 업데이트 확인은 부가 기능이라 어떤 문제가 생겨도 앱 사용을 막지 않는다
        viewModelScope.launch { _update.value = runCatching { updateChecker.checkOnLaunch() }.getOrNull() }
    }

    /** "나중에" */
    fun dismissUpdate() {
        val info = _update.value ?: return
        _update.value = null
        viewModelScope.launch { updateChecker.dismiss(info) }
    }

    /** "다운로드"를 눌러 브라우저로 넘어갔을 때. 이번 실행에서는 다시 띄우지 않는다. */
    fun onUpdateOpened() {
        _update.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel(
                    appContainer.schoolRepository,
                    appContainer.userPrefs,
                    appContainer.mealAlarmScheduler,
                    appContainer.updateChecker,
                )
            }
        }
    }
}
