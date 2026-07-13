package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.remote.dto.resolveDiyBackgroundValue
import com.example.diywallpaper.domain.model.DiyBackgroundValue
import com.example.diywallpaper.domain.model.DiyElementType
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import com.example.diywallpaper.domain.model.design.DiyTemplateSnapshot
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoPlaceholderLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import java.util.UUID
import javax.inject.Inject

class CreateDiyDesignDraftUseCase @Inject constructor(
    private val createDesignDraftUseCase: CreateDesignDraftUseCase
) {
    suspend operator fun invoke(
        template: DiyTemplate,
        templateData: DiyTemplateData,
        title: String? = null
    ): AppResult<String> {
        val project = template.toEditorProject(templateData)
        return createDesignDraftUseCase(
            project = project,
            title = title ?: "DIY ${template.id}"
        )
    }
}

internal fun DiyTemplate.toEditorProject(
    templateData: DiyTemplateData,
    projectId: String = UUID.randomUUID().toString(),
    createdAt: Long = System.currentTimeMillis()
): EditorProject {
    val backgroundValue = resolveDiyBackgroundValue(
        diyDataUrl = diyDataUrl,
        background = templateData.background
    )

    return EditorProject(
        id = projectId,
        source = EditorProjectSource.Diy(
            templateId = id,
            templateSnapshot = DiyTemplateSnapshot(
                width = templateData.width,
                height = templateData.height,
                background = backgroundValue.toTemplateBackgroundSnapshot(),
                elements = templateData.elements.map { element ->
                    DiyTemplateElementSnapshot(
                        id = buildElementSnapshotId(element),
                        type = element.type.name,
                        x = element.x,
                        y = element.y,
                        width = element.width,
                        height = element.height,
                        rotation = element.rotation,
                        zIndex = element.zIndex,
                        assetUrl = element.assetUrl
                    )
                }
            )
        ),
        canvas = EditorCanvasSpec(
            width = templateData.width,
            height = templateData.height
        ),
        background = backgroundValue.toEditorBackground(),
        layers = templateData.elements
            .filter { it.type == DiyElementType.PICTURE && !it.assetUrl.isNullOrBlank() }
            .sortedBy { it.zIndex }
            .map { element ->
                StickerLayer(
                    id = buildElementSnapshotId(element),
                    stickerId = null,
                    assetPathOrUrl = element.assetUrl.orEmpty(),
                    zIndex = element.zIndex,
                    transform = LayerTransform(
                        offsetX = element.x,
                        offsetY = element.y,
                        scale = 1f,
                        rotation = element.rotation
                    ),
                    isLocked = true,
                    isHidden = false
                )
            },
        placeholders = templateData.placeholders
            .sortedBy { it.zIndex }
            .map { placeholder ->
                PhotoPlaceholderLayer(
                    id = placeholder.id,
                    x = placeholder.x,
                    y = placeholder.y,
                    width = placeholder.width,
                    height = placeholder.height,
                    rotation = placeholder.rotation,
                    zIndex = placeholder.zIndex
                )
            },
        selectedLayerId = null,
        createdAt = createdAt,
        updatedAt = createdAt,
        schemaVersion = 1
    )
}

private fun DiyBackgroundValue.toEditorBackground(): EditorBackground = when (this) {
    is DiyBackgroundValue.ColorHex -> EditorBackground.SolidColor(colorHex = value)
    is DiyBackgroundValue.RemoteUrl -> EditorBackground.ApiImage(
        backgroundId = url,
        imageUrl = url
    )
    is DiyBackgroundValue.AssetUrl -> EditorBackground.ApiImage(
        backgroundId = url,
        imageUrl = url
    )
    DiyBackgroundValue.Empty -> EditorBackground.SolidColor("#FFFFFF")
}

private fun DiyBackgroundValue.toTemplateBackgroundSnapshot() = when (this) {
    is DiyBackgroundValue.ColorHex ->
        com.example.diywallpaper.domain.model.design.TemplateBackgroundSnapshot.ColorHex(value)
    is DiyBackgroundValue.RemoteUrl ->
        com.example.diywallpaper.domain.model.design.TemplateBackgroundSnapshot.AssetUrl(url)
    is DiyBackgroundValue.AssetUrl ->
        com.example.diywallpaper.domain.model.design.TemplateBackgroundSnapshot.AssetUrl(url)
    DiyBackgroundValue.Empty -> null
}

private fun buildElementSnapshotId(element: com.example.diywallpaper.domain.model.DiyElement): String {
    val suffix = element.srcName.ifBlank { "element" }
        .replace("/", "_")
        .replace("\\", "_")
    return "element_${element.zIndex}_$suffix"
}
