package com.example.diywallpaper.ui.feature.preview

import androidx.annotation.StringRes
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction

@StringRes
fun PreviewPrimaryAction.toLabelRes(): Int {
    return when (this) {
        PreviewPrimaryAction.SET_WALLPAPER -> R.string.preview_next
        PreviewPrimaryAction.SET_LIVE_WALLPAPER -> R.string.preview_next
        PreviewPrimaryAction.EDIT_DIY -> R.string.preview_edit
        PreviewPrimaryAction.CREATE_FROM_SCRATCH -> R.string.preview_create
    }
}

@StringRes
fun PreviewPrimaryAction.toDeviceLabelRes(): Int {
    return when (this) {
        PreviewPrimaryAction.SET_WALLPAPER -> R.string.preview_set_wallpaper
        PreviewPrimaryAction.SET_LIVE_WALLPAPER -> R.string.preview_set_live_wallpaper
        PreviewPrimaryAction.EDIT_DIY -> R.string.preview_edit
        PreviewPrimaryAction.CREATE_FROM_SCRATCH -> R.string.preview_create_from_scratch
    }
}
