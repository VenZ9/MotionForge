package com.motionforge.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motionforge.app.model.*
import com.motionforge.app.ui.theme.*

@Composable
fun InspectorPanel(
    layer: Layer?,
    allLayers: List<Layer>,
    currentTimeMs: Long,
    onUpdateLayer: ((Layer) -> Layer) -> Unit,
    onOpenGraphEditor: (EasingCurve) -> Unit,
    modifier: Modifier = Modifier
) {
    if (layer == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Select a layer to view and edit properties", color = TextTertiary, fontSize = 13.sp)
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Transform", "Effects", "Styling", "Blending", "Parenting", "Expressions")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
    ) {
        // Tab row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = AccentCyan,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (selectedTab) {
                0 -> TransformTab(layer, currentTimeMs, onUpdateLayer, onOpenGraphEditor)
                1 -> EffectsTab(layer, onUpdateLayer)
                2 -> StylingTab(layer, onUpdateLayer)
                3 -> BlendingTab(layer, onUpdateLayer)
                4 -> ParentingTab(layer, allLayers, onUpdateLayer)
                5 -> ExpressionsTab(layer, onUpdateLayer)
            }
        }
    }
}

@Composable
private fun TransformTab(
    layer: Layer,
    currentTimeMs: Long,
    onUpdateLayer: ((Layer) -> Layer) -> Unit,
    onOpenGraphEditor: (EasingCurve) -> Unit
) {
    val t = layer.transform

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            // Position X & Y
            PropertySliderRow(
                label = "Position X",
                value = t.positionX.evaluate(currentTimeMs),
                valueRange = -800f..800f,
                hasKeyframe = t.positionX.hasKeyframeAt(currentTimeMs),
                onValueChange = { newVal ->
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(positionX = updateAnimatable(l.transform.positionX, currentTimeMs, newVal)))
                    }
                },
                onToggleKeyframe = {
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(positionX = toggleKeyframe(l.transform.positionX, currentTimeMs)))
                    }
                }
            )
        }

        item {
            PropertySliderRow(
                label = "Position Y",
                value = t.positionY.evaluate(currentTimeMs),
                valueRange = -800f..800f,
                hasKeyframe = t.positionY.hasKeyframeAt(currentTimeMs),
                onValueChange = { newVal ->
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(positionY = updateAnimatable(l.transform.positionY, currentTimeMs, newVal)))
                    }
                },
                onToggleKeyframe = {
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(positionY = toggleKeyframe(l.transform.positionY, currentTimeMs)))
                    }
                }
            )
        }

        item {
            // Scale
            PropertySliderRow(
                label = "Scale",
                value = t.scaleX.evaluate(currentTimeMs),
                valueRange = 0.1f..3f,
                hasKeyframe = t.scaleX.hasKeyframeAt(currentTimeMs),
                onValueChange = { newVal ->
                    onUpdateLayer { l ->
                        l.copy(
                            transform = l.transform.copy(
                                scaleX = updateAnimatable(l.transform.scaleX, currentTimeMs, newVal),
                                scaleY = updateAnimatable(l.transform.scaleY, currentTimeMs, newVal)
                            )
                        )
                    }
                },
                onToggleKeyframe = {
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(scaleX = toggleKeyframe(l.transform.scaleX, currentTimeMs)))
                    }
                }
            )
        }

        item {
            // Rotation
            PropertySliderRow(
                label = "Rotation",
                value = t.rotation.evaluate(currentTimeMs),
                valueRange = -360f..360f,
                hasKeyframe = t.rotation.hasKeyframeAt(currentTimeMs),
                onValueChange = { newVal ->
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(rotation = updateAnimatable(l.transform.rotation, currentTimeMs, newVal)))
                    }
                },
                onToggleKeyframe = {
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(rotation = toggleKeyframe(l.transform.rotation, currentTimeMs)))
                    }
                }
            )
        }

        item {
            // Opacity
            PropertySliderRow(
                label = "Opacity",
                value = t.opacity.evaluate(currentTimeMs),
                valueRange = 0f..1f,
                hasKeyframe = t.opacity.hasKeyframeAt(currentTimeMs),
                onValueChange = { newVal ->
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(opacity = updateAnimatable(l.transform.opacity, currentTimeMs, newVal)))
                    }
                },
                onToggleKeyframe = {
                    onUpdateLayer { l ->
                        l.copy(transform = l.transform.copy(opacity = toggleKeyframe(l.transform.opacity, currentTimeMs)))
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Bézier Graph Editor Launcher
            Button(
                onClick = { onOpenGraphEditor(EasingCurve.EaseInOut) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Bézier Graph Editor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PropertySliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    hasKeyframe: Boolean,
    onValueChange: (Float) -> Unit,
    onToggleKeyframe: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(90.dp)) {
            Text(label, fontSize = 12.sp, color = TextSecondary)
            Text(String.format("%.1f", value), fontSize = 11.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
        )
        IconButton(onClick = onToggleKeyframe) {
            Icon(
                Icons.Default.Diamond,
                contentDescription = "Keyframe",
                tint = if (hasKeyframe) AccentRose else TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EffectsTab(
    layer: Layer,
    onUpdateLayer: ((Layer) -> Layer) -> Unit
) {
    var showAddEffectMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Applied Effects (${layer.effects.size})", fontWeight = FontWeight.Bold, color = TextPrimary)
            Box {
                OutlinedButton(onClick = { showAddEffectMenu = true }, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Effect", fontSize = 12.sp)
                }
                DropdownMenu(expanded = showAddEffectMenu, onDismissRequest = { showAddEffectMenu = false }) {
                    DropdownMenuItem(text = { Text("Chroma Key (Green Screen)") }, onClick = {
                        showAddEffectMenu = false
                        onUpdateLayer { it.copy(effects = it.effects + LayerEffect.ChromaKey()) }
                    })
                    DropdownMenuItem(text = { Text("Color Grading") }, onClick = {
                        showAddEffectMenu = false
                        onUpdateLayer { it.copy(effects = it.effects + LayerEffect.ColorGrading()) }
                    })
                    DropdownMenuItem(text = { Text("Glow") }, onClick = {
                        showAddEffectMenu = false
                        onUpdateLayer { it.copy(effects = it.effects + LayerEffect.Glow()) }
                    })
                    DropdownMenuItem(text = { Text("Camera Shake") }, onClick = {
                        showAddEffectMenu = false
                        onUpdateLayer { it.copy(effects = it.effects + LayerEffect.Shake()) }
                    })
                    DropdownMenuItem(text = { Text("RGB Split (Chromatic)") }, onClick = {
                        showAddEffectMenu = false
                        onUpdateLayer { it.copy(effects = it.effects + LayerEffect.RgbSplit()) }
                    })
                    DropdownMenuItem(text = { Text("Particles Generator") }, onClick = {
                        showAddEffectMenu = false
                        onUpdateLayer { it.copy(effects = it.effects + LayerEffect.Particles()) }
                    })
                    DropdownMenuItem(text = { Text("Motion Blur") }, onClick = {
                        showAddEffectMenu = false
                        onUpdateLayer { it.copy(effects = it.effects + LayerEffect.MotionBlur()) }
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(layer.effects, key = { it.id }) { effect ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(effect.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = effect.isEnabled,
                                    onCheckedChange = { checked ->
                                        onUpdateLayer { l ->
                                            l.copy(effects = l.effects.map { if (it.id == effect.id) toggleEffectEnabled(it, checked) else it })
                                        }
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                                IconButton(
                                    onClick = {
                                        onUpdateLayer { l -> l.copy(effects = l.effects.filterNot { it.id == effect.id }) }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRose, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Specific Effect Sliders
                        when (effect) {
                            is LayerEffect.ChromaKey -> {
                                Text("Tolerance: ${(effect.tolerance * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary)
                                Slider(
                                    value = effect.tolerance,
                                    onValueChange = { newVal ->
                                        onUpdateLayer { l ->
                                            l.copy(effects = l.effects.map { if (it.id == effect.id) effect.copy(tolerance = newVal) else it })
                                        }
                                    },
                                    valueRange = 0.05f..0.8f
                                )
                            }
                            is LayerEffect.ColorGrading -> {
                                Text("Saturation: ${(effect.saturation * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary)
                                Slider(
                                    value = effect.saturation,
                                    onValueChange = { newVal ->
                                        onUpdateLayer { l ->
                                            l.copy(effects = l.effects.map { if (it.id == effect.id) effect.copy(saturation = newVal) else it })
                                        }
                                    },
                                    valueRange = 0f..2f
                                )
                            }
                            is LayerEffect.Glow -> {
                                Text("Intensity: ${(effect.intensity * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary)
                                Slider(
                                    value = effect.intensity,
                                    onValueChange = { newVal ->
                                        onUpdateLayer { l ->
                                            l.copy(effects = l.effects.map { if (it.id == effect.id) effect.copy(intensity = newVal) else it })
                                        }
                                    },
                                    valueRange = 0.2f..3f
                                )
                            }
                            is LayerEffect.Shake -> {
                                Text("Amplitude: ${effect.amplitudeX.toInt()}px", fontSize = 11.sp, color = TextSecondary)
                                Slider(
                                    value = effect.amplitudeX,
                                    onValueChange = { newVal ->
                                        onUpdateLayer { l ->
                                            l.copy(effects = l.effects.map { if (it.id == effect.id) effect.copy(amplitudeX = newVal, amplitudeY = newVal) else it })
                                        }
                                    },
                                    valueRange = 2f..50f
                                )
                            }
                            is LayerEffect.RgbSplit -> {
                                Text("Split Amount: ${effect.amount.toInt()}px", fontSize = 11.sp, color = TextSecondary)
                                Slider(
                                    value = effect.amount,
                                    onValueChange = { newVal ->
                                        onUpdateLayer { l ->
                                            l.copy(effects = l.effects.map { if (it.id == effect.id) effect.copy(amount = newVal) else it })
                                        }
                                    },
                                    valueRange = 0f..40f
                                )
                            }
                            is LayerEffect.Particles -> {
                                Text("Particle Count: ${effect.count}", fontSize = 11.sp, color = TextSecondary)
                                Slider(
                                    value = effect.count.toFloat(),
                                    onValueChange = { newVal ->
                                        onUpdateLayer { l ->
                                            l.copy(effects = l.effects.map { if (it.id == effect.id) effect.copy(count = newVal.toInt()) else it })
                                        }
                                    },
                                    valueRange = 10f..100f
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StylingTab(
    layer: Layer,
    onUpdateLayer: ((Layer) -> Layer) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (layer.type == LayerType.TEXT && layer.textData != null) {
            val td = layer.textData
            item {
                OutlinedTextField(
                    value = td.text,
                    onValueChange = { newText ->
                        onUpdateLayer { it.copy(textData = td.copy(text = newText)) }
                    },
                    label = { Text("Text Content") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("Font Size: ${td.fontSize.toInt()}sp", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = td.fontSize,
                    onValueChange = { newSize ->
                        onUpdateLayer { it.copy(textData = td.copy(fontSize = newSize)) }
                    },
                    valueRange = 16f..120f
                )
            }
            item {
                Text("Text Animation", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextAnimationType.values().forEach { anim ->
                        FilterChip(
                            selected = td.animationType == anim,
                            onClick = { onUpdateLayer { it.copy(textData = td.copy(animationType = anim)) } },
                            label = { Text(anim.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        } else if (layer.type == LayerType.SHAPE && layer.shapeData != null) {
            val sd = layer.shapeData
            item {
                Text("Shape Type", fontSize = 12.sp, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShapeType.values().forEach { shapeType ->
                        FilterChip(
                            selected = sd.shapeType == shapeType,
                            onClick = { onUpdateLayer { it.copy(shapeData = sd.copy(shapeType = shapeType)) } },
                            label = { Text(shapeType.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
            item {
                Text("Corner Radius: ${sd.cornerRadius.toInt()}px", fontSize = 12.sp, color = TextSecondary)
                Slider(
                    value = sd.cornerRadius,
                    onValueChange = { newRad ->
                        onUpdateLayer { it.copy(shapeData = sd.copy(cornerRadius = newRad)) }
                    },
                    valueRange = 0f..60f
                )
            }
        } else {
            item {
                Text("No additional styling properties for this layer type.", color = TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BlendingTab(
    layer: Layer,
    onUpdateLayer: ((Layer) -> Layer) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Blending Mode", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(AppBlendMode.NORMAL, AppBlendMode.MULTIPLY, AppBlendMode.SCREEN, AppBlendMode.OVERLAY, AppBlendMode.ADD).forEach { mode ->
                    FilterChip(
                        selected = layer.blendMode == mode,
                        onClick = { onUpdateLayer { it.copy(blendMode = mode) } },
                        label = { Text(mode.label, fontSize = 11.sp) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Masking", fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MaskType.values().forEach { maskType ->
                    FilterChip(
                        selected = layer.mask.maskType == maskType,
                        onClick = { onUpdateLayer { it.copy(mask = it.mask.copy(maskType = maskType)) } },
                        label = { Text(maskType.label, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentingTab(
    layer: Layer,
    allLayers: List<Layer>,
    onUpdateLayer: ((Layer) -> Layer) -> Unit
) {
    val potentialParents = allLayers.filterNot { it.id == layer.id }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Parent Layer (Hierarchical Rigging)", fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(
            "Child transforms move, scale, and rotate seamlessly with the parent layer.",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            onClick = { onUpdateLayer { it.copy(parentLayerId = null) } },
            shape = RoundedCornerShape(8.dp),
            color = if (layer.parentLayerId == null) AccentCyan.copy(alpha = 0.2f) else SurfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("None (Root)", modifier = Modifier.padding(12.dp), color = if (layer.parentLayerId == null) AccentCyan else TextPrimary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        potentialParents.forEach { parentCandidate ->
            val isCurrentParent = layer.parentLayerId == parentCandidate.id
            Surface(
                onClick = { onUpdateLayer { it.copy(parentLayerId = parentCandidate.id) } },
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrentParent) AccentViolet.copy(alpha = 0.2f) else SurfaceElevated,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    parentCandidate.name,
                    modifier = Modifier.padding(12.dp),
                    color = if (isCurrentParent) AccentViolet else TextPrimary
                )
            }
        }
    }
}

@Composable
private fun ExpressionsTab(
    layer: Layer,
    onUpdateLayer: ((Layer) -> Layer) -> Unit
) {
    var exprText by remember { mutableStateOf(layer.transform.rotation.expression ?: "wiggle(5, 20)") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Motion Expression System", fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Evaluate dynamic procedural math formulas in real time.", fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = exprText,
            onValueChange = { exprText = it },
            label = { Text("Expression Formula") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Quick Presets:", fontSize = 11.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("wiggle(5, 20)", "time * 90", "bounce", "sin(time * 3) * 50").forEach { preset ->
                OutlinedButton(
                    onClick = { exprText = preset },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(preset, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                onUpdateLayer { l ->
                    l.copy(
                        transform = l.transform.copy(
                            rotation = l.transform.rotation.copy(expression = exprText)
                        )
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Expression to Rotation", fontWeight = FontWeight.Bold)
        }
    }
}

private fun updateAnimatable(prop: AnimatableFloat, timeMs: Long, newVal: Float): AnimatableFloat {
    if (prop.keyframes.isEmpty()) {
        return prop.copy(staticValue = newVal)
    }
    val existing = prop.getKeyframeAt(timeMs)
    return if (existing != null) {
        prop.copy(keyframes = prop.keyframes.map { if (it.id == existing.id) it.copy(value = newVal) else it })
    } else {
        prop.copy(staticValue = newVal)
    }
}

private fun toggleKeyframe(prop: AnimatableFloat, timeMs: Long): AnimatableFloat {
    val existing = prop.getKeyframeAt(timeMs)
    return if (existing != null) {
        prop.copy(keyframes = prop.keyframes.filterNot { it.id == existing.id })
    } else {
        val currentVal = prop.evaluate(timeMs)
        prop.copy(keyframes = prop.keyframes + Keyframe(timeMs = timeMs, value = currentVal))
    }
}

private fun toggleEffectEnabled(effect: LayerEffect, enabled: Boolean): LayerEffect {
    return when (effect) {
        is LayerEffect.ChromaKey -> effect.copy(isEnabled = enabled)
        is LayerEffect.ColorGrading -> effect.copy(isEnabled = enabled)
        is LayerEffect.Glow -> effect.copy(isEnabled = enabled)
        is LayerEffect.Blur -> effect.copy(isEnabled = enabled)
        is LayerEffect.Shake -> effect.copy(isEnabled = enabled)
        is LayerEffect.RgbSplit -> effect.copy(isEnabled = enabled)
        is LayerEffect.Particles -> effect.copy(isEnabled = enabled)
        is LayerEffect.MotionBlur -> effect.copy(isEnabled = enabled)
        is LayerEffect.Transition -> effect.copy(isEnabled = enabled)
    }
}
