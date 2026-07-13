package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.domain.model.design.EditorFontOption
import com.example.diywallpaper.domain.model.design.EditorTextPreset
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import javax.inject.Inject

class GetEditorTextLibraryUseCase @Inject constructor() {
    operator fun invoke(): EditorTextLibrary {
        return EditorTextLibrary(
            fonts = listOf(
                EditorFontOption(
                    id = FONT_INTER,
                    displayName = "Inter"
                ),
                EditorFontOption(
                    id = FONT_PLUS_JAKARTA_SANS,
                    displayName = "Plus Jakarta Sans"
                ),
                EditorFontOption("allura", "Allura"),
                EditorFontOption("are_you_serious", "Are You Serious"),
                EditorFontOption("arizonia", "Arizonia"),
                EditorFontOption("bagel_fat_one", "Bagel Fat One"),
                EditorFontOption("dawning_of_a_new_day", "Dawning"),
                EditorFontOption("delius_wash_caps", "Delius"),
                EditorFontOption("fredoka_condensed", "Fredoka"),
                EditorFontOption("ledger", "Ledger"),
                EditorFontOption("lemon", "Lemon"),
                EditorFontOption("lexend_deca", "Lexend"),
                EditorFontOption("licorice", "Licorice"),
                EditorFontOption("lieckerli_one", "L Lieckerli"),
                EditorFontOption("londrina_shadow", "Londrina"),
                EditorFontOption("manrope", "Manrope"),
                EditorFontOption("ma_shan_zheng", "Ma Shan"),
                EditorFontOption("meow_script", "Meow"),
                EditorFontOption("overlock", "Overlock"),
                EditorFontOption("roboto_condensed", "Roboto"),
                EditorFontOption("sansita_swashed", "Sansita"),
                EditorFontOption("sora", "Sora")
            ),
            presets = listOf(
                EditorTextPreset(
                    id = "preset_clean",
                    title = "Clean",
                    previewText = "Hello",
                    style = TextStyleSpec(
                        fontFamilyId = FONT_INTER,
                        fontDisplayName = "Inter",
                        fontSizeSp = 28f,
                        textColorHex = "#201A2E"
                    )
                ),
                EditorTextPreset(
                    id = "preset_soft",
                    title = "Soft",
                    previewText = "Dream",
                    style = TextStyleSpec(
                        fontFamilyId = FONT_PLUS_JAKARTA_SANS,
                        fontDisplayName = "Plus Jakarta Sans",
                        fontSizeSp = 30f,
                        textColorHex = "#8B5CF6"
                    )
                ),
                EditorTextPreset(
                    id = "preset_pop",
                    title = "Pop",
                    previewText = "Lovely",
                    style = TextStyleSpec(
                        fontFamilyId = FONT_PLUS_JAKARTA_SANS,
                        fontDisplayName = "Plus Jakarta Sans",
                        fontSizeSp = 32f,
                        textColorHex = "#FF7A8B"
                    )
                ),
                EditorTextPreset(
                    id = "preset_glow",
                    title = "Glow",
                    previewText = "Shine",
                    style = TextStyleSpec(
                        fontFamilyId = FONT_INTER,
                        fontDisplayName = "Inter",
                        fontSizeSp = 30f,
                        textColorHex = "#201A2E",
                        textBrush = TextBrushStyle.Gradient(
                            colors = listOf("#FF7A8B", "#8B5CF6")
                        )
                    )
                )
            )
        )
    }

    companion object {
        const val FONT_INTER = "inter"
        const val FONT_PLUS_JAKARTA_SANS = "plus_jakarta_sans"
    }
}

data class EditorTextLibrary(
    val fonts: List<EditorFontOption>,
    val presets: List<EditorTextPreset>
)
