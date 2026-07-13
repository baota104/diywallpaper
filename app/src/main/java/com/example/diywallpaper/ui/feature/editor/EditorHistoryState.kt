package com.example.diywallpaper.ui.feature.editor

import com.example.diywallpaper.domain.model.design.EditorProject

data class EditorHistoryState(
    val undoStack: List<EditorProject> = emptyList(),
    val current: EditorProject? = null,
    val redoStack: List<EditorProject> = emptyList()
) {
    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}
