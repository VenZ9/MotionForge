package com.motionforge.app.ui.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.motionforge.app.model.EasingCurve
import com.motionforge.app.ui.theme.BgDark

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val project by viewModel.project.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val isExporting by viewModel.isExporting.collectAsState()
    val exportProgress by viewModel.exportProgress.collectAsState()
    val currentFrame by viewModel.exportCurrentFrame.collectAsState()
    val totalFrames by viewModel.exportTotalFrames.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()
    val exportError by viewModel.exportError.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showGraphEditor by remember { mutableStateOf(false) }
    var activeEasingCurve by remember { mutableStateOf(EasingCurve.EaseInOut) }
    var safeMarginsEnabled by remember { mutableStateOf(false) }

    val selectedLayer = remember(project.layers, selectedLayerId) {
        project.layers.firstOrNull { it.id == selectedLayerId }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            EditorTopBar(
                project = project,
                currentTimeMs = currentTimeMs,
                canUndo = canUndo,
                canRedo = canRedo,
                safeMarginEnabled = safeMarginsEnabled,
                onToggleSafeMargins = { safeMarginsEnabled = !safeMarginsEnabled },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onBack = {
                    viewModel.pause()
                    onBackToDashboard()
                },
                onExportClick = { showExportDialog = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Preview Canvas View (upper region)
            PreviewCanvas(
                project = project,
                currentTimeMs = currentTimeMs,
                selectedLayerId = selectedLayerId,
                safeMarginsEnabled = safeMarginsEnabled,
                onUpdateLayer = { updater -> viewModel.updateSelectedLayer(updater) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
            )

            // 2. Playback and Layer Editing Controls Bar
            TimelineControls(
                isPlaying = isPlaying,
                hasSelectedLayer = selectedLayer != null,
                onTogglePlay = { viewModel.togglePlayPause() },
                onStepPrevFrame = { viewModel.stepFrame(-1) },
                onStepNextFrame = { viewModel.stepFrame(1) },
                onJumpPrevKeyframe = { viewModel.jumpToPrevKeyframe() },
                onJumpNextKeyframe = { viewModel.jumpToNextKeyframe() },
                onSplitLayer = { selectedLayerId?.let { viewModel.splitLayerAtPlayhead(it) } },
                onDuplicateLayer = { selectedLayerId?.let { viewModel.duplicateLayer(it) } },
                onDeleteLayer = { selectedLayerId?.let { viewModel.deleteLayer(it) } },
                onAddLayer = { layerType -> viewModel.addLayer(layerType) }
            )

            // 3. Multitrack Timeline (middle region)
            MultitrackTimeline(
                project = project,
                currentTimeMs = currentTimeMs,
                selectedLayerId = selectedLayerId,
                onSeek = { targetMs -> viewModel.seekTo(targetMs) },
                onSelectLayer = { id -> viewModel.selectLayer(id) },
                onToggleVisibility = { id -> viewModel.toggleLayerVisibility(id) },
                onToggleLock = { id -> viewModel.toggleLayerLock(id) },
                onUpdateLayer = { updater -> viewModel.updateSelectedLayer(updater) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.28f)
            )

            // 4. Contextual Inspector Panel (bottom region)
            InspectorPanel(
                layer = selectedLayer,
                allLayers = project.layers,
                currentTimeMs = currentTimeMs,
                onUpdateLayer = { updater -> viewModel.updateSelectedLayer(updater) },
                onOpenGraphEditor = { curve ->
                    activeEasingCurve = curve
                    showGraphEditor = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.30f)
            )
        }
    }

    // Graph Editor Modal
    if (showGraphEditor) {
        GraphEditorDialog(
            initialCurve = activeEasingCurve,
            onDismiss = { showGraphEditor = false },
            onApplyCurve = { newCurve ->
                showGraphEditor = false
                viewModel.updateSelectedLayer { layer ->
                    val updatedPosX = layer.transform.positionX.copy(
                        keyframes = layer.transform.positionX.keyframes.map { it.copy(easing = newCurve) }
                    )
                    layer.copy(transform = layer.transform.copy(positionX = updatedPosX))
                }
            }
        )
    }

    // Export Modal Dialog
    if (showExportDialog || isExporting || exportedFile != null) {
        ExportDialog(
            project = project,
            isExporting = isExporting,
            exportProgress = exportProgress,
            currentFrame = currentFrame,
            totalFrames = totalFrames,
            exportedFile = exportedFile,
            exportError = exportError,
            onDismiss = {
                showExportDialog = false
                viewModel.dismissExport()
            },
            onStartExport = { w, h, fps, bitrate ->
                viewModel.startExport(context, w, h, fps, bitrate)
            },
            onCancelExport = {
                viewModel.cancelExport()
            }
        )
    }
}
