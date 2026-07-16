package com.example.diywallpaper.data.mapper

import com.example.diywallpaper.data.local.dto.SpecialTextDto
import com.example.diywallpaper.domain.model.design.EditorTextPreset
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase

fun SpecialTextDto.toEditorTextPreset(): EditorTextPreset {
    return EditorTextPreset(
        id = "special_text_$id",
        title = "Style $rank",
        previewText = data,
        style = TextStyleSpec(
            fontFamilyId = GetEditorTextLibraryUseCase.FONT_PLUS_JAKARTA_SANS,
            fontDisplayName = "Plus Jakarta Sans",
            fontSizeSp = 22f,
            textColorHex = "#201A2E"
        )
    )
}
