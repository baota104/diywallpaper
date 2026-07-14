package com.example.diywallpaper.domain.model.design

import kotlinx.serialization.Serializable

@Serializable
data class EditorProject(
    val id: String,
    val source: EditorProjectSource,
    val canvas: EditorCanvasSpec,
    val background: EditorBackground,
    val layers: List<EditorLayer>,
    val placeholders: List<PhotoPlaceholderLayer>,
    val selectedLayerId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val schemaVersion: Int
)

@Serializable
sealed interface EditorProjectSource {
    @Serializable
    data class Diy(
        val templateId: String,
        val templateSnapshot: DiyTemplateSnapshot
    ) : EditorProjectSource

    @Serializable
    data object Scratch : EditorProjectSource
}

@Serializable
data class EditorCanvasSpec(
    val width: Int,
    val height: Int
)

@Serializable
sealed interface EditorBackground {
    @Serializable
    data class SolidColor(val colorHex: String) : EditorBackground

    @Serializable
    data class Gradient(val colors: List<String>) : EditorBackground

    @Serializable
    data class ApiImage(
        val backgroundId: String,
        val imageUrl: String
    ) : EditorBackground

    @Serializable
    data class LocalImage(
        val localPath: String,
        val crop: CropSpec? = null
    ) : EditorBackground
}

@Serializable
enum class CropPresetRatio {
    RATIO_9_16,
    RATIO_3_4,
    RATIO_2_3,
    RATIO_1_1
}

@Serializable
data class CropSpec(
    val normalizedLeft: Float,
    val normalizedTop: Float,
    val normalizedRight: Float,
    val normalizedBottom: Float,
    val ratio: CropPresetRatio? = null
)

@Serializable
data class DiyTemplateSnapshot(
    val width: Int,
    val height: Int,
    val background: TemplateBackgroundSnapshot? = null,
    val elements: List<DiyTemplateElementSnapshot> = emptyList()
)

@Serializable
data class DiyTemplateElementSnapshot(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val zIndex: Int,
    val assetUrl: String? = null
)

@Serializable
sealed interface TemplateBackgroundSnapshot {
    @Serializable
    data class ColorHex(val colorHex: String) : TemplateBackgroundSnapshot

    @Serializable
    data class AssetUrl(val url: String) : TemplateBackgroundSnapshot
}

@Serializable
sealed interface EditorLayer {
    val id: String
    val zIndex: Int
    val transform: LayerTransform
    val isLocked: Boolean
    val isHidden: Boolean
}

@Serializable
data class LayerTransform(
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float,
    val rotation: Float,
    val alpha: Float = 1f
)

@Serializable
data class PhotoLayer(
    override val id: String,
    val localPath: String,
    val crop: CropSpec? = null,
    val placeholderId: String? = null,
    override val zIndex: Int,
    override val transform: LayerTransform,
    override val isLocked: Boolean,
    override val isHidden: Boolean
) : EditorLayer

@Serializable
data class StickerLayer(
    override val id: String,
    val stickerId: String? = null,
    val assetPathOrUrl: String,
    val animatedAssetPathOrUrl: String? = null,
    val isAnimated: Boolean = false,
    override val zIndex: Int,
    override val transform: LayerTransform,
    override val isLocked: Boolean,
    override val isHidden: Boolean
) : EditorLayer

@Serializable
data class TextLayer(
    override val id: String,
    val text: String,
    val style: TextStyleSpec,
    override val zIndex: Int,
    override val transform: LayerTransform,
    override val isLocked: Boolean,
    override val isHidden: Boolean
) : EditorLayer

@Serializable
data class DrawLayer(
    override val id: String,
    val drawData: DrawLayerData,
    override val zIndex: Int,
    override val transform: LayerTransform,
    override val isLocked: Boolean,
    override val isHidden: Boolean
) : EditorLayer

@Serializable
data class TextStyleSpec(
    val fontFamilyId: String,
    val fontDisplayName: String,
    val fontSizeSp: Float,
    val textColorHex: String? = null,
    val textBrush: TextBrushStyle? = null,
    val letterSpacing: Float = 0f,
    val lineHeight: Float? = null,
    val textAlign: EditorTextAlign = EditorTextAlign.START,
    val shadow: TextShadowSpec? = null
)

@Serializable
enum class EditorTextAlign {
    START,
    CENTER,
    END
}

@Serializable
data class TextShadowSpec(
    val colorHex: String,
    val blurRadius: Float,
    val offsetX: Float,
    val offsetY: Float
)

@Serializable
sealed interface TextBrushStyle {
    @Serializable
    data class Solid(val colorHex: String) : TextBrushStyle

    @Serializable
    data class Gradient(val colors: List<String>) : TextBrushStyle
}

@Serializable
sealed interface DrawLayerData {
    @Serializable
    data class FreeStroke(val stroke: BrushStroke) : DrawLayerData

    @Serializable
    data class EraseStroke(val stroke: BrushStroke) : DrawLayerData

    @Serializable
    data class BrushStack(
        val items: List<BrushStackItem>
    ) : DrawLayerData

    @Serializable
    data class StickerTrail(
        val stickerAssetPathOrUrl: String,
        val points: List<StrokePoint>,
        val spacing: Float,
        val stampSize: Float,
        val rotationMode: StickerTrailRotationMode
    ) : DrawLayerData

    @Serializable
    data class TextTrail(
        val text: String,
        val textStyle: TextStyleSpec,
        val points: List<StrokePoint>,
        val spacing: Float
    ) : DrawLayerData
}

@Serializable
sealed interface BrushStackItem {
    @Serializable
    data class Draw(val stroke: BrushStroke) : BrushStackItem

    @Serializable
    data class Erase(val stroke: BrushStroke) : BrushStackItem
}

@Serializable
data class BrushStroke(
    val points: List<StrokePoint>,
    val colorHex: String? = null,
    val brushStyle: BrushStyleSpec? = null,
    val strokeWidth: Float
)

@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float
)

@Serializable
sealed interface BrushStyleSpec {
    @Serializable
    data class Solid(val colorHex: String) : BrushStyleSpec

    @Serializable
    data class Gradient(val colors: List<String>) : BrushStyleSpec
}

@Serializable
enum class StickerTrailRotationMode {
    FIXED,
    FOLLOW_PATH
}

@Serializable
data class PhotoPlaceholderLayer(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val zIndex: Int
)
