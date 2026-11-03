package com.geupsik.alimi.ui.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geupsik.alimi.data.model.Meal
import com.geupsik.alimi.data.model.School
import com.geupsik.alimi.domain.Allergy
import com.geupsik.alimi.domain.Dish
import com.geupsik.alimi.domain.MealDates
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val MONTH_DAY = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

/** 한 주(월~일)의 급식을 한 화면에 모아 보여준다. 주말은 급식이 있을 때만 나온다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekView(
    school: School,
    weekStart: LocalDate,
    today: LocalDate,
    load: WeekLoad?,
    allergies: Set<Int>?,
    onWeekChange: (LocalDate) -> Unit,
    onDishClick: (Dish) -> Unit,
    vm: MealViewModel,
) {
    val thisWeek = MealDates.weekStart(today)
    WeekBar(
        weekStart = weekStart,
        isThisWeek = weekStart == thisWeek,
        onPrevious = { onWeekChange(weekStart.minusWeeks(1)) },
        onNext = { onWeekChange(weekStart.plusWeeks(1)) },
        onThisWeek = { onWeekChange(thisWeek) },
    )

    val failed = (load as? WeekLoad.Failed)?.error
    PullToRefreshBox(
        isRefreshing = load is WeekLoad.Loading && load.userInitiated,
        onRefresh = { vm.refreshNow(weekStart) },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (failed != null) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CacheNotice(failed)
                        TextButton(onClick = { vm.refreshNow(weekStart) }) { Text("다시 시도") }
                    }
                }
            }
            items((0L..6L).map { weekStart.plusDays(it) }, key = { it.toEpochDay() }) { date ->
                DayBlock(
                    school = school,
                    date = date,
                    isToday = date == today,
                    loadFailed = failed != null,
                    allergies = allergies.orEmpty(),
                    onDishClick = onDishClick,
                    vm = vm,
                )
            }
        }
    }
}

@Composable
private fun WeekBar(
    weekStart: LocalDate,
    isThisWeek: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onThisWeek: () -> Unit,
) {
    val weekEnd = weekStart.plusDays(6)
    val endText = if (weekEnd.month == weekStart.month) "${weekEnd.dayOfMonth}일" else weekEnd.format(MONTH_DAY)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "지난주")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${weekStart.format(MONTH_DAY)} ~ $endText",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Box(Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                if (isThisWeek) {
                    Text("이번 주", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = onThisWeek, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("이번 주로 돌아가기")
                    }
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 주")
        }
    }
}

@Composable
private fun DayBlock(
    school: School,
    date: LocalDate,
    isToday: Boolean,
    loadFailed: Boolean,
    allergies: Set<Int>,
    onDishClick: (Dish) -> Unit,
    vm: MealViewModel,
) {
    val dayFlow = remember(school, date) { vm.observeDay(school, date) }
    val day by dayFlow.collectAsStateWithLifecycle(initialValue = null)
    val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val current = day
    // 주말은 급식이 있을 때만 보여준다
    if (weekend && (current == null || current.meals.isEmpty())) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    date.format(MONTH_DAY),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isToday) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "오늘",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                current == null -> SmallNote("불러오는 중…")
                !current.weekFetched && current.meals.isEmpty() ->
                    SmallNote(if (loadFailed) "불러오지 못했어요" else "불러오는 중…")
                current.meals.isEmpty() -> SmallNote("급식 정보가 없어요")
                else -> current.meals.forEach { CompactMeal(it, allergies, onDishClick) }
            }
        }
    }
}

/** 주간 보기용 끼니 한 줄: 아이콘 + 메뉴들을 가운뎃점으로 이어서 줄바꿈 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactMeal(meal: Meal, allergies: Set<Int>, onDishClick: (Dish) -> Unit) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        MealIcon(meal.code, size = 24.dp)
        Spacer(Modifier.width(10.dp))
        FlowRow(Modifier.weight(1f)) {
            meal.dishes.forEachIndexed { index, dish ->
                // 가운뎃점을 메뉴 뒤에 붙여 둬서 줄이 바뀔 때 점이 줄 맨 앞에 오지 않게 한다
                Row {
                    val warn = Allergy.matched(dish, allergies).isNotEmpty()
                    Text(
                        dish.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (warn) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .clickable { onDishClick(dish) }
                            .padding(vertical = 2.dp),
                    )
                    if (index < meal.dishes.lastIndex) {
                        Text(
                            " · ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
