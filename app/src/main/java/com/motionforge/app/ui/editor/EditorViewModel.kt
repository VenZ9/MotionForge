package com.motionforge.app.ui.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionforge.app.data.HistoryManager
import com.motionforge.app.data.ProjectRepository
import com.motionforge.app.engine.ExportEngine
import com.motionforge.app.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class EditorViewModel(
    private val repository: ProjectRepository,
    initialProject: Project
) : ViewModel() {

    private val _project = MutableStateFlow(initialProject)
    val project: StateFlow<Project> = _project.asStateFlow()

    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(
        initialProject.layers.firstOrNull()?.id
    )
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private val historyManager = HistoryManager()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Export State
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportCurrentFrame = MutableStateFlow(0)
    val exportCurrentFrame: StateFlow<Int> = _exportCurrentFrame.asStateFlow()

    private val _exportTotalFrames = MutableStateFlow(0)
    val exportTotalFrames: StateFlow<Int> = _exportTotalFrames.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    private val cancelExportFlag = AtomicBoolean(false)
    private var playbackJob: Job? = null
    private var autoSaveJob: Job? = null

    init {
        updateUndoRedoState()
    }

    fun seekTo(timeMs: Long) {
        val clamped = timeMs.coerceIn(0L, _project.value.durationMs)
        _currentTimeMs.value = clamped
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (_currentTimeMs.value >= _project.value.durationMs) {
            _currentTimeMs.value = 0L
        }
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val frameIntervalMs = (1000f / _project.value.fps).toLong().coerceAtLeast(16L)
            while (isActive && _isPlaying.value) {
                delay(frameIntervalMs)
                val nextTime = _currentTimeMs.value + frameIntervalMs
                if (nextTime >= _project.value.durationMs) {
                    _currentTimeMs.value = _project.value.durationMs
                    _isPlaying.value = false
                    break
                } else {
                    _currentTimeMs.value = nextTime
                }
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun stepFrame(deltaFrames: Int) {
        pause()
        val frameDurationMs = (1000f / _project.value.fps).toLong().coerceAtLeast(1L)
        seekTo(_currentTimeMs.value + deltaFrames * frameDurationMs)
    }

    fun selectLayer(id: String?) {
        _selectedLayerId.value = id
    }

    fun mutateProject(block: (Project) -> Project) {
        historyManager.pushState(_project.value)
        _project.value = block(_project.value)
        updateUndoRedoState()
        scheduleAutoSave()
    }

    fun updateSelectedLayer(update: (Layer) -> Layer) {
        val selId = _selectedLayerId.value ?: return
        mutateProject { current ->
            current.copy(
                layers = current.layers.map { layer ->
                    if (layer.id == selId) update(layer) else layer
                }
            )
        }
    }

    fun addLayer(type: LayerType) {
        val current = _project.value
        val newTrackIndex = (current.layers.maxOfOrNull { it.trackIndex } ?: -1) + 1
        val newLayer = when (type) {
            LayerType.TEXT -> Layer(
                name = "Text ${current.layers.count { it.type == LayerType.TEXT } + 1}",
                type = LayerType.TEXT,
                trackIndex = newTrackIndex,
                startTimeMs = _currentTimeMs.value,
                durationMs = 3000L.coerceAtMost(current.durationMs - _currentTimeMs.value).coerceAtLeast(1000L),
                textData = TextLayerData(text = "New Text Layer")
            )
            LayerType.SHAPE -> Layer(
                name = "Shape ${current.layers.count { it.type == LayerType.SHAPE } + 1}",
                type = LayerType.SHAPE,
                trackIndex = newTrackIndex,
                startTimeMs = _currentTimeMs.value,
                durationMs = 3000L.coerceAtMost(current.durationMs - _currentTimeMs.value).coerceAtLeast(1000L),
                shapeData = ShapeLayerData(shapeType = ShapeType.ROUNDED_RECT)
            )
            LayerType.ADJUSTMENT -> Layer(
                name = "Adjustment Layer",
                type = LayerType.ADJUSTMENT,
                trackIndex = newTrackIndex,
                startTimeMs = 0L,
                durationMs = current.durationMs,
                effects = listOf(LayerEffect.ColorGrading())
            )
            LayerType.IMAGE -> Layer(
                name = "Image Layer",
                type = LayerType.IMAGE,
                trackIndex = newTrackIndex,
                startTimeMs = _currentTimeMs.value,
                durationMs = 3000L.coerceAtMost(current.durationMs - _currentTimeMs.value).coerceAtLeast(1000L),
                mediaData = MediaLayerData()
            )
            LayerType.VIDEO -> Layer(
                name = "Video Layer",
                type = LayerType.VIDEO,
                trackIndex = newTrackIndex,
                startTimeMs = _currentTimeMs.value,
                durationMs = 3000L.coerceAtMost(current.durationMs - _currentTimeMs.value).coerceAtLeast(1000L),
                mediaData = MediaLayerData()
            )
            LayerType.AUDIO -> Layer(
                name = "Audio Track",
                type = LayerType.AUDIO,
                trackIndex = newTrackIndex,
                startTimeMs = _currentTimeMs.value,
                durationMs = 3000L.coerceAtMost(current.durationMs - _currentTimeMs.value).coerceAtLeast(1000L),
                audioData = AudioLayerData()
            )
            LayerType.NESTED_COMP -> Layer(
                name = "Nested Comp",
                type = LayerType.NESTED_COMP,
                trackIndex = newTrackIndex,
                startTimeMs = _currentTimeMs.value,
                durationMs = 3000L.coerceAtMost(current.durationMs - _currentTimeMs.value).coerceAtLeast(1000L)
            )
        }

        mutateProject { it.copy(layers = it.layers + newLayer) }
        _selectedLayerId.value = newLayer.id
    }

    fun splitLayerAtPlayhead(layerId: String) {
        val current = _project.value
        val target = current.layers.firstOrNull { it.id == layerId } ?: return
        val playhead = _currentTimeMs.value

        if (playhead <= target.startTimeMs || playhead >= target.endTimeMs) return

        val durationFirstPart = playhead - target.startTimeMs
        val durationSecondPart = target.endTimeMs - playhead

        val firstPart = target.copy(durationMs = durationFirstPart)
        val secondPart = target.copy(
            id = UUID.randomUUID().toString(),
            name = "${target.name} (Split)",
            startTimeMs = playhead,
            durationMs = durationSecondPart
        )

        mutateProject {
            it.copy(layers = it.layers.map { l -> if (l.id == target.id) firstPart else l } + secondPart)
        }
    }

    fun duplicateLayer(layerId: String) {
        val target = _project.value.layers.firstOrNull { it.id == layerId } ?: return
        val dup = target.copy(
            id = UUID.randomUUID().toString(),
            name = "${target.name} (Copy)",
            trackIndex = target.trackIndex + 1,
            startTimeMs = (target.startTimeMs + 200L).coerceAtMost(_project.value.durationMs - 500L)
        )
        mutateProject { it.copy(layers = it.layers + dup) }
        _selectedLayerId.value = dup.id
    }

    fun deleteLayer(layerId: String) {
        mutateProject {
            it.copy(layers = it.layers.filterNot { l -> l.id == layerId })
        }
        if (_selectedLayerId.value == layerId) {
            _selectedLayerId.value = _project.value.layers.firstOrNull()?.id
        }
    }

    fun toggleLayerVisibility(layerId: String) {
        mutateProject {
            it.copy(layers = it.layers.map { l -> if (l.id == layerId) l.copy(isVisible = !l.isVisible) else l })
        }
    }

    fun toggleLayerLock(layerId: String) {
        mutateProject {
            it.copy(layers = it.layers.map { l -> if (l.id == layerId) l.copy(isLocked = !l.isLocked) else l })
        }
    }

    fun jumpToPrevKeyframe() {
        val sel = _project.value.layers.firstOrNull { it.id == _selectedLayerId.value } ?: return
        val times = sel.allKeyframeTimes().filter { it < _currentTimeMs.value }.sortedDescending()
        times.firstOrNull()?.let { seekTo(it) }
    }

    fun jumpToNextKeyframe() {
        val sel = _project.value.layers.firstOrNull { it.id == _selectedLayerId.value } ?: return
        val times = sel.allKeyframeTimes().filter { it > _currentTimeMs.value }.sorted()
        times.firstOrNull()?.let { seekTo(it) }
    }

    fun undo() {
        val previous = historyManager.undo(_project.value) ?: return
        _project.value = previous
        updateUndoRedoState()
        scheduleAutoSave()
    }

    fun redo() {
        val next = historyManager.redo(_project.value) ?: return
        _project.value = next
        updateUndoRedoState()
        scheduleAutoSave()
    }

    private fun updateUndoRedoState() {
        _canUndo.value = historyManager.canUndo
        _canRedo.value = historyManager.canRedo
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(800L) // debounce
            repository.saveProject(_project.value)
        }
    }

    fun startExport(context: Context, width: Int, height: Int, fps: Int, bitrate: Int) {
        cancelExportFlag.set(false)
        _isExporting.value = true
        _exportProgress.value = 0f
        _exportedFile.value = null
        _exportError.value = null

        viewModelScope.launch {
            val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
            val fileName = "MotionForge_${System.currentTimeMillis()}.mp4"
            val destFile = File(exportDir, fileName)

            val config = ExportEngine.ExportConfig(
                width = width,
                height = height,
                fps = fps,
                bitrate = bitrate,
                outputFile = destFile
            )

            val result = ExportEngine.exportProjectToMp4(
                context = context,
                project = _project.value,
                config = config,
                cancelFlag = cancelExportFlag,
                onProgress = { progress, frame, total ->
                    _exportProgress.value = progress
                    _exportCurrentFrame.value = frame
                    _exportTotalFrames.value = total
                }
            )

            result.onSuccess { file ->
                _isExporting.value = false
                _exportedFile.value = file
            }.onFailure { err ->
                _isExporting.value = false
                _exportError.value = err.message ?: "Export failed"
            }
        }
    }

    fun cancelExport() {
        cancelExportFlag.set(true)
        _isExporting.value = false
    }

    fun dismissExport() {
        _isExporting.value = false
        _exportedFile.value = null
        _exportError.value = null
    }
}
