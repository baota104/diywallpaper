package com.example.diywallpaper.domain.usecase.diy

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.repository.DiyRepository
import javax.inject.Inject

class ToggleDiyFavoriteUseCase @Inject constructor(
    private val diyRepository: DiyRepository
) {
    suspend operator fun invoke(templateId: String): AppResult<Unit> {
        return diyRepository.toggleFavorite(templateId)
    }
}
