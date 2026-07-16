package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.remote.dto.resolveDiyBackgroundValue
import com.example.diywallpaper.domain.model.DiyBackgroundValue
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import com.example.diywallpaper.domain.model.design.DiyTemplateSnapshot
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.PhotoPlaceholderLayer
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
            isLive = type == DiyTemplateType.DIY_LIVE,
            diyAnimationUrl = diyAnimationUrl,
            templateSnapshot = DiyTemplateSnapshot(
                width = templateData.width,
                height = templateData.height,
                background = backgroundValue.toTemplateBackgroundSnapshot(),
                elements = templateData.elements.map { element ->
                    val placeholder = templateData.placeholders.firstOrNull { it.zIndex == element.zIndex }
                    DiyTemplateElementSnapshot(
                        id = buildElementSnapshotId(element),
                        type = element.type.name,
                        x = element.x,
                        y = element.y,
                        width = element.width,
                        height = element.height,
                        rotation = element.rotation,
                        zIndex = element.zIndex,
                        assetUrl = element.assetUrl,
                        title = element.title,
                        fontSize = element.fontSize,
                        fontColor = element.fontColor,
                        fontFamilyIndex = element.fontFamilyIndex,
                        maskPathOrUrl = element.maskUrl ?: placeholder?.maskUrl,
                        previewPathOrUrl = placeholder?.previewUrl
                    )
                }
            )
        ),
        canvas = EditorCanvasSpec(
            width = templateData.width,
            height = templateData.height
        ),
        background = backgroundValue.toEditorBackground(),
        layers = emptyList(),
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
                    zIndex = placeholder.zIndex,
                    maskPathOrUrl = placeholder.maskUrl,
                    previewPathOrUrl = placeholder.previewUrl
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
