package com.geupsik.alimi.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.geupsik.alimi.GeupsikApp
import com.geupsik.alimi.update.UpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

/** 업데이트 팝업의 단계 */
private sealed interface Step {
    data object Ask : Step
    data class Downloading(val progress: Float?) : Step
    /** 받기는 끝났는데 "출처를 알 수 없는 앱 설치" 허용이 아직 없음 */
    data class NeedsPermission(val apk: File) : Step
    data object Failed : Step
}

/**
 * 새 버전 안내. "업데이트"를 누르면 앱 안에서 APK를 받아 바로 설치 화면을 띄운다.
 * 릴리스에 APK가 없거나 받기에 실패하면 브라우저로 받게 한다.
 */
@Composable
fun UpdateDialog(info: UpdateInfo, onDownload: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    val installer = remember { (context.applicationContext as GeupsikApp).container.apkInstaller }
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<Step>(Step.Ask) }
    var job by remember { mutableStateOf<Job?>(null) }
    val hasApk = info.downloadUrl.endsWith(".apk", ignoreCase = true)

    fun installOrAsk(apk: File) {
        if (!installer.canInstall()) {
            step = Step.NeedsPermission(apk)
        } else if (installer.install(apk)) {
            onDownload()
        } else {
            step = Step.Failed
        }
    }

    fun startDownload() {
        step = Step.Downloading(null)
        job = scope.launch {
            try {
                val apk = installer.download(info.downloadUrl, info.tag) { progress ->
                    step = Step.Downloading(progress)
                }
                installOrAsk(apk)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                step = Step.Failed
            }
        }
    }

    // 허용 화면에서 켜고 돌아오면 바로 설치 화면을 띄운다
    LifecycleResumeEffect(step) {
        (step as? Step.NeedsPermission)?.let { if (installer.canInstall()) installOrAsk(it.apk) }
        onPauseOrDispose { }
    }

    val current = step
    AlertDialog(
        // 받는 중에는 바깥을 눌러도 닫히지 않게 한다
        onDismissRequest = { if (current !is Step.Downloading) onLater() },
        title = { Text(if (current is Step.Failed) "업데이트를 받지 못했어요" else "새 버전이 있어요") },
        text = {
            Column {
                when (current) {
                    Step.Ask -> AskText(info, hasApk)
                    is Step.Downloading -> {
                        Text("${info.tag} 받는 중…")
                        Spacer(Modifier.size(12.dp))
                        val progress = current.progress
                        if (progress != null) {
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                    is Step.NeedsPermission -> Text(
                        "처음 한 번만 허용이 필요해요.\n\n" +
                            "'허용하러 가기'를 누르고 '이 출처 허용'을 켠 다음 뒤로 돌아오면 설치 화면이 떠요.",
                    )
                    Step.Failed -> Text("인터넷 연결을 확인하고 다시 시도해 주세요. 브라우저로 받아서 설치해도 돼요.")
                }
            }
        },
        confirmButton = {
            when (current) {
                Step.Ask -> TextButton(onClick = {
                    if (hasApk) {
                        startDownload()
                    } else {
                        openInBrowser(context, info.downloadUrl)
                        onDownload()
                    }
                }) { Text(if (hasApk) "업데이트" else "다운로드") }
                is Step.Downloading -> Unit
                is Step.NeedsPermission ->
                    TextButton(onClick = installer::openInstallPermissionSettings) { Text("허용하러 가기") }
                Step.Failed -> TextButton(onClick = { startDownload() }) { Text("다시 시도") }
            }
        },
        dismissButton = {
            when (current) {
                is Step.Downloading -> TextButton(onClick = {
                    job?.cancel()
                    step = Step.Ask
                }) { Text("취소") }
                Step.Failed -> TextButton(onClick = {
                    openInBrowser(context, info.downloadUrl)
                    onDownload()
                }) { Text("브라우저로 받기") }
                else -> TextButton(onClick = onLater) { Text("나중에") }
            }
        },
    )
}

@Composable
private fun AskText(info: UpdateInfo, hasApk: Boolean) {
    Text("${info.tag} 버전이 나왔어요. (지금 버전: ${info.currentVersion})")
    info.notes?.let { notes ->
        Spacer(Modifier.size(12.dp))
        Text(
            notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState()),
        )
    }
    Spacer(Modifier.size(12.dp))
    Text(
        if (hasApk) {
            "'업데이트'를 누르면 받아서 바로 설치 화면을 띄워요. 마지막에 '설치'만 눌러 주세요. 설정과 알레르기 정보는 그대로 남아요."
        } else {
            "다운로드한 APK를 열어 설치하면 지금 앱 위에 업데이트돼요. 설정과 알레르기 정보는 그대로 남아요."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

fun openInBrowser(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "브라우저를 열 수 없어요.", Toast.LENGTH_SHORT).show()
    }
}
