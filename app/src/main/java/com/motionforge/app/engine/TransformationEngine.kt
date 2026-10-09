package com.motionforge.app.engine

import com.motionforge.app.model.Layer
import com.motionforge.app.model.LayerTransform
import kotlin.math.cos
import kotlin.math.sin

data class EvaluatedTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val anchorX: Float = 0.5f,
    val anchorY: Float = 0.5f,
    val opacity: Float = 1f
)

object TransformationEngine {

    fun evaluateLayer(
        layer: Layer,
        allLayers: List<Layer>,
        timeMs: Long,
        compositionWidth: Int,
        compositionHeight: Int
    ): EvaluatedTransform {
        val visited = mutableSetOf<String>()
        return resolveRecursive(
            layer = layer,
            allLayers = allLayers,
            timeMs = timeMs,
            visited = visited,
            compositionWidth = compositionWidth,
            compositionHeight = compositionHeight
        )
    }

    private fun resolveRecursive(
        layer: Layer,
        allLayers: List<Layer>,
        timeMs: Long,
        visited: MutableSet<String>,
        compositionWidth: Int,
        compositionHeight: Int
    ): EvaluatedTransform {
        if (!visited.add(layer.id)) {
            // Cyclic parenting loop guard
            return evaluateLocal(layer.transform, timeMs)
        }

        val local = evaluateLocal(layer.transform, timeMs)

        val parentId = layer.parentLayerId
        if (parentId == null) {
            return local
        }

        val parent = allLayers.firstOrNull { it.id == parentId } ?: return local
        val parentTransform = resolveRecursive(
            layer = parent,
            allLayers = allLayers,
            timeMs = timeMs,
            visited = visited,
            compositionWidth = compositionWidth,
            compositionHeight = compositionHeight
        )

        // Concatenate child local transform with parent transform
        // Child's position rotates around parent's position by parent's rotation, scaled by parent's scale
        val rad = Math.toRadians(parentTransform.rotation.toDouble())
        val cos = cos(rad).toFloat()
        val sin = sin(rad).toFloat()

        val scaledChildX = local.x * parentTransform.scaleX
        val scaledChildY = local.y * parentTransform.scaleY

        val rotatedX = scaledChildX * cos - scaledChildY * sin
        val rotatedY = scaledChildX * sin + scaledChildY * cos

        val combinedX = parentTransform.x + rotatedX
        val combinedY = parentTransform.y + rotatedY
        val combinedScaleX = local.scaleX * parentTransform.scaleX
        val combinedScaleY = local.scaleY * parentTransform.scaleY
        val combinedRotation = (parentTransform.rotation + local.rotation) % 360f
        val combinedOpacity = (parentTransform.opacity * local.opacity).coerceIn(0f, 1f)

        return EvaluatedTransform(
            x = combinedX,
            y = combinedY,
            scaleX = combinedScaleX,
            scaleY = combinedScaleY,
            rotation = combinedRotation,
            anchorX = local.anchorX,
            anchorY = local.anchorY,
            opacity = combinedOpacity
        )
    }

    fun evaluateLocal(transform: LayerTransform, timeMs: Long): EvaluatedTransform {
        val exprEval = { expr: String, t: Long, base: Float ->
            ExpressionEngine.evaluate(expr, t, base)
        }

        val posX = transform.positionX.evaluate(timeMs, exprEval)
        val posY = transform.positionY.evaluate(timeMs, exprEval)

        val sX = transform.scaleX.evaluate(timeMs, exprEval)
        val sY = if (transform.uniformScale) sX else transform.scaleY.evaluate(timeMs, exprEval)

        val rot = transform.rotation.evaluate(timeMs, exprEval)
        val ancX = transform.anchorX.evaluate(timeMs, exprEval)
        val ancY = transform.anchorY.evaluate(timeMs, exprEval)
        val op = transform.opacity.evaluate(timeMs, exprEval).coerceIn(0f, 1f)

        return EvaluatedTransform(
            x = posX,
            y = posY,
            scaleX = sX,
            scaleY = sY,
            rotation = rot,
            anchorX = ancX,
            anchorY = ancY,
            opacity = op
        )
    }
}
