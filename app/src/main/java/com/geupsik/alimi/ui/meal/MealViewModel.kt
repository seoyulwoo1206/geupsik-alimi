package com.geupsik.alimi.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.geupsik.alimi.appContainer
import com.geupsik.alimi.data.AppError
import com.geupsik.alimi.data.model.DayMeals
import com.geupsik.alimi.data.model.School
import com.geupsik.alimi.data.prefs.UserPrefs
import com.geupsik.alimi.data.repository.MealRepository
import com.geupsik.alimi.data.repository.SchoolRepository
import com.geupsik.alimi.data.repository.cacheKey
import com.geupsik.alimi.data.toAppError
import com.geupsik.alimi.domain.MealDates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 한 주치를 받아 오는 상태 */
sealed interface WeekLoad {
    data class Loading(val userInitiated: Boolean) : WeekLoad
    data object Done : WeekLoad
    data class Failed(val error: AppError) : WeekLoad
}

class MealViewModel(
    private val schoolRepository: SchoolRepository,
    private val mealRepository: MealRepository,
    userPrefs: UserPrefs,
) : ViewModel() {

    /** 내가 등록한 알레르기 번호. null이면 아직 읽는 중 */
    val allergies: StateFlow<Set<Int>?> = userPrefs.allergies
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** 화면을 처음 연 날. 페이저의 가운데 페이지가 이 날짜다. */
    val baseDate: LocalDate = LocalDate.now()

    val school: StateFlow<School?> = schoolRepository.selectedSchool
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val selectedDate = MutableStateFlow(baseDate)

    /** 키: "학교-주시작일" */
    private val _weekLoads = MutableStateFlow<Map<String, WeekLoad>>(emptyMap())
    val weekLoads: StateFlow<Map<String, WeekLoad>> = _weekLoads.asStateFlow()

    init {
        // 날짜를 넘겨 다른 주로 가거나 학교를 바꾸면 그 주를 받아 온다
        viewModelScope.launch {
            combine(school.filterNotNull(), selectedDate) { s, d -> s to MealDates.weekStart(d) }
                .distinctUntilChanged()
                .collect { (s, weekStart) -> refresh(s, weekStart, force = false) }
        }
    }

    fun observeDay(school: School, date: LocalDate): Flow<DayMeals> =
        mealRepository.observeDay(school, date)

    fun onDateSelected(date: LocalDate) {
        selectedDate.value = date
    }

    /** 앱으로 돌아왔을 때. 받아 둔 지 오래됐으면 다시 받는다. */
    fun onResume() {
        val s = school.value ?: return
        refresh(s, MealDates.weekStart(selectedDate.value), force = false)
    }

    /** 당겨서 새로고침, 다시 시도 버튼 */
    fun refreshNow(date: LocalDate) {
        val s = school.value ?: return
        refresh(s, MealDates.weekStart(date), force = true)
    }

    private fun refresh(school: School, weekStart: LocalDate, force: Boolean) {
        val key = weekKey(school, weekStart)
        if (_weekLoads.value[key] is WeekLoad.Loading) return
        _weekLoads.update { it + (key to WeekLoad.Loading(userInitiated = force)) }
        viewModelScope.launch {
            val result = try {
                mealRepository.refreshWeek(school, weekStart, force)
                WeekLoad.Done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                WeekLoad.Failed(e.toAppError())
            }
            _weekLoads.update { it + (key to result) }
        }
    }

    companion object {
        fun weekKey(school: School, date: LocalDate) =
            "${school.cacheKey}-${MealDates.toYmd(MealDates.weekStart(date))}"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MealViewModel(
                    appContainer.schoolRepository,
                    appContainer.mealRepository,
                    appContainer.userPrefs,
                )
            }
        }
    }
}
