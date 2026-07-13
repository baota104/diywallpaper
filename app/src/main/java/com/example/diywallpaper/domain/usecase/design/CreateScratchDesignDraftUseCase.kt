package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import java.util.UUID
import javax.inject.Inject

class CreateScratchDesignDraftUseCase @Inject constructor(
    private val createDesignDraftUseCase: CreateDesignDraftUseCase
) {
    suspend operator fun invoke(
        request: ScratchDesignDraftRequest = ScratchDesignDraftRequest(),
        title: String? = null
    ): AppResult<String> {
        val now = System.currentTimeMillis()
        val project = EditorProject(
            id = UUID.randomUUID().toString(),
            source = EditorProjectSource.Scratch,
            canvas = request.canvas,
            background = request.background.toEditorBackground(),
            layers = emptyList(),
            placeholders = emptyList(),
            selectedLayerId = null,
            createdAt = now,
            updatedAt = now,
            schemaVersion = 1
        )

        return createDesignDraftUseCase(
            project = project,
            title = title ?: DEFAULT_TITLE
        )
    }

    companion object {
        private const val DEFAULT_TITLE = "Create from Scratch"
    }
}

data class ScratchDesignDraftRequest(
    val canvas: EditorCanvasSpec = DEFAULT_CANVAS,
    val background: ScratchDesignBackground = ScratchDesignBackground.SolidColor("#FFFFFF")
) {
    companion object {
        val DEFAULT_CANVAS = EditorCanvasSpec(width = 1080, height = 1920)
    }
}

sealed interface ScratchDesignBackground {
    data class SolidColor(val colorHex: String) : ScratchDesignBackground
    data class Gradient(val colors: List<String>) : ScratchDesignBackground
    data class ApiBackground(val item: BackgroundCreateItem) : ScratchDesignBackground
}

internal fun ScratchDesignBackground.toEditorBackground(): EditorBackground = when (this) {
    is ScratchDesignBackground.SolidColor -> EditorBackground.SolidColor(colorHex)
    is ScratchDesignBackground.Gradient -> EditorBackground.Gradient(colors)
    is ScratchDesignBackground.ApiBackground -> EditorBackground.ApiImage(
        backgroundId = item.id,
        imageUrl = item.imageUrl
    )
}
