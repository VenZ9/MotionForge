package com.motionforge.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class LayerType(val label: String) {
    VIDEO("Video"),
    IMAGE("Image"),
    AUDIO("Audio"),
    TEXT("Text"),
    SHAPE("Vector Shape"),
    ADJUSTMENT("Adjustment Layer"),
    NESTED_COMP("Nested Comp")
}

@Serializable
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer",
    val type: LayerType = LayerType.TEXT,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 3000L,
    val inPointMs: Long = 0L,
    val outPointMs: Long = 3000L,
    val trackIndex: Int = 0,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isMuted: Boolean = false,
    val blendMode: AppBlendMode = AppBlendMode.NORMAL,
    val parentLayerId: String? = null,
    val transform: LayerTransform = LayerTransform(),
    val effects: List<LayerEffect> = emptyList(),
    val mask: MaskConfig = MaskConfig(),
    val shapeData: ShapeLayerData? = null,
    val textData: TextLayerData? = null,
    val mediaData: MediaLayerData? = null,
    val audioData: AudioLayerData? = null,
    val nestedCompId: String? = null
) {
    val endTimeMs: Long
        get() = startTimeMs + durationMs

    fun isActiveAt(timeMs: Long): Boolean {
        return isVisible && timeMs in startTimeMs until endTimeMs
    }

    fun getLocalTimeMs(globalTimeMs: Long): Long {
        return (globalTimeMs - startTimeMs).coerceIn(0L, durationMs)
    }

    fun hasKeyframes(): Boolean {
        return transform.positionX.keyframes.isNotEmpty() ||
                transform.positionY.keyframes.isNotEmpty() ||
                transform.scaleX.keyframes.isNotEmpty() ||
                transform.scaleY.keyframes.isNotEmpty() ||
                transform.rotation.keyframes.isNotEmpty() ||
                transform.opacity.keyframes.isNotEmpty()
    }

    fun allKeyframeTimes(): Set<Long> {
        val times = mutableSetOf<Long>()
        times.addAll(transform.positionX.keyframes.map { it.timeMs })
        times.addAll(transform.positionY.keyframes.map { it.timeMs })
        times.addAll(transform.scaleX.keyframes.map { it.timeMs })
        times.addAll(transform.scaleY.keyframes.map { it.timeMs })
        times.addAll(transform.rotation.keyframes.map { it.timeMs })
        times.addAll(transform.opacity.keyframes.map { it.timeMs })
        return times
    }
}
