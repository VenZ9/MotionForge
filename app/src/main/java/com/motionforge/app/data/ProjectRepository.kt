package com.motionforge.app.data

import android.content.Context
import com.motionforge.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    suspend fun getAllProjects(): List<Project> = withContext(Dispatchers.IO) {
        val files = projectsDir.listFiles { file -> file.extension == "json" } ?: return@withContext emptyList()
        val projects = files.mapNotNull { file ->
            try {
                json.decodeFromString<Project>(file.readText())
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }

        if (projects.isEmpty()) {
            val defaultList = createDemoProjects()
            defaultList.forEach { saveProject(it) }
            defaultList
        } else {
            projects
        }
    }

    suspend fun getProjectById(id: String): Project? = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$id.json")
        if (!file.exists()) return@withContext null
        try {
            json.decodeFromString<Project>(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveProject(project: Project): Unit = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        val file = File(projectsDir, "${project.id}.json")
        file.writeText(json.encodeToString(updated))
    }

    suspend fun deleteProject(id: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(projectsDir, "$id.json")
        file.delete()
    }

    suspend fun duplicateProject(project: Project): Project = withContext(Dispatchers.IO) {
        val dup = project.copy(
            id = UUID.randomUUID().toString(),
            name = "${project.name} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveProject(dup)
        dup
    }

    private fun createDemoProjects(): List<Project> {
        val demo1 = Project(
            id = "demo-cyberpunk",
            name = "Cyberpunk Neon Intro",
            width = 1920,
            height = 1080,
            fps = 60,
            durationMs = 5000L,
            backgroundColorHex = "#07090E",
            layers = listOf(
                // Layer 1: Ambient Star Particles
                Layer(
                    id = "layer-bg-particles",
                    name = "Starfield Particles",
                    type = LayerType.SHAPE,
                    trackIndex = 0,
                    startTimeMs = 0L,
                    durationMs = 5000L,
                    shapeData = ShapeLayerData(
                        shapeType = ShapeType.RECTANGLE,
                        width = 1920f,
                        height = 1080f,
                        fillColorHex = "#00000000"
                    ),
                    effects = listOf(
                        LayerEffect.Particles(
                            particleType = ParticleType.STARS,
                            count = 50,
                            speed = 1.2f,
                            size = 4f
                        )
                    )
                ),
                // Layer 2: Glowing Hexagon Badge
                Layer(
                    id = "layer-hex-badge",
                    name = "Neon Hexagon",
                    type = LayerType.SHAPE,
                    trackIndex = 1,
                    startTimeMs = 0L,
                    durationMs = 5000L,
                    shapeData = ShapeLayerData(
                        shapeType = ShapeType.POLYGON,
                        points = 6,
                        width = 420f,
                        height = 420f,
                        fillColorHex = "#161B26",
                        strokeColorHex = "#00E5FF",
                        strokeWidth = 6f
                    ),
                    transform = LayerTransform(
                        rotation = AnimatableFloat(
                            staticValue = 0f,
                            keyframes = listOf(
                                Keyframe(timeMs = 0L, value = 0f, easing = EasingCurve.EaseInOut),
                                Keyframe(timeMs = 2500L, value = 180f, easing = EasingCurve.EaseInOut),
                                Keyframe(timeMs = 5000L, value = 360f, easing = EasingCurve.EaseInOut)
                            )
                        ),
                        scaleX = AnimatableFloat(
                            staticValue = 1f,
                            keyframes = listOf(
                                Keyframe(timeMs = 0L, value = 0.5f, easing = EasingCurve.Bounce),
                                Keyframe(timeMs = 1200L, value = 1.15f, easing = EasingCurve.EaseOut),
                                Keyframe(timeMs = 2000L, value = 1f, easing = EasingCurve.Linear)
                            )
                        )
                    ),
                    effects = listOf(
                        LayerEffect.Glow(radius = 35f, intensity = 1.5f, glowColorHex = "#00E5FF")
                    )
                ),
                // Layer 3: Main Animated Title
                Layer(
                    id = "layer-title",
                    name = "MOTIONFORGE Title",
                    type = LayerType.TEXT,
                    trackIndex = 2,
                    startTimeMs = 300L,
                    durationMs = 4700L,
                    textData = TextLayerData(
                        text = "MOTIONFORGE",
                        fontSize = 72f,
                        colorHex = "#FFFFFF",
                        isBold = true,
                        letterSpacing = 6f,
                        animationType = TextAnimationType.TYPEWRITER
                    ),
                    transform = LayerTransform(
                        positionY = AnimatableFloat(
                            staticValue = -20f,
                            keyframes = listOf(
                                Keyframe(timeMs = 300L, value = -60f, easing = EasingCurve.EaseOut),
                                Keyframe(timeMs = 1500L, value = -20f, easing = EasingCurve.EaseInOut)
                            )
                        )
                    ),
                    effects = listOf(
                        LayerEffect.RgbSplit(amount = 14f),
                        LayerEffect.Glow(radius = 20f, intensity = 1.2f, glowColorHex = "#7C4DFF")
                    )
                ),
                // Layer 4: Subtitle Text
                Layer(
                    id = "layer-subtitle",
                    name = "Pro Compositor Subtitle",
                    type = LayerType.TEXT,
                    trackIndex = 3,
                    startTimeMs = 1200L,
                    durationMs = 3800L,
                    textData = TextLayerData(
                        text = "Next-Gen Mobile Motion Graphics",
                        fontSize = 28f,
                        colorHex = "#00E5FF",
                        isBold = false,
                        letterSpacing = 4f
                    ),
                    transform = LayerTransform(
                        positionY = AnimatableFloat(70f),
                        opacity = AnimatableFloat(
                            staticValue = 0f,
                            keyframes = listOf(
                                Keyframe(timeMs = 1200L, value = 0f, easing = EasingCurve.EaseIn),
                                Keyframe(timeMs = 1800L, value = 1f, easing = EasingCurve.EaseOut)
                            )
                        )
                    )
                )
            )
        )

        val demo2 = Project(
            id = "demo-kinetic",
            name = "Kinetic Typography & Easing",
            width = 1080,
            height = 1920,
            fps = 30,
            durationMs = 4000L,
            backgroundColorHex = "#0E1118",
            layers = listOf(
                Layer(
                    id = "layer-star",
                    name = "Golden Star Accent",
                    type = LayerType.SHAPE,
                    trackIndex = 0,
                    startTimeMs = 0L,
                    durationMs = 4000L,
                    shapeData = ShapeLayerData(
                        shapeType = ShapeType.STAR,
                        points = 5,
                        width = 280f,
                        height = 280f,
                        fillColorHex = "#FFD600",
                        strokeColorHex = "#FF9100",
                        strokeWidth = 4f
                    ),
                    transform = LayerTransform(
                        positionY = AnimatableFloat(-180f),
                        rotation = AnimatableFloat(
                            staticValue = 0f,
                            expression = "wiggle(3, 25)"
                        )
                    ),
                    effects = listOf(
                        LayerEffect.Glow(radius = 30f, intensity = 1.2f, glowColorHex = "#FFD600"),
                        LayerEffect.Shake(amplitudeX = 8f, amplitudeY = 8f, frequency = 6f)
                    )
                ),
                Layer(
                    id = "layer-kinetic-text",
                    name = "Kinetic Headline",
                    type = LayerType.TEXT,
                    trackIndex = 1,
                    startTimeMs = 200L,
                    durationMs = 3800L,
                    textData = TextLayerData(
                        text = "CREATE MOTION",
                        fontSize = 62f,
                        colorHex = "#FFFFFF",
                        isBold = true,
                        animationType = TextAnimationType.SCALE_BOUNCE
                    ),
                    transform = LayerTransform(
                        positionY = AnimatableFloat(40f)
                    ),
                    effects = listOf(
                        LayerEffect.ColorGrading(saturation = 1.3f, contrast = 1.2f)
                    )
                )
            )
        )

        return listOf(demo1, demo2)
    }
}
