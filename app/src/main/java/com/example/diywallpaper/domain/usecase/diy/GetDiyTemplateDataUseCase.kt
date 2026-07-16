package com.example.diywallpaper.domain.usecase.diy

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.repository.DiyRepository
import javax.inject.Inject

class GetDiyTemplateDataUseCase @Inject constructor(
    private val diyRepository: DiyRepository
) {
    suspend operator fun invoke(
        templateId: String,
        diyDataUrl: String,
        dataZipUrl: String? = null
    ): AppResult<DiyTemplateData> {
        return diyRepository.getDiyTemplateData(
            templateId = templateId,
            diyDataUrl = diyDataUrl,
            dataZipUrl = dataZipUrl
        )
    }
}
