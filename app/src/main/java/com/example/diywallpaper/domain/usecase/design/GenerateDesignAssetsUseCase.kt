package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.GeneratedDesignAssets
import com.example.diywallpaper.domain.repository.DesignAssetExporter
import javax.inject.Inject

class GenerateDesignAssetsUseCase @Inject constructor(
    private val designAssetExporter: DesignAssetExporter
) {
    suspend operator fun invoke(project: EditorProject): AppResult<GeneratedDesignAssets> {
        return designAssetExporter.export(project)
    }
}
