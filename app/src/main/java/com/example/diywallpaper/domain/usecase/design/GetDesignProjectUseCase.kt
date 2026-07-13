package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.repository.UserDesignRepository
import javax.inject.Inject

class GetDesignProjectUseCase @Inject constructor(
    private val userDesignRepository: UserDesignRepository
) {
    suspend operator fun invoke(designId: String): AppResult<EditorProject> {
        return userDesignRepository.getProject(designId)
    }
}
