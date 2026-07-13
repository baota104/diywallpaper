package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.repository.UserDesignRepository
import javax.inject.Inject

class RenameDesignUseCase @Inject constructor(
    private val userDesignRepository: UserDesignRepository
) {
    suspend operator fun invoke(designId: String, title: String): AppResult<Unit> {
        return userDesignRepository.renameDesign(designId, title)
    }
}
