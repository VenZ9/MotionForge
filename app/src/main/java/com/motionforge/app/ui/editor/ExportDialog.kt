package com.motionforge.app.ui.editor

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.motionforge.app.model.Project
import com.motionforge.app.ui.theme.*
import java.io.File

@Composable
fun ExportDialog(
    project: Project,
    isExporting: Boolean,
    exportProgress: Float,
    currentFrame: Int,
    totalFrames: Int,
    exportedFile: File?,
    exportError: String?,
    onDismiss: () -> Unit,
    onStartExport: (width: Int, height: Int, fps: Int, bitrate: Int) -> Unit,
    onCancelExport: () -> Unit
) {
    val context = LocalContext.current
    var selectedRes by remember { mutableStateOf("1080p") }
    var selectedFps by remember { mutableIntStateOf(project.fps) }
    var selectedBitrate by remember { mutableIntStateOf(8_000_000) }

    AlertDialog(
        onDismissRequest = {
            if (!isExporting) onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.IosShare, contentDescription = null, tint = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (exportedFile != null) "Export Complete!" else "Export Project (MP4)",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (exportedFile != null) {
                    // Success View
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ready for sharing!", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("File: ${exportedFile.name}", fontSize = 12.sp, color = TextSecondary)
                            Text("Size: ${exportedFile.length() / (1024 * 1024)} MB", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { shareVideoFile(context, exportedFile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share")
                                }
                                OutlinedButton(
                                    onClick = { openVideoFile(context, exportedFile) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open")
                                }
                            }
                        }
                    }
                } else if (isExporting) {
                    // Export In Progress View
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Encoding Hardware H.264 MP4...",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentCyan,
                            trackColor = SurfaceElevated
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${(exportProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, color = AccentCyan)
                            Text("Frame: $currentFrame / $totalFrames", fontSize = 12.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onCancelExport,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose)
                        ) {
                            Text("Cancel Export")
                        }
                    }
                } else {
                    // Export Settings Config View
                    if (exportError != null) {
                        Text("Error: $exportError", color = AccentRose, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text("Resolution", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("720p", "1080p", "4K").forEach { res ->
                            FilterChip(
                                selected = selectedRes == res,
                                onClick = { selectedRes = res },
                                label = { Text(res) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Frame Rate", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(24, 30, 60).forEach { fps ->
                            FilterChip(
                                selected = selectedFps == fps,
                                onClick = { selectedFps = fps },
                                label = { Text("${fps}fps") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Bitrate / Quality", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            4_000_000 to "Standard",
                            8_000_000 to "High",
                            16_000_000 to "Ultra"
                        ).forEach { (rate, label) ->
                            FilterChip(
                                selected = selectedBitrate == rate,
                                onClick = { selectedBitrate = rate },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = SurfaceElevated,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "100% Free Forever · No Watermark · Hardware Accelerated",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (exportedFile != null) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgDark)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            } else if (!isExporting) {
                Button(
                    onClick = {
                        val (w, h) = when (selectedRes) {
                            "720p" -> if (project.width > project.height) 1280 to 720 else 720 to 1280
                            "4K" -> if (project.width > project.height) 3840 to 2160 else 2160 to 3840
                            else -> if (project.width > project.height) 1920 to 1080 else 1080 to 1920
                        }
                        onStartExport(w, h, selectedFps, selectedBitrate)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgDark)
                ) {
                    Text("Start Export", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isExporting && exportedFile == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        },
        containerColor = SurfaceDark
    )
}

private fun shareVideoFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    } catch (_: Exception) {}
}

private fun openVideoFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(viewIntent)
    } catch (_: Exception) {}
}
