package com.example.diywallpaper.domain.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.GeneratedDesignAssets

interface DesignAssetExporter {
    suspend fun export(project: EditorProject): AppResult<GeneratedDesignAssets>
}
