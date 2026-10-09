package com.motionforge.app.engine

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.motionforge.app.model.*
import kotlin.math.*
import java.util.Random

object RenderEngine {

    fun parseColor(hex: String, defaultColor: Color = Color.White): Color {
        return try {
            val clean = hex.removePrefix("#")
            val intColor = when (clean.length) {
                6 -> android.graphics.Color.parseColor("#FF$clean")
                8 -> android.graphics.Color.parseColor("#$clean")
                else -> android.graphics.Color.WHITE
            }
            Color(intColor)
        } catch (_: Exception) {
            defaultColor
        }
    }

    fun mapBlendMode(appBlendMode: AppBlendMode): BlendMode {
        return when (appBlendMode) {
            AppBlendMode.NORMAL -> BlendMode.SrcOver
            AppBlendMode.MULTIPLY -> BlendMode.Multiply
            AppBlendMode.SCREEN -> BlendMode.Screen
            AppBlendMode.OVERLAY -> BlendMode.Overlay
            AppBlendMode.DARKEN -> BlendMode.Darken
            AppBlendMode.LIGHTEN -> BlendMode.Lighten
            AppBlendMode.COLOR_DODGE -> BlendMode.ColorDodge
            AppBlendMode.COLOR_BURN -> BlendMode.ColorBurn
            AppBlendMode.HARD_LIGHT -> BlendMode.Hardlight
            AppBlendMode.SOFT_LIGHT -> BlendMode.Softlight
            AppBlendMode.DIFFERENCE -> BlendMode.Difference
            AppBlendMode.EXCLUSION -> BlendMode.Exclusion
            AppBlendMode.ADD -> BlendMode.Plus
        }
    }

    /**
     * Renders a complete composition frame onto a DrawScope at given playhead time.
     */
    fun renderComposition(
        drawScope: DrawScope,
        project: Project,
        timeMs: Long,
        canvasWidth: Float,
        canvasHeight: Float,
        selectedLayerId: String? = null,
        renderGizmo: Boolean = false
    ) {
        val scaleFactorX = canvasWidth / project.width.toFloat()
        val scaleFactorY = canvasHeight / project.height.toFloat()

        // Background color fill
        val bgColor = parseColor(project.backgroundColorHex, Color(0xFF0A0C10))
        drawScope.drawRect(
            color = bgColor,
            size = Size(canvasWidth, canvasHeight)
        )

        // Sort layers: bottom to top
        val sortedLayers = project.layers.sortedBy { it.trackIndex }

        // Track accumulated adjustment effects
        var accumulatedColorGrading: LayerEffect.ColorGrading? = null

        for (layer in sortedLayers) {
            if (!layer.isActiveAt(timeMs)) continue

            // Handle Adjustment Layer
            if (layer.type == LayerType.ADJUSTMENT) {
                val cg = layer.effects.filterIsInstance<LayerEffect.ColorGrading>().firstOrNull { it.isEnabled }
                if (cg != null) {
                    accumulatedColorGrading = cg
                }
                continue
            }

            // Evaluate transformation (with parenting resolved)
            val evaluated = TransformationEngine.evaluateLayer(
                layer = layer,
                allLayers = project.layers,
                timeMs = timeMs,
                compositionWidth = project.width,
                compositionHeight = project.height
            )

            // Render Layer
            renderSingleLayer(
                drawScope = drawScope,
                layer = layer,
                evaluated = evaluated,
                timeMs = timeMs,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                scaleX = scaleFactorX,
                scaleY = scaleFactorY,
                parentGrading = accumulatedColorGrading
            )
        }
    }

