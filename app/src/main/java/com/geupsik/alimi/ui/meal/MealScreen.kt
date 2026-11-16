package com.geupsik.alimi.ui.meal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.geupsik.alimi.R
import com.geupsik.alimi.domain.DishEmoji
import java.time.LocalTime
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.geupsik.alimi.domain.MealDates
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
    val weeklyView by vm.weeklyView.collectAsStateWithLifecycle()
    val pager = rememberPagerState(initialPage = DAYS_EACH_SIDE) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    var sheetDish by remember { mutableStateOf<Dish?>(null) }

    fun dateOf(page: Int): LocalDate = vm.baseDate.plusDays((page - DAYS_EACH_SIDE).toLong())

    val today = LocalDate.now()
    val currentDate = dateOf(pager.currentPage)
    var weekStart by rememberSaveable { mutableStateOf(MealDates.weekStart(today)) }
    val weekly = weeklyView == true

    LaunchedEffect(pager, weekly) {
        if (!weekly) snapshotFlow { pager.settledPage }.collect { vm.onDateSelected(dateOf(it)) }
    }
    LaunchedEffect(weekly, weekStart) {
        if (weekly) vm.onDateSelected(weekStart)
    }
    LifecycleResumeEffect(Unit) {
        vm.onResume()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { TopTitle(school?.name.orEmpty()) },
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
            ViewModeSwitch(
                weekly = weekly,
                onChange = { toWeekly ->
                    // 보고 있던 날짜를 이어서 보여준다
                    if (toWeekly) weekStart = MealDates.weekStart(currentDate)
                    vm.setWeeklyView(toWeekly)
                },
            )

            val currentSchool = school
            when {
                currentSchool == null || weeklyView == null -> Box(Modifier.fillMaxSize())

                weekly -> WeekView(
                    school = currentSchool,
                    weekStart = weekStart,
                    today = today,
                    load = weekLoads[MealViewModel.weekKey(currentSchool, weekStart)],
                    allergies = allergies,
                    onWeekChange = { weekStart = it },
                    onDishClick = { sheetDish = it },
                    vm = vm,
                )

                else -> {
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
                            onDishClick = { sheetDish = it },
                            vm = vm,
                        )
                    }
                }
            }
        }
    }

    sheetDish?.let { DishSheet(it, allergies.orEmpty(), onDismiss = { sheetDish = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeSwitch(weekly: Boolean, onChange: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        SegmentedButton(
            selected = !weekly,
            onClick = { onChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("하루") }
        SegmentedButton(
            selected = weekly,
            onClick = { onChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("일주일") }
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
                    TodayBadge()
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
    onDishClick: (Dish) -> Unit,
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
            onDishClick = onDishClick,
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
    onDishClick: (Dish) -> Unit,
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
                items(day.meals, key = { it.code }) { MealCard(it, allergies.orEmpty(), onDishClick) }
                if (allergies != null && allergies.isEmpty()) {
                    item { AllergyHint(onOpenAllergy) }
                }
                item { TapHint() }
            }

            day.weekFetched -> {
                if (failed != null) item { CacheNotice(failed) }
                item {
                    CenterMessage(
                        icon = { Mascot(Modifier.size(120.dp)) },
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
private fun MealCard(meal: Meal, allergies: Set<Int>, onDishClick: (Dish) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MealIcon(meal.code)
                Spacer(Modifier.width(12.dp))
                Text(
                    meal.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                meal.calories?.let {
                    Surface(shape = CircleShape, color = MealStyle.tint(meal.code)) {
                        Text(
                            it.replace(" Kcal", " kcal"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            meal.dishes.forEachIndexed { index, dish ->
                if (index > 0) {
                    // 이모지 칸 다음부터 그어서 메뉴 이름 줄이 반듯하게 보이게 한다
                    HorizontalDivider(
                        modifier = Modifier.padding(start = DISH_TEXT_START),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
                DishRow(dish, Allergy.matched(dish, allergies), onClick = { onDishClick(dish) })
            }
        }
    }
}

/** 이모지 칸 너비 + 간격. 모든 메뉴 이름이 이 위치에서 시작한다. */
private val DISH_EMOJI_WIDTH = 32.dp
private val DISH_TEXT_START = DISH_EMOJI_WIDTH + 10.dp

/**
 * 메뉴 한 줄: [음식 이모지] [메뉴 이름] [내 알레르기 배지].
 * 이름이 길면 이름 칸 안에서 줄을 바꿔서 왼쪽 줄이 흐트러지지 않는다.
 * 누르면 사진·알레르기 정보 창을 연다.
 */
@Composable
private fun DishRow(dish: Dish, matched: Set<Int>, onClick: () -> Unit) {
    val warn = matched.isNotEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = "사진과 알레르기 정보 보기", onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (warn) contentDescription = "${dish.name}, ${Allergy.names(matched)} 포함"
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(DISH_EMOJI_WIDTH), contentAlignment = Alignment.Center) {
            Text(DishEmoji.of(dish.name), fontSize = 20.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            dish.name,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = if (warn) FontWeight.Bold else FontWeight.Medium,
            color = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (warn) {
            Spacer(Modifier.width(8.dp))
            AllergyBadge(Allergy.names(matched))
        }
    }
}

/** 메뉴 오른쪽 끝에 붙는 내 알레르기 표시 */
@Composable
private fun AllergyBadge(names: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            Modifier.padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                names,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 1,
            )
        }
    }
}

/** 상단 바 제목: 마스코트 + 학교 이름 + 시간대에 맞춘 한마디 */
@Composable
private fun TopTitle(schoolName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Mascot(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                schoolName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Text(
                greeting(LocalTime.now()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun greeting(now: LocalTime): String = when (now.hour) {
    in 5..9 -> "좋은 아침! 아침밥 챙겨 먹어요"
    in 10..13 -> "점심 맛있게 먹어요"
    in 14..19 -> "저녁도 든든하게 먹어요"
    else -> "오늘 하루도 수고했어요"
}

@Composable
fun Mascot(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.mascot), contentDescription = null, modifier = modifier)
}

@Composable
fun TodayBadge() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            "오늘",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun TapHint() {
    Text(
        "메뉴를 누르면 사진과 알레르기 정보를 볼 수 있어요.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
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
fun CacheNotice(error: AppError) {
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
fun CenterMessage(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 72.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), contentAlignment = Alignment.Center) {
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
