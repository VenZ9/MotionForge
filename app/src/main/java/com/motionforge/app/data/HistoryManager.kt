package com.motionforge.app.data

import com.motionforge.app.model.Project

class HistoryManager(private val maxHistorySize: Int = 30) {
    private val undoStack = mutableListOf<Project>()
    private val redoStack = mutableListOf<Project>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    fun pushState(currentState: Project) {
        if (undoStack.isNotEmpty() && undoStack.last() == currentState) {
            return
        }
        undoStack.add(currentState)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo(currentState: Project): Project? {
        if (undoStack.isEmpty()) return null
        redoStack.add(currentState)
        return undoStack.removeAt(undoStack.lastIndex)
    }

    fun redo(currentState: Project): Project? {
        if (redoStack.isEmpty()) return null
        undoStack.add(currentState)
        return redoStack.removeAt(redoStack.lastIndex)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
