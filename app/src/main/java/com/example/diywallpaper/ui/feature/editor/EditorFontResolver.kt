package com.example.diywallpaper.ui.feature.editor

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase
import com.example.diywallpaper.ui.theme.Inter
import com.example.diywallpaper.ui.theme.PlusJakartaSans

fun editorFontFamily(fontFamilyId: String): FontFamily {
    return when (fontFamilyId) {
        GetEditorTextLibraryUseCase.FONT_PLUS_JAKARTA_SANS -> PlusJakartaSans
        GetEditorTextLibraryUseCase.FONT_INTER -> Inter
        "allura" -> FontFamily(Font(R.font.allura_regular))
        "are_you_serious" -> FontFamily(Font(R.font.areyouserious_regular))
        "arizonia" -> FontFamily(Font(R.font.arizonia_regular))
        "bagel_fat_one" -> FontFamily(Font(R.font.bagelfatone_regular))
        "dawning_of_a_new_day" -> FontFamily(Font(R.font.dawningofanewday_regular))
        "delius_wash_caps" -> FontFamily(Font(R.font.deliuswashcaps_regular))
        "fredoka_condensed" -> FontFamily(
            Font(R.font.fredoka_condensed_regular, weight = FontWeight.Normal),
            Font(R.font.fredoka_condensed_medium, weight = FontWeight.Medium)
        )
        "ledger" -> FontFamily(Font(R.font.ledger_regular))
        "lemon" -> FontFamily(Font(R.font.lemon_regular))
        "lexend_deca" -> FontFamily(Font(R.font.lexenddeca_regular))
        "licorice" -> FontFamily(Font(R.font.licorice_regular))
        "lieckerli_one" -> FontFamily(Font(R.font.lieckerlione_regular))
        "londrina_shadow" -> FontFamily(Font(R.font.londrina_shadow_regular))
        "manrope" -> FontFamily(Font(R.font.manrope_medium, weight = FontWeight.Medium))
        "ma_shan_zheng" -> FontFamily(Font(R.font.mashanzheng_regular))
        "meow_script" -> FontFamily(Font(R.font.meowscript_regular))
        "overlock" -> FontFamily(Font(R.font.overlock_regular))
        "roboto_condensed" -> FontFamily(Font(R.font.roboto_condensed_regular))
        "sansita_swashed" -> FontFamily(Font(R.font.sansitaswashed_regular))
        "sora" -> FontFamily(Font(R.font.sora_regular))
        else -> Inter
    }
}

fun editorFontResId(fontFamilyId: String): Int {
    return when (fontFamilyId) {
        GetEditorTextLibraryUseCase.FONT_PLUS_JAKARTA_SANS -> R.font.plusjakartasans_semibold
        GetEditorTextLibraryUseCase.FONT_INTER -> R.font.inter_18pt_semibold
        "allura" -> R.font.allura_regular
        "are_you_serious" -> R.font.areyouserious_regular
        "arizonia" -> R.font.arizonia_regular
        "bagel_fat_one" -> R.font.bagelfatone_regular
        "dawning_of_a_new_day" -> R.font.dawningofanewday_regular
        "delius_wash_caps" -> R.font.deliuswashcaps_regular
        "fredoka_condensed" -> R.font.fredoka_condensed_medium
        "ledger" -> R.font.ledger_regular
        "lemon" -> R.font.lemon_regular
        "lexend_deca" -> R.font.lexenddeca_regular
        "licorice" -> R.font.licorice_regular
        "lieckerli_one" -> R.font.lieckerlione_regular
        "londrina_shadow" -> R.font.londrina_shadow_regular
        "manrope" -> R.font.manrope_medium
        "ma_shan_zheng" -> R.font.mashanzheng_regular
        "meow_script" -> R.font.meowscript_regular
        "overlock" -> R.font.overlock_regular
        "roboto_condensed" -> R.font.roboto_condensed_regular
        "sansita_swashed" -> R.font.sansitaswashed_regular
        "sora" -> R.font.sora_regular
        else -> R.font.inter_18pt_semibold
    }
}
