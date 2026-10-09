package com.motionforge.app.model

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.abs

@Serializable
enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    BEZIER,
    SPRING,
    BOUNCE,
    STEP
}

@Serializable
data class EasingCurve(
    val type: EasingType = EasingType.LINEAR,
    val cp1x: Float = 0.25f,
    val cp1y: Float = 0.1f,
    val cp2x: Float = 0.25f,
    val cp2y: Float = 1.0f
) {
    companion object {
        val Linear = EasingCurve(EasingType.LINEAR)
        val EaseIn = EasingCurve(EasingType.EASE_IN, 0.42f, 0f, 1f, 1f)
        val EaseOut = EasingCurve(EasingType.EASE_OUT, 0f, 0f, 0.58f, 1f)
        val EaseInOut = EasingCurve(EasingType.EASE_IN_OUT, 0.42f, 0f, 0.58f, 1f)
        val Spring = EasingCurve(EasingType.SPRING)
        val Bounce = EasingCurve(EasingType.BOUNCE)
        val Step = EasingCurve(EasingType.STEP)

        fun bezier(cp1x: Float, cp1y: Float, cp2x: Float, cp2y: Float) =
            EasingCurve(EasingType.BEZIER, cp1x, cp1y, cp2x, cp2y)
    }

    fun interpolate(progress: Float): Float {
        val t = progress.coerceIn(0f, 1f)
        return when (type) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN -> t * t
            EasingType.EASE_OUT -> t * (2f - t)
            EasingType.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
            EasingType.BEZIER -> solveCubicBezier(t, cp1x, cp1y, cp2x, cp2y)
            EasingType.SPRING -> {
                val decay = Math.exp(-6.0 * t).toFloat()
                (1.0f - decay * kotlin.math.cos(12.0 * t)).toFloat()
            }
            EasingType.BOUNCE -> solveBounce(t)
            EasingType.STEP -> if (t >= 1f) 1f else 0f
        }
    }

    private fun solveBounce(t: Float): Float {
        var x = t
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> {
                x -= 1.5f / d1
                n1 * x * x + 0.75f
            }
            x < 2.5f / d1 -> {
                x -= 2.25f / d1
                n1 * x * x + 0.9375f
            }
            else -> {
                x -= 2.625f / d1
                n1 * x * x + 0.984375f
            }
        }
    }

    private fun solveCubicBezier(x: Float, p1x: Float, p1y: Float, p2x: Float, p2y: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f

        // Newton-Raphson iteration to find t for given x
        var t = x
        for (i in 0 until 8) {
            val currentX = sampleCurveX(t, p1x, p2x) - x
            if (abs(currentX) < 1e-4) break
            val dX = sampleCurveDerivativeX(t, p1x, p2x)
            if (abs(dX) < 1e-6) break
            t -= currentX / dX
        }
        return sampleCurveY(t.coerceIn(0f, 1f), p1y, p2y)
    }

    private fun sampleCurveX(t: Float, p1x: Float, p2x: Float): Float {
        val inv = 1f - t
        return 3f * inv * inv * t * p1x + 3f * inv * t * t * p2x + t * t * t
    }

    private fun sampleCurveY(t: Float, p1y: Float, p2y: Float): Float {
        val inv = 1f - t
        return 3f * inv * inv * t * p1y + 3f * inv * t * t * p2y + t * t * t
    }

    private fun sampleCurveDerivativeX(t: Float, p1x: Float, p2x: Float): Float {
        val inv = 1f - t
        return 3f * inv * inv * p1x + 6f * inv * t * (p2x - p1x) + 3f * t * t * (1f - p2x)
    }
}

@Serializable
data class Keyframe(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val value: Float,
    val easing: EasingCurve = EasingCurve.Linear
)

@Serializable
data class AnimatableFloat(
    val staticValue: Float = 0f,
    val keyframes: List<Keyframe> = emptyList(),
    val expression: String? = null
) {
    fun evaluate(timeMs: Long, evaluateExpression: ((String, Long, Float) -> Float)? = null): Float {
        if (!expression.isNullOrBlank() && evaluateExpression != null) {
            val baseVal = evaluateBase(timeMs)
            return evaluateExpression(expression, timeMs, baseVal)
        }
        return evaluateBase(timeMs)
    }

    private fun evaluateBase(timeMs: Long): Float {
        if (keyframes.isEmpty()) return staticValue
        if (keyframes.size == 1) return keyframes[0].value

        val sorted = keyframes.sortedBy { it.timeMs }
        if (timeMs <= sorted.first().timeMs) return sorted.first().value
        if (timeMs >= sorted.last().timeMs) return sorted.last().value

        for (i in 0 until sorted.size - 1) {
            val kf1 = sorted[i]
            val kf2 = sorted[i + 1]
            if (timeMs in kf1.timeMs..kf2.timeMs) {
                val duration = (kf2.timeMs - kf1.timeMs).toFloat()
                if (duration <= 0f) return kf2.value
                val rawProgress = (timeMs - kf1.timeMs) / duration
                val easedProgress = kf1.easing.interpolate(rawProgress)
                return kf1.value + (kf2.value - kf1.value) * easedProgress
            }
        }
        return staticValue
    }

    fun hasKeyframeAt(timeMs: Long, toleranceMs: Long = 50L): Boolean {
        return keyframes.any { abs(it.timeMs - timeMs) <= toleranceMs }
    }

    fun getKeyframeAt(timeMs: Long, toleranceMs: Long = 50L): Keyframe? {
        return keyframes.firstOrNull { abs(it.timeMs - timeMs) <= toleranceMs }
    }
}

@Serializable
data class LayerTransform(
    val positionX: AnimatableFloat = AnimatableFloat(0f),
    val positionY: AnimatableFloat = AnimatableFloat(0f),
    val scaleX: AnimatableFloat = AnimatableFloat(1f),
    val scaleY: AnimatableFloat = AnimatableFloat(1f),
    val uniformScale: Boolean = true,
    val rotation: AnimatableFloat = AnimatableFloat(0f),
    val anchorX: AnimatableFloat = AnimatableFloat(0.5f),
    val anchorY: AnimatableFloat = AnimatableFloat(0.5f),
    val opacity: AnimatableFloat = AnimatableFloat(1f)
)
