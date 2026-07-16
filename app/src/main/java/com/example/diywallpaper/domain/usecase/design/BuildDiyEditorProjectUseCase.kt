package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.repository.DiyRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class BuildDiyEditorProjectUseCase @Inject constructor(
    private val diyRepository: DiyRepository
) {
    suspend operator fun invoke(
        templateId: String,
        projectId: String
    ): AppResult<EditorProject> {
        val template = diyRepository.observeDiyTemplates()
            .first()
            .firstOrNull { it.id == templateId }
            ?: return AppResult.Error(
                AppError.StorageError("DIY template not found")
            )

        return when (
            val templateDataResult = diyRepository.getDiyTemplateData(
                templateId = template.id,
                diyDataUrl = template.diyDataUrl,
                dataZipUrl = template.dataZipUrl
            )
        ) {
            is AppResult.Success -> AppResult.Success(
                template.toEditorProject(
                    templateData = templateDataResult.data,
                    projectId = projectId
                )
            )

            is AppResult.Error -> templateDataResult
        }
    }
}
