package com.geupsik.alimi.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * 새 버전 APK를 앱 안에서 받아 안드로이드 설치 화면을 띄운다.
 * 스토어 밖 앱이라 마지막 "설치" 버튼은 사용자가 직접 눌러야 한다.
 */
class ApkInstaller(private val context: Context, okHttp: OkHttpClient) {

    // APK는 수 MB라서 읽기 시간을 넉넉히 둔다
    private val client = okHttp.newBuilder().readTimeout(60, TimeUnit.SECONDS).build()

    private val folder get() = File(context.cacheDir, "updates")

    /**
     * url의 APK를 받아 파일로 저장한다. onProgress에는 0~1 사이 값이 온다(전체 크기를 모르면 null).
     * 실패하면 IOException을 던진다.
     */
    suspend fun download(url: String, tag: String, onProgress: (Float?) -> Unit): File =
        withContext(Dispatchers.IO) {
            // 예전에 받아 둔 다른 버전 파일은 지운다
            folder.mkdirs()
            folder.listFiles()?.forEach { it.delete() }
            val target = File(folder, "geupsik-alimi-$tag.apk")
            val partial = File(folder, target.name + ".part")

            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("빈 응답")
                val total = body.contentLength().takeIf { it > 0 }
                body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            done += read
                            onProgress(total?.let { done.toFloat() / it })
                        }
                    }
                }
            }
            if (!partial.renameTo(target)) throw IOException("파일 저장 실패")
            target
        }

    /** "출처를 알 수 없는 앱" 설치를 이 앱에 허용했는지 */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** 이 앱의 "출처를 알 수 없는 앱 설치" 허용 화면을 연다 */
    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** 안드로이드 설치 화면을 띄운다. 못 띄우면 false */
    fun install(apk: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
