package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.repository.UserDesignRepository
import javax.inject.Inject

class UpdateDesignAssetsUseCase @Inject constructor(
    private val userDesignRepository: UserDesignRepository
) {
    suspend operator fun invoke(
        designId: String,
        thumbnailPath: String? = null,
        previewPath: String? = null,
        exportedImagePath: String? = null
    ): AppResult<Unit> {
        return userDesignRepository.updateAssets(
            designId = designId,
            thumbnailPath = thumbnailPath,
            previewPath = previewPath,
            exportedImagePath = exportedImagePath
        )
    }
}
