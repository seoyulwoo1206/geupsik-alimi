package com.geupsik.alimi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.geupsik.alimi.appContainer
import com.geupsik.alimi.data.model.School
import com.geupsik.alimi.data.prefs.NotificationSettings
import com.geupsik.alimi.data.prefs.UserPrefs
import com.geupsik.alimi.data.repository.SchoolRepository
import com.geupsik.alimi.notification.DailyMealNotification
import com.geupsik.alimi.notification.MealAlarmScheduler
import com.geupsik.alimi.notification.MealNotifier
import com.geupsik.alimi.update.ManualCheckResult
import com.geupsik.alimi.update.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    schoolRepository: SchoolRepository,
    private val userPrefs: UserPrefs,
    private val scheduler: MealAlarmScheduler,
    private val notifier: MealNotifier,
    private val dailyMealNotification: DailyMealNotification,
    private val updateChecker: UpdateChecker,
) : ViewModel() {

    /** github 빌드이고 저장소가 정해져 있을 때만 "업데이트 확인"을 보여준다 */
    val updateCheckAvailable: Boolean = updateChecker.isAvailable

    private val _checkingUpdate = MutableStateFlow(false)
    val checkingUpdate: StateFlow<Boolean> = _checkingUpdate.asStateFlow()

    private val _updateResult = MutableStateFlow<ManualCheckResult?>(null)
    val updateResult: StateFlow<ManualCheckResult?> = _updateResult.asStateFlow()

    fun checkUpdate() {
        if (_checkingUpdate.value) return
        _checkingUpdate.value = true
        viewModelScope.launch {
            _updateResult.value = updateChecker.checkNow()
            _checkingUpdate.value = false
        }
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }

    val school: StateFlow<School?> = schoolRepository.selectedSchool
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allergies: StateFlow<Set<Int>> = userPrefs.allergies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val notification: StateFlow<NotificationSettings?> = userPrefs.notification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 시스템 설정에서 앱 알림이 꺼져 있지 않은지 (13 이상은 권한 포함) */
    fun canNotify(): Boolean = notifier.canNotify()

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setNotificationEnabled(enabled)
            scheduler.apply(userPrefs.notification.first())
        }
    }

    fun setTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            userPrefs.setNotificationTime(hour * 60 + minute)
            scheduler.apply(userPrefs.notification.first())
        }
    }

    /** 개발용: 알람을 기다리지 않고 지금 오늘 급식 알림을 보내 본다 */
    fun sendTestNotification() {
        viewModelScope.launch { dailyMealNotification.send() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    appContainer.schoolRepository,
                    appContainer.userPrefs,
                    appContainer.mealAlarmScheduler,
                    appContainer.mealNotifier,
                    appContainer.dailyMealNotification,
                    appContainer.updateChecker,
                )
            }
        }
    }
}
