package com.example.diywallpaper.domain.usecase.diy

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.repository.DiyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetDiyTemplatesUseCase @Inject constructor(
    private val diyRepository: DiyRepository
) {
    operator fun invoke(): Flow<List<DiyTemplate>> = diyRepository.observeDiyTemplates()

    suspend fun refresh(): AppResult<Unit> = diyRepository.refreshDiyTemplates()
}
