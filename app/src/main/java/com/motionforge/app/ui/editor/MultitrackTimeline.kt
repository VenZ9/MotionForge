package com.motionforge.app.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionforge.app.model.Layer
import com.motionforge.app.model.LayerType
import com.motionforge.app.model.Project
import com.motionforge.app.ui.theme.*

@Composable
fun MultitrackTimeline(
    project: Project,
    currentTimeMs: Long,
    selectedLayerId: String?,
    onSeek: (Long) -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onUpdateLayer: ((Layer) -> Layer) -> Unit,
    modifier: Modifier = Modifier
) {
    val durationMs = project.durationMs.coerceAtLeast(1000L)
    val headerWidth = 110.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
    ) {
        // Time Ruler
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(SurfaceElevated)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Header spacer
                Box(
                    modifier = Modifier
                        .width(headerWidth)
                        .fillMaxHeight()
                        .background(SurfaceDark),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "TRACKS",
                        fontSize = 10.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                // Ruler area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val progress = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek((progress * durationMs).toLong())
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeek((progress * durationMs).toLong())
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val totalSeconds = (durationMs / 1000).toInt()
                        for (sec in 0..totalSeconds) {
                            val x = (sec * 1000f / durationMs) * w
                            // Second marker
                            drawLine(
                                color = TextSecondary,
                                start = Offset(x, h - 14f),
                                end = Offset(x, h),
                                strokeWidth = 1.5f
                            )
                            // Intermediate sub-ticks
                            if (sec < totalSeconds) {
                                val halfX = x + (500f / durationMs) * w
                                drawLine(
                                    color = TextTertiary.copy(alpha = 0.5f),
                                    start = Offset(halfX, h - 8f),
                                    end = Offset(halfX, h),
                                    strokeWidth = 1f
                                )
                            }
                        }

                        // Playhead needle in ruler
                        val playheadX = (currentTimeMs.toFloat() / durationMs.toFloat()) * w
                        drawCircle(
                            color = AccentCyan,
                            radius = 6f,
                            center = Offset(playheadX, h / 2f)
                        )
                    }
                }
            }
        }

        // Multitrack Layers Rows
        val sortedLayers = project.layers.sortedByDescending { it.trackIndex }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sortedLayers, key = { it.id }) { layer ->
                    TimelineTrackRow(
                        layer = layer,
                        isSelected = layer.id == selectedLayerId,
                        totalDurationMs = durationMs,
                        headerWidth = headerWidth,
                        onSelect = { onSelectLayer(layer.id) },
                        onToggleVisibility = { onToggleVisibility(layer.id) },
                        onToggleLock = { onToggleLock(layer.id) },
                        onUpdateLayer = onUpdateLayer
                    )
                }
            }

            // Global Playhead Line across all tracks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val headerPx = headerWidth.toPx()
                val timelineWidth = size.width - headerPx
                if (timelineWidth > 0) {
                    val playheadX = headerPx + (currentTimeMs.toFloat() / durationMs.toFloat()) * timelineWidth
                    drawLine(
                        color = AccentCyan,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, size.height),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineTrackRow(
    layer: Layer,
    isSelected: Boolean,
    totalDurationMs: Long,
    headerWidth: androidx.compose.ui.unit.Dp,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onUpdateLayer: ((Layer) -> Layer) -> Unit
) {
    val trackColor = when (layer.type) {
        LayerType.VIDEO -> TrackColorVideo
        LayerType.IMAGE -> TrackColorImage
        LayerType.AUDIO -> TrackColorAudio
        LayerType.TEXT -> TrackColorText
        LayerType.SHAPE -> TrackColorShape
        LayerType.ADJUSTMENT -> TrackColorAdjustment
        LayerType.NESTED_COMP -> AccentViolet
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(if (isSelected) SurfaceHighlight else SurfaceDark)
            .border(0.5.dp, SurfaceElevated)
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header
        Row(
            modifier = Modifier
                .width(headerWidth)
                .fillMaxHeight()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Visibility",
                    tint = if (layer.isVisible) TextPrimary else TextTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock",
                    tint = if (layer.isLocked) AccentAmber else TextTertiary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = layer.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) AccentCyan else TextPrimary,
                maxLines = 1,
                fontSize = 11.sp
            )
        }

        // Timeline Clip Track
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            val startRatio = (layer.startTimeMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            val durationRatio = (layer.durationMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0.05f, 1f - startRatio)

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(durationRatio)
                    .offset(x = (startRatio * 100).dp) // approximate visual offset
                    .clip(RoundedCornerShape(6.dp))
                    .background(trackColor.copy(alpha = 0.25f))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) AccentCyan else trackColor,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .pointerInput(layer.id) {
                        if (!layer.isLocked) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaRatio = dragAmount.x / size.width
                                val deltaMs = (deltaRatio * totalDurationMs).toLong()
                                onUpdateLayer { current ->
                                    val newStart = (current.startTimeMs + deltaMs).coerceIn(0L, totalDurationMs - 500L)
                                    current.copy(startTimeMs = newStart)
                                }
                            }
                        }
                    }
            ) {
                // Keyframe Diamonds inside clip
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val keyframeTimes = layer.allKeyframeTimes()
                    val clipDuration = layer.durationMs.toFloat()

                    for (kfTime in keyframeTimes) {
                        val kfLocalTime = (kfTime - layer.startTimeMs).coerceIn(0L, layer.durationMs)
                        val kfX = (kfLocalTime / clipDuration) * size.width

                        // Draw diamond
                        val diamondPath = Path().apply {
                            moveTo(kfX, size.height / 2f - 5f)
                            lineTo(kfX + 5f, size.height / 2f)
                            lineTo(kfX, size.height / 2f + 5f)
                            lineTo(kfX - 5f, size.height / 2f)
                            close()
                        }
                        drawPath(diamondPath, color = AccentRose)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = layer.name,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