    private fun renderSingleLayer(
        drawScope: DrawScope,
        layer: Layer,
        evaluated: EvaluatedTransform,
        timeMs: Long,
        canvasWidth: Float,
        canvasHeight: Float,
        scaleX: Float,
        scaleY: Float,
        parentGrading: LayerEffect.ColorGrading?
    ) {
        val localTimeMs = layer.getLocalTimeMs(timeMs)
        val opacity = evaluated.opacity
        if (opacity <= 0.01f) return

        // Compute Camera Shake effect if enabled
        var shakeOffsetX = 0f
        var shakeOffsetY = 0f
        val shake = layer.effects.filterIsInstance<LayerEffect.Shake>().firstOrNull { it.isEnabled }
        if (shake != null) {
            val t = (localTimeMs / 1000f) * shake.frequency * shake.speed
            val rnd = Random(shake.seed.toLong())
            val phaseX = rnd.nextFloat() * 10f
            val phaseY = rnd.nextFloat() * 10f
            shakeOffsetX = (sin(t + phaseX) + sin(t * 1.7f)) * 0.5f * shake.amplitudeX
            shakeOffsetY = (cos(t + phaseY) + cos(t * 1.5f)) * 0.5f * shake.amplitudeY
        }

        // Layer Center in Canvas Space
        val centerX = (canvasWidth / 2f + (evaluated.x + shakeOffsetX) * scaleX)
        val centerY = (canvasHeight / 2f + (evaluated.y + shakeOffsetY) * scaleY)

        val blendMode = mapBlendMode(layer.blendMode)

        // Save canvas transformation state
        drawScope.drawIntoCanvas { canvas ->
            canvas.save()

            // Translate to center & Anchor point
            canvas.translate(centerX, centerY)
            canvas.rotate(evaluated.rotation)
            canvas.scale(evaluated.scaleX, evaluated.scaleY)

            // Color grading and effects
            val colorGrading = layer.effects.filterIsInstance<LayerEffect.ColorGrading>().firstOrNull { it.isEnabled }
                ?: parentGrading

            val colorFilter = createColorFilter(colorGrading)

            // Handle RGB Split effect
            val rgbSplit = layer.effects.filterIsInstance<LayerEffect.RgbSplit>().firstOrNull { it.isEnabled }

            // Render specific layer content
            when (layer.type) {
                LayerType.SHAPE -> {
                    layer.shapeData?.let { shape ->
                        renderShape(
                            drawScope = drawScope,
                            shape = shape,
                            opacity = opacity,
                            blendMode = blendMode,
                            colorFilter = colorFilter,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            rgbSplit = rgbSplit
                        )
                    }
                }
                LayerType.TEXT -> {
                    layer.textData?.let { textData ->
                        renderText(
                            drawScope = drawScope,
                            textData = textData,
                            localTimeMs = localTimeMs,
                            layerDurationMs = layer.durationMs,
                            opacity = opacity,
                            blendMode = blendMode,
                            colorFilter = colorFilter,
                            scaleX = scaleX,
                            rgbSplit = rgbSplit
                        )
                    }
                }
                LayerType.IMAGE, LayerType.VIDEO -> {
                    renderMediaPlaceholder(
                        drawScope = drawScope,
                        layer = layer,
                        opacity = opacity,
                        blendMode = blendMode,
                        colorFilter = colorFilter,
                        scaleX = scaleX,
                        scaleY = scaleY
                    )
                }
                else -> {}
            }

            // Render Glow effect if present
            val glow = layer.effects.filterIsInstance<LayerEffect.Glow>().firstOrNull { it.isEnabled }
            if (glow != null) {
                renderGlow(drawScope, glow, scaleX, scaleY)
            }

            // Render Particles effect if present
            val particles = layer.effects.filterIsInstance<LayerEffect.Particles>().firstOrNull { it.isEnabled }
            if (particles != null) {
                renderParticles(drawScope, particles, localTimeMs, scaleX, scaleY)
            }

            canvas.restore()
        }
    }

