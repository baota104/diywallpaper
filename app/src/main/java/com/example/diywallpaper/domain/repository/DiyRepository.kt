package com.example.diywallpaper.domain.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.DiyAnimationRaw
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import kotlinx.coroutines.flow.Flow

interface DiyRepository {
    fun observeDiyTemplates(): Flow<List<DiyTemplate>>

    suspend fun refreshDiyTemplates(): AppResult<Unit>

    suspend fun toggleFavorite(templateId: String): AppResult<Unit>

    suspend fun getDiyTemplateData(
        templateId: String,
        diyDataUrl: String,
        dataZipUrl: String? = null,
    ): AppResult<DiyTemplateData>

    suspend fun getDiyAnimationRaw(
        templateId: String,
        animationUrl: String
    ): AppResult<DiyAnimationRaw>
}
