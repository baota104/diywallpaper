package com.example.diywallpaper.domain.design

import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetEditorTextLibraryUseCaseTest {

    @Test
    fun `returns local fonts and text presets`() {
        val useCase = GetEditorTextLibraryUseCase()

        val result = useCase()

        assertTrue(result.fonts.size > 2)
        assertEquals(GetEditorTextLibraryUseCase.FONT_INTER, result.fonts.first().id)
        assertTrue(result.fonts.any { it.id == "allura" })
        assertTrue(result.presets.isNotEmpty())
        assertTrue(result.presets.any { it.style.fontFamilyId == GetEditorTextLibraryUseCase.FONT_PLUS_JAKARTA_SANS })
    }
}