    private fun renderShape(
        drawScope: DrawScope,
        shape: ShapeLayerData,
        opacity: Float,
        blendMode: BlendMode,
        colorFilter: ColorFilter?,
        scaleX: Float,
        scaleY: Float,
        rgbSplit: LayerEffect.RgbSplit?
    ) {
        val w = shape.width * scaleX
        val h = shape.height * scaleY
        val left = -w / 2f
        val top = -h / 2f

        val fillColor = parseColor(shape.fillColorHex, Color(0xFF7C4DFF)).copy(alpha = opacity)
        val strokeColor = parseColor(shape.strokeColorHex, Color(0xFF00E5FF)).copy(alpha = opacity)

        if (rgbSplit != null && rgbSplit.amount > 0f) {
            // Render RGB Split channels: Red offset + Cyan offset
            val rad = Math.toRadians(rgbSplit.angle.toDouble())
            val dx = (cos(rad) * rgbSplit.amount * scaleX).toFloat()
            val dy = (sin(rad) * rgbSplit.amount * scaleY).toFloat()

            // Red channel shifted
            drawSingleShape(
                drawScope = drawScope,
                shape = shape,
                left = left - dx,
                top = top - dy,
                w = w,
                h = h,
                fillColor = Color.Red.copy(alpha = opacity * 0.7f),
                strokeColor = Color.Red.copy(alpha = opacity * 0.7f),
                blendMode = BlendMode.Screen,
                colorFilter = null
            )

            // Cyan channel shifted
            drawSingleShape(
                drawScope = drawScope,
                shape = shape,
                left = left + dx,
                top = top + dy,
                w = w,
                h = h,
                fillColor = Color.Cyan.copy(alpha = opacity * 0.7f),
                strokeColor = Color.Cyan.copy(alpha = opacity * 0.7f),
                blendMode = BlendMode.Screen,
                colorFilter = null
            )
        }

        drawSingleShape(
            drawScope = drawScope,
            shape = shape,
            left = left,
            top = top,
            w = w,
            h = h,
            fillColor = fillColor,
            strokeColor = strokeColor,
            blendMode = blendMode,
            colorFilter = colorFilter
        )
    }

