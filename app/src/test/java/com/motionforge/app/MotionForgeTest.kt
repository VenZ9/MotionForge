package com.motionforge.app

import com.motionforge.app.data.HistoryManager
import com.motionforge.app.engine.ExpressionEngine
import com.motionforge.app.engine.TransformationEngine
import com.motionforge.app.model.*
import org.junit.Assert.*
import org.junit.Test

class MotionForgeTest {

    @Test
    fun testEasingCurveInterpolation() {
        val linear = EasingCurve.Linear
        assertEquals(0f, linear.interpolate(0f), 0.001f)
        assertEquals(0.5f, linear.interpolate(0.5f), 0.001f)
        assertEquals(1f, linear.interpolate(1f), 0.001f)

        val bezier = EasingCurve.bezier(0.25f, 0.1f, 0.25f, 1f)
        assertEquals(0f, bezier.interpolate(0f), 0.001f)
        assertEquals(1f, bezier.interpolate(1f), 0.001f)
        assertTrue(bezier.interpolate(0.5f) in 0f..1f)
    }

    @Test
    fun testExpressionEngine() {
        val baseVal = 100f
        val timeMs = 2000L // 2 seconds

        val timeResult = ExpressionEngine.evaluate("time * 30", timeMs, baseVal)
        assertEquals(60f, timeResult, 0.01f)

        val wiggleResult = ExpressionEngine.evaluate("wiggle(5, 20)", timeMs, baseVal)
        assertTrue(wiggleResult in (baseVal - 25f)..(baseVal + 25f))

        val bounceResult = ExpressionEngine.evaluate("bounce", timeMs, baseVal)
        assertTrue(bounceResult >= baseVal * 0.9f)
    }

    @Test
    fun testHistoryManagerUndoRedo() {
        val history = HistoryManager()
        val proj1 = Project(id = "p1", name = "First")
        val proj2 = Project(id = "p1", name = "Second")
        val proj3 = Project(id = "p1", name = "Third")

        history.pushState(proj1)
        history.pushState(proj2)

        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        val undone = history.undo(proj3)
        assertEquals(proj2, undone)
        assertTrue(history.canRedo)

        val redone = history.redo(proj2)
        assertEquals(proj3, redone)
    }

    @Test
    fun testParentingHierarchicalTransform() {
        val parentLayer = Layer(
            id = "parent",
            name = "Parent",
            transform = LayerTransform(
                positionX = AnimatableFloat(100f),
                positionY = AnimatableFloat(50f),
                scaleX = AnimatableFloat(2f),
                scaleY = AnimatableFloat(2f)
            )
        )

        val childLayer = Layer(
            id = "child",
            name = "Child",
            parentLayerId = "parent",
            transform = LayerTransform(
                positionX = AnimatableFloat(20f),
                positionY = AnimatableFloat(10f),
                scaleX = AnimatableFloat(1f),
                scaleY = AnimatableFloat(1f)
            )
        )

        val evaluated = TransformationEngine.evaluateLayer(
            layer = childLayer,
            allLayers = listOf(parentLayer, childLayer),
            timeMs = 0L,
            compositionWidth = 1920,
            compositionHeight = 1080
        )

        // Child local position (20, 10) scaled by 2 => (40, 20), plus parent (100, 50) => (140, 70)
        assertEquals(140f, evaluated.x, 0.01f)
        assertEquals(70f, evaluated.y, 0.01f)
        assertEquals(2f, evaluated.scaleX, 0.01f)
    }
}
