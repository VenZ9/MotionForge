package com.motionforge.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppBlendMode(val label: String) {
    NORMAL("Normal"),
    MULTIPLY("Multiply"),
    SCREEN("Screen"),
    OVERLAY("Overlay"),
    DARKEN("Darken"),
    LIGHTEN("Lighten"),
    COLOR_DODGE("Color Dodge"),
    COLOR_BURN("Color Burn"),
    HARD_LIGHT("Hard Light"),
    SOFT_LIGHT("Soft Light"),
    DIFFERENCE("Difference"),
    EXCLUSION("Exclusion"),
    ADD("Add / Linear Dodge")
}

@Serializable
enum class MaskType(val label: String) {
    NONE("None"),
    RECTANGLE("Rectangle"),
    CIRCLE("Circle / Ellipse"),
    ALPHA_MATTE("Alpha Matte (Layer Above)"),
    LUMA_MATTE("Luma Matte (Layer Above)")
}

@Serializable
data class MaskConfig(
    val maskType: MaskType = MaskType.NONE,
    val isInverted: Boolean = false,
    val feather: Float = 0f,
    val sourceLayerId: String? = null
)

@Serializable
enum class ShapeType(val label: String) {
    RECTANGLE("Rectangle"),
    ROUNDED_RECT("Rounded Rectangle"),
    CIRCLE("Circle / Ellipse"),
    STAR("Star"),
    POLYGON("Polygon / Hexagon"),
    TRIANGLE("Triangle")
}

@Serializable
enum class TextAnimationType(val label: String) {
    NONE("None"),
    TYPEWRITER("Typewriter"),
    WAVE("Wave Amplitude"),
    SCALE_BOUNCE("Scale Bounce"),
    GLITCH("Cyber Glitch")
}

@Serializable
data class ShapeLayerData(
    val shapeType: ShapeType = ShapeType.ROUNDED_RECT,
    val fillColorHex: String = "#7C4DFF",
    val strokeColorHex: String = "#00E5FF",
    val strokeWidth: Float = 0f,
    val cornerRadius: Float = 24f,
    val points: Int = 5,
    val innerRadiusRatio: Float = 0.5f,
    val width: Float = 300f,
    val height: Float = 300f
)

@Serializable
data class TextLayerData(
    val text: String = "MotionForge",
    val fontSize: Float = 64f,
    val colorHex: String = "#FFFFFF",
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val letterSpacing: Float = 2f,
    val alignment: String = "CENTER",
    val animationType: TextAnimationType = TextAnimationType.NONE
)

@Serializable
data class MediaLayerData(
    val uri: String = "",
    val durationMs: Long = 5000L,
    val speed: Float = 1.0f,
    val loop: Boolean = false
)

@Serializable
data class AudioLayerData(
    val uri: String = "",
    val durationMs: Long = 5000L,
    val volume: Float = 1.0f,
    val pan: Float = 0.0f,
    val speed: Float = 1.0f
)
