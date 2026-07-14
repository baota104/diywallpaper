package com.example.diywallpaper.data.remote.dto

import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.DiyBackgroundValue
import com.example.diywallpaper.domain.model.DiyElement
import com.example.diywallpaper.domain.model.DiyElementType
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.PhotoPlaceholder
import com.example.diywallpaper.domain.model.StickerItem
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.model.WallpaperItem
import com.example.diywallpaper.domain.model.WallpaperType

private const val DIY_CATEGORY_NAME = "DIY"

fun RemoteCategoryDto.toWallpaperCategoryOrNull(): WallpaperCategory? {
    if (category.equals(DIY_CATEGORY_NAME, ignoreCase = true)) return null

    val categoryId = category.trim()
    if (categoryId.isBlank()) return null

    return WallpaperCategory(
        id = categoryId,
        name = category,
        iconUrl = icon,
        rank = rank,
        items = items.mapNotNull { it.toWallpaperDomainOrNull(categoryId) }
    )
}

fun RemoteCategoryDto.toDiyTemplates(): List<DiyTemplate> {
    if (!category.equals(DIY_CATEGORY_NAME, ignoreCase = true)) return emptyList()
    return items.mapNotNull { it.toDiyTemplateDomainOrNull() }
}

fun RemoteItemDto.toWallpaperDomainOrNull(categoryId: String): WallpaperItem? {
    val normalizedType = type?.trim().orEmpty()
    val validThumb = thumb?.takeIf { it.isNotBlank() }
    val validData = data?.takeIf { it.isNotBlank() }

    if (normalizedType == "2d" && validThumb == null && validData == null) return null
    if (normalizedType == "live" && validThumb == null) return null
    if (normalizedType != "2d" && normalizedType != "live" && validThumb == null) return null

    val resolvedThumb = validThumb ?: validData ?: return null

    return WallpaperItem(
        id = "wallpaper_${categoryId}_$id",
        categoryId = categoryId,
        type = when (normalizedType) {
            "2d" -> WallpaperType.STATIC_2D
            "live" -> WallpaperType.LIVE_VIDEO
            else -> WallpaperType.UNKNOWN
        },
        rank = rank,
        thumbUrl = resolvedThumb,
        imageUrl = when (normalizedType) {
            "2d" -> validData ?: resolvedThumb
            else -> null
        },
        videoUrl = when (normalizedType) {
            "live" -> content?.takeIf { it.isNotBlank() }
            else -> null
        },
        isFavorite = false
    )
}

fun RemoteItemDto.toDiyTemplateDomainOrNull(): DiyTemplate? {
    val validDiyData = diyData?.takeIf { it.isNotBlank() } ?: return null

    return DiyTemplate(
        id = id.toString(),
        type = when (type) {
            "diy-live" -> DiyTemplateType.DIY_LIVE
            "diy-static" -> DiyTemplateType.DIY_STATIC
            else -> DiyTemplateType.DIY_STATIC
        },
        rank = rank,
        thumbUrl = thumb.orEmpty(),
        diyDataUrl = validDiyData,
        diyAnimationUrl = diyAnimation?.takeIf { it.isNotBlank() },
        isFavorite = false
    )
}

fun BackgroundCreateDto.toDomainOrNull(): BackgroundCreateItem? {
    val imageUrl = data.takeIf { it.isNotBlank() } ?: return null

    return BackgroundCreateItem(
        id = id.toString(),
        rank = categoryRank,
        name = name,
        imageUrl = imageUrl,
        thumbnailUrl = imageUrl
    )
}

fun StickerDto.toDomainOrNull(): StickerItem? {
    val stickerUrl = stickers.takeIf { it.isNotBlank() } ?: return null

    return StickerItem(
        id = id.toString(),
        rank = rank,
        stickerUrl = stickerUrl,
        thumbnailUrl = stickerUrl,
        isAnimated = stickerUrl.isAnimatedStickerAsset()
    )
}

private fun String.isAnimatedStickerAsset(): Boolean {
    val cleanUrl = substringBefore("?").lowercase()
    return cleanUrl.endsWith(".gif") ||
        cleanUrl.endsWith(".webp") ||
        cleanUrl.endsWith(".apng")
}

fun DiyTemplateDataDto.toDomain(diyDataUrl: String): DiyTemplateData {
    val sortedElements = elements.sortedBy { it.layoutIndex }

    return DiyTemplateData(
        width = width,
        height = height,
        background = background,
        elements = sortedElements.map { it.toDomain(diyDataUrl) },
        placeholders = sortedElements
            .filter { it.type == "Image" }
            .map { it.toPlaceholder() }
    )
}

fun DiyElementDto.toDomain(diyDataUrl: String): DiyElement {
    val mappedType = when (type) {
        "Picture" -> DiyElementType.PICTURE
        "Image" -> DiyElementType.IMAGE
        else -> DiyElementType.UNKNOWN
    }

    return DiyElement(
        type = mappedType,
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = angle,
        zIndex = layoutIndex,
        srcName = srcName,
        assetUrl = when (mappedType) {
            DiyElementType.PICTURE -> srcName.takeIf { it.isNotBlank() }?.let {
                resolveDiyAssetUrl(diyDataUrl, it)
            }
            else -> null
        }
    )
}

fun DiyElementDto.toPlaceholder(): PhotoPlaceholder {
    return PhotoPlaceholder(
        id = buildPlaceholderId(this),
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = angle,
        zIndex = layoutIndex
    )
}

fun resolveDiyAssetUrl(diyDataUrl: String, srcName: String): String {
    val baseUrl = diyDataUrl.substringBeforeLast("/")
    return "$baseUrl/$srcName"
}

fun resolveDiyBackgroundValue(diyDataUrl: String, background: String): DiyBackgroundValue {
    val value = background.trim()

    if (value.isBlank()) return DiyBackgroundValue.Empty
    if (value.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) {
        return DiyBackgroundValue.ColorHex(value)
    }
    if (value.startsWith("http://") || value.startsWith("https://")) {
        return DiyBackgroundValue.RemoteUrl(value)
    }

    return DiyBackgroundValue.AssetUrl(resolveDiyAssetUrl(diyDataUrl, value))
}

fun buildPlaceholderId(element: DiyElementDto): String {
    val safeSrcName = element.srcName
        .ifBlank { "empty" }
        .replace("/", "_")
        .replace("\\", "_")

    return "image_${element.layoutIndex}_$safeSrcName"
}
