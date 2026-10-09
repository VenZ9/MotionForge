package com.motionforge.app.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.motionforge.app.engine.RenderEngine
import com.motionforge.app.engine.TransformationEngine
import com.motionforge.app.model.AnimatableFloat
import com.motionforge.app.model.Layer
import com.motionforge.app.model.Project
import com.motionforge.app.ui.theme.*

@Composable
fun PreviewCanvas(
    project: Project,
    currentTimeMs: Long,
    selectedLayerId: String?,
    safeMarginsEnabled: Boolean,
    onUpdateLayer: ((Layer) -> Layer) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedLayer = project.layers.firstOrNull { it.id == selectedLayerId }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BgDark)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Aspect ratio container
        Box(
            modifier = Modifier
                .aspectRatio(project.aspectRatioFloat)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, SurfaceHighlight, RoundedCornerShape(8.dp))
                .background(RenderEngine.parseColor(project.backgroundColorHex, BgDark))
                .pointerInput(selectedLayerId) {
                    if (selectedLayer != null && !selectedLayer.isLocked) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Convert canvas pixel drag to composition coordinate change
                            val scaleRatio = project.width.toFloat() / size.width.toFloat()
                            val deltaCompX = dragAmount.x * scaleRatio
                            val deltaCompY = dragAmount.y * scaleRatio

                            onUpdateLayer { current ->
                                val oldPosX = current.transform.positionX.staticValue
                                val oldPosY = current.transform.positionY.staticValue
                                current.copy(
                                    transform = current.transform.copy(
                                        positionX = current.transform.positionX.copy(staticValue = oldPosX + deltaCompX),
                                        positionY = current.transform.positionY.copy(staticValue = oldPosY + deltaCompY)
                                    )
                                )
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // Render full composition frame
                RenderEngine.renderComposition(
                    drawScope = this,
                    project = project,
                    timeMs = currentTimeMs,
                    canvasWidth = canvasW,
                    canvasHeight = canvasH,
                    selectedLayerId = selectedLayerId,
                    renderGizmo = false
                )

                // Safe Margins Overlay
                if (safeMarginsEnabled) {
                    // Action Safe (90%)
                    val actionSafeW = canvasW * 0.9f
                    val actionSafeH = canvasH * 0.9f
                    drawRect(
                        color = AccentCyan.copy(alpha = 0.35f),
                        topLeft = Offset((canvasW - actionSafeW) / 2f, (canvasH - actionSafeH) / 2f),
                        size = Size(actionSafeW, actionSafeH),
                        style = Stroke(width = 1.5f)
                    )
                    // Title Safe (80%)
                    val titleSafeW = canvasW * 0.8f
                    val titleSafeH = canvasH * 0.8f
                    drawRect(
                        color = AccentAmber.copy(alpha = 0.35f),
                        topLeft = Offset((canvasW - titleSafeW) / 2f, (canvasH - titleSafeH) / 2f),
                        size = Size(titleSafeW, titleSafeH),
                        style = Stroke(width = 1.5f)
                    )
                    // Center crosshair
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(canvasW / 2f - 15f, canvasH / 2f),
                        end = Offset(canvasW / 2f + 15f, canvasH / 2f),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(canvasW / 2f, canvasH / 2f - 15f),
                        end = Offset(canvasW / 2f, canvasH / 2f + 15f),
                        strokeWidth = 1.5f
                    )
                }

                // Selected Layer Transform Gizmo
                if (selectedLayer != null && selectedLayer.isActiveAt(currentTimeMs)) {
                    val scaleFactorX = canvasW / project.width.toFloat()
                    val scaleFactorY = canvasH / project.height.toFloat()

                    val evaluated = TransformationEngine.evaluateLayer(
                        layer = selectedLayer,
                        allLayers = project.layers,
                        timeMs = currentTimeMs,
                        compositionWidth = project.width,
                        compositionHeight = project.height
                    )

                    val gizmoCenterX = canvasW / 2f + evaluated.x * scaleFactorX
                    val gizmoCenterY = canvasH / 2f + evaluated.y * scaleFactorY

                    // Draw selected target anchor and bounds ring
                    drawCircle(
                        color = AccentCyan,
                        radius = 6f,
                        center = Offset(gizmoCenterX, gizmoCenterY)
                    )
                    drawCircle(
                        color = AccentCyan.copy(alpha = 0.6f),
                        radius = 24f,
                        center = Offset(gizmoCenterX, gizmoCenterY),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}