    private fun drawSingleShape(
        drawScope: DrawScope,
        shape: ShapeLayerData,
        left: Float,
        top: Float,
        w: Float,
        h: Float,
        fillColor: Color,
        strokeColor: Color,
        blendMode: BlendMode,
        colorFilter: ColorFilter?
    ) {
        when (shape.shapeType) {
            ShapeType.RECTANGLE -> {
                drawScope.drawRect(
                    color = fillColor,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    blendMode = blendMode,
                    colorFilter = colorFilter
                )
                if (shape.strokeWidth > 0f) {
                    drawScope.drawRect(
                        color = strokeColor,
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        style = Stroke(width = shape.strokeWidth),
                        blendMode = blendMode,
                        colorFilter = colorFilter
                    )
                }
            }
            ShapeType.ROUNDED_RECT -> {
                val cr = CornerRadius(shape.cornerRadius)
                drawScope.drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = cr,
                    blendMode = blendMode,
                    colorFilter = colorFilter
                )
                if (shape.strokeWidth > 0f) {
                    drawScope.drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        cornerRadius = cr,
                        style = Stroke(width = shape.strokeWidth),
                        blendMode = blendMode,
                        colorFilter = colorFilter
                    )
                }
            }
            ShapeType.CIRCLE -> {
                drawScope.drawOval(
                    color = fillColor,
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    blendMode = blendMode,
                    colorFilter = colorFilter
                )
                if (shape.strokeWidth > 0f) {
                    drawScope.drawOval(
                        color = strokeColor,
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        style = Stroke(width = shape.strokeWidth),
                        blendMode = blendMode,
                        colorFilter = colorFilter
                    )
                }
            }
            ShapeType.STAR -> {
                val path = createStarPath(w / 2f, shape.points, shape.innerRadiusRatio)
                drawScope.drawPath(
                    path = path,
                    color = fillColor,
                    blendMode = blendMode,
                    colorFilter = colorFilter
                )
                if (shape.strokeWidth > 0f) {
                    drawScope.drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = shape.strokeWidth),
                        blendMode = blendMode,
                        colorFilter = colorFilter
                    )
                }
            }
            ShapeType.POLYGON -> {
                val path = createPolygonPath(w / 2f, shape.points.coerceAtLeast(3))
                drawScope.drawPath(
                    path = path,
                    color = fillColor,
                    blendMode = blendMode,
                    colorFilter = colorFilter
                )
                if (shape.strokeWidth > 0f) {
                    drawScope.drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = shape.strokeWidth),
                        blendMode = blendMode,
                        colorFilter = colorFilter
                    )
                }
            }
            ShapeType.TRIANGLE -> {
                val path = createPolygonPath(w / 2f, 3)
                drawScope.drawPath(
                    path = path,
                    color = fillColor,
                    blendMode = blendMode,
                    colorFilter = colorFilter
                )
                if (shape.strokeWidth > 0f) {
                    drawScope.drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = shape.strokeWidth),
                        blendMode = blendMode,
                        colorFilter = colorFilter
                    )
                }
            }
        }
    }

    private fun createStarPath(outerRadius: Float, points: Int, innerRatio: Float): Path {
        val path = Path()
        val innerRadius = outerRadius * innerRatio
        val totalPoints = points * 2
        val step = (2.0 * Math.PI) / totalPoints
        for (i in 0 until totalPoints) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = i * step - (Math.PI / 2.0)
            val x = (cos(angle) * r).toFloat()
            val y = (sin(angle) * r).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun createPolygonPath(radius: Float, sides: Int): Path {
        val path = Path()
        val step = (2.0 * Math.PI) / sides
        for (i in 0 until sides) {
            val angle = i * step - (Math.PI / 2.0)
            val x = (cos(angle) * radius).toFloat()
            val y = (sin(angle) * radius).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun renderText(
        drawScope: DrawScope,
        textData: TextLayerData,
        localTimeMs: Long,
        layerDurationMs: Long,
        opacity: Float,
        blendMode: BlendMode,
        colorFilter: ColorFilter?,
        scaleX: Float,
        rgbSplit: LayerEffect.RgbSplit?
    ) {
        val baseText = textData.text
        if (baseText.isEmpty()) return

        val paint = Paint().apply {
            color = parseColor(textData.colorHex, Color.White).toArgb()
            textSize = textData.fontSize * scaleX
            isFakeBoldText = textData.isBold
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
        }

        // Apply text animation
        val displayText = when (textData.animationType) {
            TextAnimationType.TYPEWRITER -> {
                val progress = (localTimeMs.toFloat() / (layerDurationMs * 0.7f)).coerceIn(0f, 1f)
                val charCount = (progress * baseText.length).toInt().coerceIn(0, baseText.length)
                baseText.substring(0, charCount)
            }
            TextAnimationType.GLITCH -> {
                if ((localTimeMs / 120) % 4 == 0L) {
                    baseText.map { if (Random().nextFloat() > 0.8f) '#' else it }.joinToString("")
                } else baseText
            }
            else -> baseText
        }

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas

            // Wave or bounce animation offset
            val waveOffsetY = when (textData.animationType) {
                TextAnimationType.WAVE -> sin(localTimeMs / 200f) * 15f * scaleX
                TextAnimationType.SCALE_BOUNCE -> abs(sin(localTimeMs / 300f)) * -20f * scaleX
                else -> 0f
            }

            if (rgbSplit != null && rgbSplit.amount > 0f) {
                val redPaint = Paint(paint).apply { color = android.graphics.Color.RED; alpha = (opacity * 160).toInt() }
                val cyanPaint = Paint(paint).apply { color = android.graphics.Color.CYAN; alpha = (opacity * 160).toInt() }
                nativeCanvas.drawText(displayText, -rgbSplit.amount, waveOffsetY, redPaint)
                nativeCanvas.drawText(displayText, rgbSplit.amount, waveOffsetY, cyanPaint)
            }

            nativeCanvas.drawText(displayText, 0f, waveOffsetY, paint)
        }
    }

    private fun renderMediaPlaceholder(
        drawScope: DrawScope,
        layer: Layer,
        opacity: Float,
        blendMode: BlendMode,
        colorFilter: ColorFilter?,
        scaleX: Float,
        scaleY: Float
    ) {
        // Visual card representation for imported video/image
        val w = 480f * scaleX
        val h = 270f * scaleY
        val left = -w / 2f
        val top = -h / 2f

        val cardBg = Color(0xFF1E222D).copy(alpha = opacity)
        drawScope.drawRoundRect(
            color = cardBg,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(16f),
            blendMode = blendMode,
            colorFilter = colorFilter
        )
        drawScope.drawRoundRect(
            color = Color(0xFF00E5FF).copy(alpha = opacity * 0.7f),
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(16f),
            style = Stroke(width = 3f),
            blendMode = blendMode,
            colorFilter = colorFilter
        )
    }

    private fun renderGlow(
        drawScope: DrawScope,
        glow: LayerEffect.Glow,
        scaleX: Float,
        scaleY: Float
    ) {
        val glowColor = parseColor(glow.glowColorHex, Color(0xFF00E5FF))
        val r = glow.radius * scaleX * 1.5f
        drawScope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.5f * glow.intensity),
                    glowColor.copy(alpha = 0.15f * glow.intensity),
                    Color.Transparent
                ),
                center = Offset.Zero,
                radius = r
            ),
            radius = r,
            center = Offset.Zero,
            blendMode = BlendMode.Plus
        )
    }

    private fun renderParticles(
        drawScope: DrawScope,
        particles: LayerEffect.Particles,
        localTimeMs: Long,
        scaleX: Float,
        scaleY: Float
    ) {
        val t = localTimeMs / 1000f
        val rnd = Random(42)

        for (i in 0 until particles.count) {
            val seedSpeed = 0.5f + rnd.nextFloat() * 1.5f
            val angle = rnd.nextFloat() * 2f * PI.toFloat()
            val distance = ((t * particles.speed * seedSpeed * 150f) % 350f) * scaleX

            val px = cos(angle) * distance
            val py = sin(angle) * distance

            val pSize = particles.size * (0.6f + rnd.nextFloat() * 0.8f) * scaleX
            val pAlpha = (1f - (distance / (350f * scaleX))).coerceIn(0f, 1f)

            val pColor = when (particles.particleType) {
                ParticleType.STARS -> Color(0xFFFFFFFF)
                ParticleType.SPARKS -> Color(0xFFFFD600)
                ParticleType.SNOW -> Color(0xFFE0F7FA)
                ParticleType.DUST -> Color(0xFFB0BEC5)
                ParticleType.CONFETTI -> {
                    val colors = listOf(Color(0xFFFF3D71), Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFFD600))
                    colors[i % colors.size]
                }
                ParticleType.FIRE -> Color(0xFFFF5722)
            }.copy(alpha = pAlpha)

            drawScope.drawCircle(
                color = pColor,
                radius = pSize,
                center = Offset(px, py),
                blendMode = BlendMode.Plus
            )
        }
    }

    private fun createColorFilter(cg: LayerEffect.ColorGrading?): ColorFilter? {
        if (cg == null) return null

        val cm = ColorMatrix()

        // Contrast and Brightness
        val contrast = cg.contrast
        val brightness = cg.brightness * 255f
        val c = contrast
        val b = brightness + (1f - c) * 128f / 255f

        // Saturation
        val satMatrix = ColorMatrix().apply { setSaturation(cg.saturation) }

        // Temperature / Tint
        val temp = cg.temperature // -1 to 1: cool (blue) to warm (red/orange)
        val rScale = 1f + temp * 0.2f
        val bScale = 1f - temp * 0.2f

        val scaleMatrix = ColorMatrix(
            floatArrayOf(
                c * rScale, 0f, 0f, 0f, b,
                0f, c, 0f, 0f, b,
                0f, 0f, c * bScale, 0f, b,
                0f, 0f, 0f, 1f, 0f
            )
        )

        scaleMatrix.preConcat(satMatrix)
        return ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(scaleMatrix.array))
    }
}
