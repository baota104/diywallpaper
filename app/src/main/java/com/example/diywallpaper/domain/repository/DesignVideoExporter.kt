package com.example.diywallpaper.domain.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject

interface DesignVideoExporter {
    suspend fun export(project: EditorProject): AppResult<String>
}
