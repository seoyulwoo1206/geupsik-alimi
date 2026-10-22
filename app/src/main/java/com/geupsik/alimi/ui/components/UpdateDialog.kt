package com.geupsik.alimi.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.geupsik.alimi.update.UpdateInfo

/** 새 버전 안내. "다운로드"를 누르면 브라우저에서 APK(또는 릴리스 페이지)를 연다. */
@Composable
fun UpdateDialog(info: UpdateInfo, onDownload: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("새 버전이 있어요") },
        text = {
            Column {
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
                    "다운로드한 APK를 열어 설치하면 지금 앱 위에 업데이트돼요. 설정과 알레르기 정보는 그대로 남아요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                openInBrowser(context, info.downloadUrl)
                onDownload()
            }) { Text("다운로드") }
        },
        dismissButton = { TextButton(onClick = onLater) { Text("나중에") } },
    )
}

fun openInBrowser(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "브라우저를 열 수 없어요.", Toast.LENGTH_SHORT).show()
    }
}
