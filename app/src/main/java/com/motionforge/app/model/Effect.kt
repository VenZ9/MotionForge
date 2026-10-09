package com.motionforge.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class EffectType {
    CHROMA_KEY,
    COLOR_GRADING,
    GLOW,
    BLUR,
    SHAKE,
    RGB_SPLIT,
    PARTICLES,
    MOTION_BLUR,
    TRANSITION
}

@Serializable
enum class BlurType {
    GAUSSIAN,
    DIRECTIONAL,
    RADIAL,
    BOX
}

@Serializable
enum class ParticleType {
    STARS,
    SPARKS,
    SNOW,
    DUST,
    CONFETTI,
    FIRE
}

@Serializable
enum class TransitionType {
    CROSS_DISSOLVE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    WIPE,
    ZOOM,
    GLITCH,
    DIP_TO_BLACK,
    DIP_TO_WHITE
}

@Serializable
sealed class LayerEffect {
    abstract val id: String
    abstract val name: String
    abstract val isEnabled: Boolean
    abstract val type: EffectType

    @Serializable
    data class ChromaKey(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Chroma Key",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.CHROMA_KEY,
        val keyColorHex: String = "#00FF00",
        val tolerance: Float = 0.35f,
        val smoothness: Float = 0.15f,
        val spillReduction: Float = 0.4f
    ) : LayerEffect()

    @Serializable
    data class ColorGrading(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Color Grading",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.COLOR_GRADING,
        val exposure: Float = 0f,       // -1.0 to 1.0
        val contrast: Float = 1.0f,     // 0.0 to 2.0
        val saturation: Float = 1.0f,   // 0.0 to 2.0
        val brightness: Float = 0f,     // -1.0 to 1.0
        val temperature: Float = 0f,    // -1.0 (cool) to 1.0 (warm)
        val tint: Float = 0f,           // -1.0 (magenta) to 1.0 (green)
        val vignette: Float = 0f        // 0.0 to 1.0
    ) : LayerEffect()

    @Serializable
    data class Glow(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Glow",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.GLOW,
        val radius: Float = 25f,
        val intensity: Float = 1.2f,
        val threshold: Float = 0.5f,
        val glowColorHex: String = "#00E5FF"
    ) : LayerEffect()

    @Serializable
    data class Blur(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Blur",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.BLUR,
        val radius: Float = 15f,
        val blurType: BlurType = BlurType.GAUSSIAN,
        val angle: Float = 0f
    ) : LayerEffect()

    @Serializable
    data class Shake(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Camera Shake",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.SHAKE,
        val amplitudeX: Float = 18f,
        val amplitudeY: Float = 18f,
        val frequency: Float = 12f,
        val speed: Float = 1f,
        val seed: Int = 42
    ) : LayerEffect()

    @Serializable
    data class RgbSplit(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "RGB Split (Chromatic)",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.RGB_SPLIT,
        val amount: Float = 12f,
        val angle: Float = 0f
    ) : LayerEffect()

    @Serializable
    data class Particles(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Particles Generator",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.PARTICLES,
        val particleType: ParticleType = ParticleType.SPARKS,
        val count: Int = 40,
        val speed: Float = 1.5f,
        val size: Float = 6f
    ) : LayerEffect()

    @Serializable
    data class MotionBlur(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Motion Blur",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.MOTION_BLUR,
        val samples: Int = 6,
        val shutterAngle: Float = 180f
    ) : LayerEffect()

    @Serializable
    data class Transition(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Transition",
        override val isEnabled: Boolean = true,
        override val type: EffectType = EffectType.TRANSITION,
        val transitionType: TransitionType = TransitionType.CROSS_DISSOLVE,
        val durationMs: Long = 600L,
        val isAtStart: Boolean = true
    ) : LayerEffect()
}
