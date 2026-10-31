package com.ccwatermark.app.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ccwatermark.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class UpdateStatus { IDLE, CHECKING, LATEST, AVAILABLE, DOWNLOADING, READY_TO_INSTALL, ERROR }

data class UpdateUiState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val status: UpdateStatus = UpdateStatus.IDLE,
    val release: ReleaseInfo? = null,
    val progress: Float = 0f,
    val downloadedApkPath: String? = null,
    val showDialog: Boolean = false,
    val errorMessage: String? = null,
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = GitHubUpdateManager(application)
    private var downloadJob: Job? = null
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun onUpdateClick() {
        when (_state.value.status) {
            UpdateStatus.AVAILABLE -> _state.value = _state.value.copy(showDialog = true)
            UpdateStatus.READY_TO_INSTALL -> installUpdate()
            UpdateStatus.ERROR -> if (_state.value.release != null) {
                _state.value = _state.value.copy(showDialog = true)
            } else checkForUpdates()
            UpdateStatus.DOWNLOADING -> _state.value = _state.value.copy(showDialog = true)
            UpdateStatus.CHECKING -> Unit
            else -> checkForUpdates()
        }
    }

    fun checkForUpdates() {
        if (_state.value.status == UpdateStatus.CHECKING || _state.value.status == UpdateStatus.DOWNLOADING) return
        _state.value = UpdateUiState(status = UpdateStatus.CHECKING)
        viewModelScope.launch {
            try {
                val release = manager.latestRelease()
                if (VersionComparator.compare(release.version, BuildConfig.VERSION_NAME) > 0) {
                    _state.value = _state.value.copy(status = UpdateStatus.AVAILABLE, release = release, showDialog = true)
                } else {
                    _state.value = _state.value.copy(status = UpdateStatus.LATEST)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _state.value = _state.value.copy(status = UpdateStatus.ERROR, errorMessage = error.message ?: "检查更新失败，请稍后重试")
            }
        }
    }

    fun downloadUpdate() {
        val release = _state.value.release ?: return
        if (_state.value.status == UpdateStatus.DOWNLOADING) return
        _state.value = _state.value.copy(status = UpdateStatus.DOWNLOADING, progress = 0f, errorMessage = null, showDialog = true)
        val job = viewModelScope.launch {
            val owner = coroutineContext[Job]
            try {
                val apk = manager.download(release) { progress ->
                    if (downloadJob === owner) {
                        _state.value = _state.value.copy(progress = progress)
                    }
                }
                if (downloadJob !== owner) return@launch
                _state.value = _state.value.copy(status = UpdateStatus.READY_TO_INSTALL, downloadedApkPath = apk.absolutePath)
                try {
                    manager.install(apk)
                } catch (error: Throwable) {
                    if (downloadJob === owner) {
                        _state.value = _state.value.copy(errorMessage = error.message ?: "无法打开系统安装器")
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (downloadJob === owner) {
                    _state.value = _state.value.copy(status = UpdateStatus.ERROR, errorMessage = error.message ?: "下载失败，请稍后重试")
                }
            } finally {
                if (downloadJob === owner) downloadJob = null
            }
        }
        downloadJob = job
    }

    fun cancelDownload() {
        val job = downloadJob ?: return
        job.cancel()
        if (downloadJob === job) downloadJob = null
        _state.value = _state.value.copy(status = UpdateStatus.AVAILABLE, progress = 0f, showDialog = false)
    }

    fun installUpdate() {
        val path = _state.value.downloadedApkPath ?: return
        val apk = File(path)
        if (!apk.isFile) {
            _state.value = _state.value.copy(
                status = UpdateStatus.AVAILABLE,
                downloadedApkPath = null,
                progress = 0f,
                showDialog = true,
                errorMessage = "安装包已被清理，请重新下载",
            )
            return
        }
        try {
            manager.install(apk)
        } catch (error: Throwable) {
            _state.value = _state.value.copy(errorMessage = error.message ?: "无法打开系统安装器")
        }
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(showDialog = false)
    }
}
