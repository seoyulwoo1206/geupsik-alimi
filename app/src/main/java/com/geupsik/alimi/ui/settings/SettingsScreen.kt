package com.geupsik.alimi.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.geupsik.alimi.BuildConfig
import com.geupsik.alimi.data.prefs.NotificationSettings
import com.geupsik.alimi.domain.Allergy
import com.geupsik.alimi.ui.components.UpdateDialog
import com.geupsik.alimi.update.ManualCheckResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangeSchool: () -> Unit,
    onOpenAllergy: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val context = LocalContext.current
    val school by vm.school.collectAsStateWithLifecycle()
    val allergies by vm.allergies.collectAsStateWithLifecycle()
    val notification by vm.notification.collectAsStateWithLifecycle()
    val checkingUpdate by vm.checkingUpdate.collectAsStateWithLifecycle()
    val updateResult by vm.updateResult.collectAsStateWithLifecycle()

    (updateResult as? ManualCheckResult.UpdateAvailable)?.let { result ->
        UpdateDialog(
            info = result.info,
            onDownload = vm::clearUpdateResult,
            onLater = vm::clearUpdateResult,
        )
    }

    // 시스템 설정에서 알림을 껐다가 돌아올 수 있으니 화면에 돌아올 때마다 다시 확인한다
    var canNotify by rememberSaveable { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        canNotify = vm.canNotify()
        onPauseOrDispose { }
    }

    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        canNotify = vm.canNotify()
        if (granted) vm.setEnabled(true) else showPermissionDialog = true
    }

    fun turnOn() {
        when {
            vm.canNotify() -> vm.setEnabled(true)
            // 13 이상: 앱이 직접 권한을 물어볼 수 있다 (두 번 거절하면 시스템이 더는 안 띄움 → 콜백에서 안내)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // 12 이하: 사용자가 시스템 설정에서 알림을 끈 경우
            else -> showPermissionDialog = true
        }
    }

    fun openNotificationSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("알림이 꺼져 있어요") },
            text = { Text("급식 알림을 받으려면 휴대폰 설정에서 이 앱의 알림을 허용해 주세요.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    openNotificationSettings()
                }) { Text("설정 열기") }
            },
            dismissButton = { TextButton(onClick = { showPermissionDialog = false }) { Text("닫기") } },
        )
    }

    val current = notification
    if (showTimePicker && current != null) {
        TimePickerDialog(
            initial = current,
            onConfirm = { h, m ->
                showTimePicker = false
                vm.setTime(h, m)
            },
            onDismiss = { showTimePicker = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("학교")
            ListItem(
                modifier = Modifier.clickable(onClick = onChangeSchool),
                leadingContent = { Icon(Icons.Outlined.School, contentDescription = null) },
                headlineContent = { Text(school?.name ?: "등록된 학교 없음") },
                supportingContent = { school?.address?.let { Text(it) } },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "학교 변경")
                },
            )
            HorizontalDivider()

            SectionTitle("알레르기")
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenAllergy),
                leadingContent = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
                headlineContent = { Text("내 알레르기") },
                supportingContent = {
                    Text(if (allergies.isEmpty()) "등록 안 함" else Allergy.names(allergies))
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "알레르기 변경")
                },
            )
            HorizontalDivider()

            SectionTitle("알림")
            if (current != null) {
                val enabled = current.enabled
                ListItem(
                    modifier = Modifier.clickable { if (enabled) vm.setEnabled(false) else turnOn() },
                    leadingContent = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
                    headlineContent = { Text("매일 급식 알림") },
                    supportingContent = { Text("정한 시간에 오늘 메뉴를 알려 줘요. 몇 분 늦게 올 수 있어요.") },
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { on -> if (on) turnOn() else vm.setEnabled(false) },
                        )
                    },
                )
                ListItem(
                    modifier = Modifier.clickable(enabled = enabled) { showTimePicker = true },
                    leadingContent = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                    headlineContent = { Text("알림 시간") },
                    supportingContent = { Text(formatTime(current)) },
                    colors = if (enabled) ListItemDefaults.colors() else disabledColors(),
                )
                if (enabled && !canNotify) {
                    ListItem(
                        modifier = Modifier.clickable { turnOn() },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        headlineContent = {
                            Text("휴대폰 설정에서 알림이 꺼져 있어요", color = MaterialTheme.colorScheme.error)
                        },
                        supportingContent = { Text("눌러서 알림을 허용해 주세요.") },
                    )
                }
                if (BuildConfig.DEBUG) {
                    ListItem(
                        modifier = Modifier.clickable { vm.sendTestNotification() },
                        leadingContent = { Icon(Icons.Outlined.Science, contentDescription = null) },
                        headlineContent = { Text("테스트 알림 보내기") },
                        supportingContent = { Text("개발용: 지금 바로 오늘 급식 알림을 보내요.") },
                    )
                }
            }
            HorizontalDivider()

            SectionTitle("앱 정보")
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                headlineContent = { Text("버전") },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            )
            if (vm.updateCheckAvailable) {
                ListItem(
                    modifier = Modifier.clickable(enabled = !checkingUpdate) { vm.checkUpdate() },
                    leadingContent = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                    headlineContent = { Text("업데이트 확인") },
                    supportingContent = { Text(updateStatusText(checkingUpdate, updateResult)) },
                    trailingContent = {
                        if (checkingUpdate) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: NotificationSettings,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("알림 시간") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("확인") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun disabledColors() = ListItemDefaults.colors(
    headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    supportingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    leadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
)

private fun updateStatusText(checking: Boolean, result: ManualCheckResult?): String = when {
    checking -> "확인하는 중…"
    result is ManualCheckResult.UpToDate -> "최신 버전이에요."
    result is ManualCheckResult.UpdateAvailable -> "새 버전 ${result.info.tag}이 있어요."
    result is ManualCheckResult.NoRelease -> "아직 올라온 릴리스가 없어요."
    result is ManualCheckResult.Failed -> "확인하지 못했어요. 인터넷 연결을 확인해 주세요."
    else -> "눌러서 새 버전이 있는지 확인해요."
}

private fun formatTime(settings: NotificationSettings): String {
    val ampm = if (settings.hour < 12) "오전" else "오후"
    val hour12 = (settings.hour % 12).let { if (it == 0) 12 else it }
    return "%s %d:%02d".format(ampm, hour12, settings.minute)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
