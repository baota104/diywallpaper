package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.repository.UserDesignRepository
import javax.inject.Inject

class CreateDesignDraftUseCase @Inject constructor(
    private val userDesignRepository: UserDesignRepository
) {
    suspend operator fun invoke(
        project: EditorProject,
        title: String? = null
    ): AppResult<String> {
        return userDesignRepository.createDraft(project, title)
    }
}
