package com.geupsik.alimi.ui.meal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.NoMeals
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geupsik.alimi.data.AppError
import com.geupsik.alimi.data.model.DayMeals
import com.geupsik.alimi.data.model.Meal
import com.geupsik.alimi.data.model.School
import com.geupsik.alimi.domain.Allergy
import com.geupsik.alimi.domain.Dish
import com.geupsik.alimi.ui.components.AllergyDisclaimer
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 오늘을 가운데에 두고 앞뒤로 1년씩 넘겨 볼 수 있다. */
private const val DAYS_EACH_SIDE = 365
private const val PAGE_COUNT = DAYS_EACH_SIDE * 2 + 1

private val DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(
    onOpenSettings: () -> Unit,
    onOpenAllergy: () -> Unit,
    vm: MealViewModel = viewModel(factory = MealViewModel.Factory),
) {
    val school by vm.school.collectAsStateWithLifecycle()
    val weekLoads by vm.weekLoads.collectAsStateWithLifecycle()
    val allergies by vm.allergies.collectAsStateWithLifecycle()
    val pager = rememberPagerState(initialPage = DAYS_EACH_SIDE) { PAGE_COUNT }
    val scope = rememberCoroutineScope()

    fun dateOf(page: Int): LocalDate = vm.baseDate.plusDays((page - DAYS_EACH_SIDE).toLong())

    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { vm.onDateSelected(dateOf(it)) }
    }
    LifecycleResumeEffect(Unit) {
        vm.onResume()
        onPauseOrDispose { }
    }

    val currentDate = dateOf(pager.currentPage)
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(school?.name.orEmpty()) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "설정")
                    }
                },
            )
        },
        bottomBar = { AllergyDisclaimer() },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DateBar(
                date = currentDate,
                isToday = currentDate == today,
                onPrevious = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                onNext = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                onToday = {
                    val todayPage = DAYS_EACH_SIDE + (today.toEpochDay() - vm.baseDate.toEpochDay()).toInt()
                    scope.launch { pager.animateScrollToPage(todayPage.coerceIn(0, PAGE_COUNT - 1)) }
                },
            )

            val currentSchool = school
            if (currentSchool == null) {
                Box(Modifier.fillMaxSize())
            } else {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    key = { it },
                ) { page ->
                    val date = dateOf(page)
                    DayPage(
                        school = currentSchool,
                        date = date,
                        load = weekLoads[MealViewModel.weekKey(currentSchool, date)],
                        allergies = allergies,
                        onOpenAllergy = onOpenAllergy,
                        vm = vm,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateBar(
    date: LocalDate,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "전날")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.format(DATE_FORMAT),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            // "오늘" 표시와 버튼 자리를 같은 높이로 맞춰서 날짜를 넘길 때 화면이 들썩이지 않게 한다
            Box(Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                if (isToday) {
                    Text(
                        "오늘",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    TextButton(onClick = onToday, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text("오늘로 돌아가기")
                    }
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음날")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPage(
    school: School,
    date: LocalDate,
    load: WeekLoad?,
    allergies: Set<Int>?,
    onOpenAllergy: () -> Unit,
    vm: MealViewModel,
) {
    val dayFlow = remember(school, date) { vm.observeDay(school, date) }
    val day by dayFlow.collectAsStateWithLifecycle(initialValue = null)
    val refreshing = load is WeekLoad.Loading && load.userInitiated

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { vm.refreshNow(date) },
        modifier = Modifier.fillMaxSize(),
    ) {
        DayContent(
            day = day,
            load = load,
            allergies = allergies,
            onRetry = { vm.refreshNow(date) },
            onOpenAllergy = onOpenAllergy,
        )
    }
}

@Composable
private fun DayContent(
    day: DayMeals?,
    load: WeekLoad?,
    allergies: Set<Int>?,
    onRetry: () -> Unit,
    onOpenAllergy: () -> Unit,
) {
    val failed = (load as? WeekLoad.Failed)?.error
    // 당겨서 새로고침이 되려면 내용이 비어 있어도 스크롤 가능한 목록이어야 한다
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            day == null -> Unit

            day.meals.isNotEmpty() -> {
                if (failed != null) item { CacheNotice(failed) }
                items(day.meals, key = { it.code }) { MealCard(it, allergies.orEmpty()) }
                if (allergies != null && allergies.isEmpty()) {
                    item { AllergyHint(onOpenAllergy) }
                }
            }

            day.weekFetched -> {
                if (failed != null) item { CacheNotice(failed) }
                item {
                    CenterMessage(
                        icon = { Icon(Icons.Outlined.NoMeals, contentDescription = null, Modifier.size(48.dp)) },
                        title = "급식 정보가 없어요",
                        body = "주말·방학·공휴일처럼 급식이 없는 날일 수 있어요.",
                    )
                }
            }

            failed != null -> item {
                CenterMessage(
                    icon = { Icon(Icons.Outlined.CloudOff, contentDescription = null, Modifier.size(48.dp)) },
                    title = "급식을 불러오지 못했어요",
                    body = failed.userMessage,
                    action = { OutlinedButton(onClick = onRetry) { Text("다시 시도") } },
                )
            }

            else -> item {
                Box(Modifier.fillMaxWidth().padding(top = 96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: Meal, allergies: Set<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                meal.calories?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                meal.dishes.forEach { DishRow(it, Allergy.matched(it, allergies)) }
            }
        }
    }
}

/** 내 알레르기가 든 메뉴는 빨간색 + 경고 아이콘. 누르면 어떤 알레르기 때문인지 펼쳐 보여준다. */
@Composable
private fun DishRow(dish: Dish, matched: Set<Int>) {
    if (matched.isEmpty()) {
        Text(
            dish.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        return
    }

    var expanded by rememberSaveable(dish.name) { mutableStateOf(false) }
    val red = MaterialTheme.colorScheme.error
    val names = Allergy.names(matched)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = if (expanded) "알레르기 정보 접기" else "알레르기 정보 보기") {
                expanded = !expanded
            }
            .semantics(mergeDescendants = true) { contentDescription = "${dish.name}, $names 포함" }
            .padding(vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dish.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = red,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = red,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    "$names 포함",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** 알레르기를 아직 등록하지 않았을 때 급식 목록 아래에 보여주는 안내 */
@Composable
private fun AllergyHint(onOpenAllergy: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "알레르기를 등록하면 해당 메뉴를 빨간색으로 표시해 줘요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenAllergy) { Text("등록하기") }
        }
    }
}

/** 캐시를 보여주는 중인데 새로 받아 오기는 실패했을 때 위에 띄우는 띠 */
@Composable
private fun CacheNotice(error: AppError) {
    val text = if (error == AppError.Network) {
        "인터넷에 연결되어 있지 않아 저장된 메뉴를 보여주고 있어요."
    } else {
        "최신 메뉴를 받아 오지 못해 저장된 메뉴를 보여주고 있어요. ${error.userMessage}"
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun CenterMessage(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 72.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.outline) { icon() }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
