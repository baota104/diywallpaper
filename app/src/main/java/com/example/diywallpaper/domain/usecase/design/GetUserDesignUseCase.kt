package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.repository.UserDesignRepository
import javax.inject.Inject

class GetUserDesignUseCase @Inject constructor(
    private val userDesignRepository: UserDesignRepository
) {
    suspend operator fun invoke(designId: String): AppResult<UserDesign> {
        return userDesignRepository.getDesign(designId)
    }
}
