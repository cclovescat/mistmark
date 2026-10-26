package com.ccwatermark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ccwatermark.app.data.AppViewModel
import com.ccwatermark.app.ui.components.AppHeader
import com.ccwatermark.app.ui.components.GlassBackground
import com.ccwatermark.app.ui.components.GlassBottomBar
import com.ccwatermark.app.ui.screens.HomeScreen
import com.ccwatermark.app.ui.screens.PresetsScreen
import com.ccwatermark.app.ui.screens.SettingsScreen
import com.ccwatermark.app.ui.theme.MistMarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MistMarkApp() }
    }
}

@Composable
fun MistMarkApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris -> viewModel.addPhotos(uris) }
    val preset = state.presets.find { it.id == state.selectedPresetId }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }
    BackHandler(enabled = tab != 0) { tab = 0 }

    MistMarkTheme(state.themeMode, state.fontSize) {
    if (!state.loaded) {
        GlassBackground { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        return@MistMarkTheme
    }
    GlassBackground {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = { Box(Modifier.navigationBarsPadding().padding(bottom = 10.dp)) { GlassBottomBar(tab) { tab = it } } },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
                    AppHeader()
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            0 -> HomeScreen(
                                state, preset,
                                onOpacity = viewModel::setOpacity,
                                onAddPhotos = { if (!state.busy) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                onGoPresets = { tab = 1 },
                                onSelectPreset = viewModel::selectPreset,
                                onSelectPhoto = viewModel::selectPhoto,
                                onRemovePhoto = viewModel::removePhoto,
                                onClear = viewModel::clearPhotos,
                                onProcess = viewModel::processAll,
                                onSave = viewModel::saveAll,
                            )
                            1 -> PresetsScreen(state, viewModel::importWatermark, viewModel::selectPreset, viewModel::renamePreset, viewModel::togglePreset, viewModel::deletePreset)
                            else -> SettingsScreen(state, viewModel::setThemeMode, viewModel::setFontSize)
                        }
                    }
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp, start = 20.dp, end = 20.dp),
            )
        }
    }
    }
}
