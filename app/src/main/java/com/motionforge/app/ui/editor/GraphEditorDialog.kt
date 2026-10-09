package com.motionforge.app.ui.editor

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionforge.app.model.EasingCurve
import com.motionforge.app.model.EasingType
import com.motionforge.app.ui.theme.*

@Composable
fun GraphEditorDialog(
    initialCurve: EasingCurve,
    onDismiss: () -> Unit,
    onApplyCurve: (EasingCurve) -> Unit
) {
    var cp1x by remember { mutableFloatStateOf(initialCurve.cp1x.coerceIn(0f, 1f)) }
    var cp1y by remember { mutableFloatStateOf(initialCurve.cp1y.coerceIn(0f, 1f)) }
    var cp2x by remember { mutableFloatStateOf(initialCurve.cp2x.coerceIn(0f, 1f)) }
    var cp2y by remember { mutableFloatStateOf(initialCurve.cp2y.coerceIn(0f, 1f)) }

    val currentCurve = remember(cp1x, cp1y, cp2x, cp2y) {
        EasingCurve(EasingType.BEZIER, cp1x, cp1y, cp2x, cp2y)
    }

    // Live preview ball animation using the curve
    val infiniteTransition = rememberInfiniteTransition(label = "GraphPreview")
    val rawProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RawProgress"
    )
    val easedProgress = remember(rawProgress, currentCurve) {
        currentCurve.interpolate(rawProgress)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bézier Graph Editor", fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Drag control points CP1 (Cyan) & CP2 (Violet) to shape velocity easing curve.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Graph Canvas
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceHighlight, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val x = (change.position.x / size.width).coerceIn(0f, 1f)
                                val y = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                // Determine closer handle
                                val d1 = (x - cp1x) * (x - cp1x) + (y - cp1y) * (y - cp1y)
                                val d2 = (x - cp2x) * (x - cp2x) + (y - cp2y) * (y - cp2y)
                                if (d1 < d2) {
                                    cp1x = x
                                    cp1y = y
                                } else {
                                    cp2x = x
                                    cp2y = y
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Grid lines
                        for (i in 1..3) {
                            val f = i / 4f
                            drawLine(
                                color = SurfaceElevated,
                                start = Offset(f * w, 0f),
                                end = Offset(f * w, h),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = SurfaceElevated,
                                start = Offset(0f, f * h),
                                end = Offset(w, f * h),
                                strokeWidth = 1f
                            )
                        }

                        // Linear baseline
                        drawLine(
                            color = TextTertiary.copy(alpha = 0.3f),
                            start = Offset(0f, h),
                            end = Offset(w, 0f),
                            strokeWidth = 1f
                        )

                        val p0 = Offset(0f, h)
                        val p1 = Offset(cp1x * w, h - cp1y * h)
                        val p2 = Offset(cp2x * w, h - cp2y * h)
                        val p3 = Offset(w, 0f)

                        // Control point handle lines
                        drawLine(color = AccentCyan.copy(alpha = 0.6f), start = p0, end = p1, strokeWidth = 2f)
                        drawLine(color = AccentViolet.copy(alpha = 0.6f), start = p3, end = p2, strokeWidth = 2f)

                        // Draw Bézier curve
                        val path = Path()
                        path.moveTo(p0.x, p0.y)
                        path.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
                        drawPath(path, color = AccentRose, style = Stroke(width = 4f))

                        // Draw handle circles
                        drawCircle(color = AccentCyan, radius = 9f, center = p1)
                        drawCircle(color = AccentViolet, radius = 9f, center = p2)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-time Eased Preview Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Live Preview:", fontSize = 11.sp, color = TextTertiary)
                        Text("Easing Result: ${(easedProgress * 100).toInt()}%", fontSize = 11.sp, color = AccentCyan)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(SurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(24.dp)
                                .offset(x = (easedProgress * 230).dp)
                                .clip(CircleShape)
                                .background(AccentCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Easing Presets
                Text("Presets:", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        "Linear" to listOf(0f, 0f, 1f, 1f),
                        "Ease In" to listOf(0.42f, 0f, 1f, 1f),
                        "Ease Out" to listOf(0f, 0f, 0.58f, 1f),
                        "Ease In Out" to listOf(0.42f, 0f, 0.58f, 1f),
                        "Snappy" to listOf(0.4f, 0f, 0.2f, 1f),
                        "Overshoot" to listOf(0.34f, 1.4f, 0.64f, 1f)
                    )
                    items(presets) { (name, values) ->
                        OutlinedButton(
                            onClick = {
                                cp1x = values[0]
                                cp1y = values[1]
                                cp2x = values[2]
                                cp2y = values[3]
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(name, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyCurve(currentCurve) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgDark)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apply Curve", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}
