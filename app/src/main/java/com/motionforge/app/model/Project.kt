package com.motionforge.app.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Project",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val durationMs: Long = 5000L,
    val backgroundColorHex: String = "#0A0C10",
    val layers: List<Layer> = emptyList(),
    val compositions: List<Composition> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val aspectRatioFloat: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 16f / 9f

    val totalFrames: Int
        get() = ((durationMs / 1000f) * fps).toInt()
}

@Serializable
data class Composition(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Comp 1",
    val width: Int = 1920,
    val height: Int = 1080,
    val durationMs: Long = 5000L,
    val layers: List<Layer> = emptyList()
)

data class ProjectPreset(
    val title: String,
    val width: Int,
    val height: Int,
    val label: String
) {
    companion object {
        val PRESETS = listOf(
            ProjectPreset("16:9 Landscape", 1920, 1080, "YouTube / Video (1080p)"),
            ProjectPreset("9:16 Portrait", 1080, 1920, "Shorts / TikTok / Reels"),
            ProjectPreset("1:1 Square", 1080, 1080, "Square Post (1080p)"),
            ProjectPreset("4:5 Portrait", 1080, 1350, "Instagram Feed"),
            ProjectPreset("21:9 Cinema", 2560, 1080, "Ultrawide Cinematic"),
            ProjectPreset("4K UHD", 3840, 2160, "Ultra HD 4K (2160p)")
        )
    }
}
