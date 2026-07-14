package com.example.diywallpaper.domain.model.design

fun EditorProject.hasAnimatedContent(): Boolean {
    return layers.any { layer ->
        when (layer) {
            is StickerLayer -> layer.isAnimated || !layer.animatedAssetPathOrUrl.isNullOrBlank()
            is DrawLayer,
            is PhotoLayer,
            is TextLayer -> false
        }
    }
}
