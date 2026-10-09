package com.motionforge.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionforge.app.model.LayerType
import com.motionforge.app.ui.theme.*

@Composable
fun TimelineControls(
    isPlaying: Boolean,
    hasSelectedLayer: Boolean,
    onTogglePlay: () -> Unit,
    onStepPrevFrame: () -> Unit,
    onStepNextFrame: () -> Unit,
    onJumpPrevKeyframe: () -> Unit,
    onJumpNextKeyframe: () -> Unit,
    onSplitLayer: () -> Unit,
    onDuplicateLayer: () -> Unit,
    onDeleteLayer: () -> Unit,
    onAddLayer: (LayerType) -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }

    Surface(
        color = SurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Quick layer actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSplitLayer,
                    enabled = hasSelectedLayer
                ) {
                    Icon(
                        Icons.Default.ContentCut,
                        contentDescription = "Split",
                        tint = if (hasSelectedLayer) TextPrimary else TextTertiary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDuplicateLayer,
                    enabled = hasSelectedLayer
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Duplicate",
                        tint = if (hasSelectedLayer) TextPrimary else TextTertiary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteLayer,
                    enabled = hasSelectedLayer
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = if (hasSelectedLayer) AccentRose else TextTertiary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Center Group: Playback & Frame Stepping
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onJumpPrevKeyframe) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Prev Keyframe", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onStepPrevFrame) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Step -1 Frame", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                FilledIconButton(
                    onClick = onTogglePlay,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentCyan,
                        contentColor = BgDark
                    ),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onStepNextFrame) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Step +1 Frame", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onJumpNextKeyframe) {
                    Icon(Icons.Default.FastForward, contentDescription = "Next Keyframe", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            // Right Group: Add Layer FAB Button
            Box {
                Button(
                    onClick = { showAddMenu = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentViolet,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Layer", fontSize = 12.sp)
                }

                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Text Layer") },
                        leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = TrackColorText) },
                        onClick = {
                            showAddMenu = false
                            onAddLayer(LayerType.TEXT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Vector Shape Layer") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = TrackColorShape) },
                        onClick = {
                            showAddMenu = false
                            onAddLayer(LayerType.SHAPE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Adjustment Layer") },
                        leadingIcon = { Icon(Icons.Default.FilterVintage, contentDescription = null, tint = TrackColorAdjustment) },
                        onClick = {
                            showAddMenu = false
                            onAddLayer(LayerType.ADJUSTMENT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Image Layer") },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = TrackColorImage) },
                        onClick = {
                            showAddMenu = false
                            onAddLayer(LayerType.IMAGE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Video Layer") },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = TrackColorVideo) },
                        onClick = {
                            showAddMenu = false
                            onAddLayer(LayerType.VIDEO)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Audio Track") },
                        leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, tint = TrackColorAudio) },
                        onClick = {
                            showAddMenu = false
                            onAddLayer(LayerType.AUDIO)
                        }
                    )
                }
            }
        }
    }
}
