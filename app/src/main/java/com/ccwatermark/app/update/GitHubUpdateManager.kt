package com.ccwatermark.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.ccwatermark.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
    val digest: String? = null,
)

data class ReleaseInfo(
    val version: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String?,
)

object VersionComparator {
    fun compare(first: String, second: String): Int {
        val firstParts = parts(first)
        val secondParts = parts(second)
        val size = maxOf(firstParts.size, secondParts.size)
        for (index in 0 until size) {
            val difference = firstParts.getOrElse(index) { 0 }.compareTo(secondParts.getOrElse(index) { 0 })
            if (difference != 0) return difference
        }
        return 0
    }

    private fun parts(version: String): List<Int> {
        val normalized = version.trim().removePrefix("v").removePrefix("V").substringBefore('-')
        val values = normalized.split('.')
        require(values.isNotEmpty() && values.all { it.isNotEmpty() && it.all(Char::isDigit) }) { "无法识别版本号：$version" }
        return values.map { it.toInt() }
    }
}

class GitHubUpdateManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun latestRelease(): ReleaseInfo = withContext(Dispatchers.IO) {
        val connection = open("https://api.github.com/repos/${BuildConfig.GITHUB_REPOSITORY}/releases/latest")
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> error("仓库暂未发布正式版本")
                else -> error("检查更新失败（HTTP ${connection.responseCode}）")
            }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val release = json.decodeFromString(GitHubRelease.serializer(), response)
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: error("最新 Release 中没有 APK 文件")
            val expectedHash = apk.digest?.let {
                require(it.startsWith("sha256:")) { "无法识别 APK 校验算法" }
                it.removePrefix("sha256:")
            } ?: release.assets.firstOrNull { it.name == "${apk.name}.sha256" }?.let { readChecksum(it.downloadUrl) }
                ?: error("APK 缺少 SHA-256 校验信息")
            require(expectedHash.matches(Regex("(?i)[0-9a-f]{64}"))) { "APK 校验信息格式无效" }
            ReleaseInfo(
                version = release.tagName.removePrefix("v").removePrefix("V"),
                title = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                notes = release.body?.takeIf { it.isNotBlank() } ?: "本次更新未填写更新日志。",
                apkUrl = apk.downloadUrl,
                apkSize = apk.size,
                sha256 = expectedHash,
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(release: ReleaseInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val url = URL(release.apkUrl)
        require(url.protocol == "https" && url.host == "github.com") { "APK 下载地址不受信任" }
        val directory = File(context.cacheDir, "updates")
        if (!directory.exists() && !directory.mkdirs()) error("无法创建更新目录")
        val output = File(directory, "mistmark-${release.version}.apk")
        val temporary = File(directory, "mistmark-${release.version}.apk.part")
        directory.listFiles()?.filter { it.name.endsWith(".apk") && it.name != output.name }?.forEach { it.delete() }
        val connection = open(release.apkUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) error("下载失败（HTTP ${connection.responseCode}）")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            connection.inputStream.buffered().use { input ->
                temporary.outputStream().buffered().use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        stream.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        downloaded += count
                        if (total > 0) onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            if (downloaded == 0L) error("下载的 APK 文件为空")
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            val expectedHash = requireNotNull(release.sha256) { "APK 缺少 SHA-256 校验信息" }
            if (!actualHash.equals(expectedHash, ignoreCase = true)) error("APK 校验失败，请重新下载")
            if (output.exists() && !output.delete()) error("无法替换旧安装包")
            if (!temporary.renameTo(output)) error("无法保存安装包")
            onProgress(1f)
            output
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun install(apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun readChecksum(url: String): String {
        val uri = URL(url)
        require(uri.protocol == "https" && uri.host == "github.com") { "校验文件地址不受信任" }
        val connection = open(url)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) error("无法读取 APK 校验文件")
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            return Regex("(?i)\\b[0-9a-f]{64}\\b").find(text)?.value ?: error("APK 校验文件格式无效")
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("User-Agent", "MistMark-Android")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
}
