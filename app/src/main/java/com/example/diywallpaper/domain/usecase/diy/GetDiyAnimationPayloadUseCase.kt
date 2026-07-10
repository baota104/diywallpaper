package com.example.diywallpaper.domain.usecase.diy

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.DiyAnimationPayload
import com.example.diywallpaper.domain.model.preview.DiyAnimationState
import com.example.diywallpaper.domain.repository.DiyRepository
import javax.inject.Inject

class GetDiyAnimationPayloadUseCase @Inject constructor(
    private val diyRepository: DiyRepository
) {
    suspend operator fun invoke(
        templateId: String,
        animationUrl: String
    ): AppResult<DiyAnimationPayload> {
        return when (val result = diyRepository.getDiyAnimationRaw(templateId, animationUrl)) {
            is AppResult.Success -> {
                AppResult.Success(
                    DiyAnimationPayload(
                        templateId = result.data.templateId,
                        animationUrl = result.data.animationUrl,
                        rawJson = result.data.rawJson,
                        state = if (result.data.rawJson.isNullOrBlank()) {
                            DiyAnimationState.UNSUPPORTED
                        } else {
                            DiyAnimationState.READY
                        }
                    )
                )
            }

            is AppResult.Error -> result
        }
    }
}
