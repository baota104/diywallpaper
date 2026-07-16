package com.example.diywallpaper.data.local.datasource

import com.example.diywallpaper.data.local.dto.SpecialTextDto

interface SpecialTextLocalDataSource {
    fun getSpecialTexts(): List<SpecialTextDto>
}
