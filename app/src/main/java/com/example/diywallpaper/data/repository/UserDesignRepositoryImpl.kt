package com.example.diywallpaper.data.repository

import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.local.dao.UserDesignDao
import com.example.diywallpaper.data.local.entity.UserDesignEntity
import com.example.diywallpaper.data.local.files.DesignFileStore
import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.repository.UserDesignRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class UserDesignRepositoryImpl @Inject constructor(
    private val userDesignDao: UserDesignDao,
    private val designFileStore: DesignFileStore
) : UserDesignRepository {

    override fun observeDesigns(): Flow<List<UserDesign>> {
        return userDesignDao.observeActiveDesigns().map { entities ->
            entities.map(UserDesignEntity::toDomain)
        }
    }

    override suspend fun getDesign(designId: String): AppResult<UserDesign> {
        return runCatching {
            userDesignDao.getDesignById(designId)?.toDomain()
        }.fold(
            onSuccess = { design ->
                if (design == null || design.isDeleted) {
                    AppResult.Error(AppError.StorageError("Design not found"))
                } else {
                    AppResult.Success(design)
                }
            },
            onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
        )
    }

    override suspend fun createDraft(project: EditorProject, title: String?): AppResult<String> {
        val designId = project.id.ifBlank { UUID.randomUUID().toString() }
        val normalizedProject = project.copy(id = designId)
        val projectFilePath = when (val writeResult = designFileStore.writeProject(normalizedProject)) {
            is AppResult.Success -> writeResult.data
            is AppResult.Error -> return writeResult
        }

        val now = System.currentTimeMillis()
        val entity = normalizedProject.toEntity(
            title = title,
            projectFilePath = projectFilePath,
            createdAt = normalizedProject.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = normalizedProject.updatedAt.takeIf { it > 0L } ?: now,
            lastOpenedAt = now
        )

        return runCatching {
            userDesignDao.upsertDesign(entity)
            designId
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
        )
    }

    override suspend fun getProject(designId: String): AppResult<EditorProject> {
        val entity = runCatching { userDesignDao.getDesignById(designId) }
            .fold(
                onSuccess = { it },
                onFailure = { return AppResult.Error(AppError.StorageError(it.message)) }
            )
            ?: return AppResult.Error(AppError.StorageError("Design not found"))

        return when (val projectResult = designFileStore.readProject(entity.projectFilePath)) {
            is AppResult.Success -> {
                runCatching {
                    userDesignDao.updateDesignSnapshot(
                        designId = entity.id,
                        title = entity.title,
                        thumbnailPath = entity.thumbnailPath,
                        previewPath = entity.previewPath,
                        exportedImagePath = entity.exportedImagePath,
                        updatedAt = entity.updatedAt,
                        lastOpenedAt = System.currentTimeMillis(),
                        canvasWidth = entity.canvasWidth,
                        canvasHeight = entity.canvasHeight,
                        schemaVersion = entity.schemaVersion
                    )
                }
                AppResult.Success(projectResult.data)
            }
            is AppResult.Error -> projectResult
        }
    }

    override suspend fun saveProject(project: EditorProject, title: String?): AppResult<Unit> {
        val projectFilePath = when (val writeResult = designFileStore.writeProject(project)) {
            is AppResult.Success -> writeResult.data
            is AppResult.Error -> return writeResult
        }

        val existing = runCatching { userDesignDao.getDesignById(project.id) }
            .fold(
                onSuccess = { it },
                onFailure = { return AppResult.Error(AppError.StorageError(it.message)) }
            )

        val now = System.currentTimeMillis()
        val baseEntity = existing ?: project.toEntity(
            title = title,
            projectFilePath = projectFilePath,
            createdAt = project.createdAt.takeIf { it > 0L } ?: now,
            updatedAt = now,
            lastOpenedAt = now
        )

        val entity = baseEntity.copy(
            sourceType = project.source.toStorageValue(),
            title = title ?: baseEntity.title,
            projectFilePath = projectFilePath,
            canvasWidth = project.canvas.width,
            canvasHeight = project.canvas.height,
            templateId = project.source.templateIdOrNull(),
            updatedAt = now,
            lastOpenedAt = now,
            schemaVersion = project.schemaVersion
        )

        return runCatching {
            userDesignDao.upsertDesign(entity)
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
        )
    }

    override suspend fun renameDesign(designId: String, title: String): AppResult<Unit> {
        return runCatching {
            userDesignDao.renameDesign(
                designId = designId,
                title = title,
                updatedAt = System.currentTimeMillis()
            )
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
        )
    }

    override suspend fun updateAssets(
        designId: String,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?
    ): AppResult<Unit> {
        return runCatching {
            userDesignDao.updateAssets(
                designId = designId,
                thumbnailPath = thumbnailPath,
                previewPath = previewPath,
                exportedImagePath = exportedImagePath,
                updatedAt = System.currentTimeMillis()
            )
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
        )
    }

    override suspend fun deleteDesign(designId: String): AppResult<Unit> {
        val deleteFilesResult = designFileStore.deleteDesignFiles(designId)
        if (deleteFilesResult is AppResult.Error) {
            return deleteFilesResult
        }

        return runCatching {
            userDesignDao.markDeleted(
                designId = designId,
                updatedAt = System.currentTimeMillis()
            )
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
        )
    }
}

private fun UserDesignEntity.toDomain(): UserDesign {
    return UserDesign(
        id = id,
        sourceType = runCatching { DesignSourceType.valueOf(sourceType) }
            .getOrDefault(DesignSourceType.SCRATCH),
        title = title,
        thumbnailPath = thumbnailPath,
        previewPath = previewPath,
        templateId = templateId,
        projectFilePath = projectFilePath,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        exportedImagePath = exportedImagePath,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        isDeleted = isDeleted,
        schemaVersion = schemaVersion
    )
}

private fun EditorProject.toEntity(
    title: String?,
    projectFilePath: String,
    createdAt: Long,
    updatedAt: Long,
    lastOpenedAt: Long
): UserDesignEntity {
    return UserDesignEntity(
        id = id,
        sourceType = source.toStorageValue(),
        title = title,
        thumbnailPath = null,
        previewPath = null,
        templateId = source.templateIdOrNull(),
        projectFilePath = projectFilePath,
        canvasWidth = canvas.width,
        canvasHeight = canvas.height,
        exportedImagePath = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastOpenedAt = lastOpenedAt,
        isDeleted = false,
        schemaVersion = schemaVersion
    )
}

private fun EditorProjectSource.toStorageValue(): String = when (this) {
    is EditorProjectSource.Diy -> DesignSourceType.DIY_TEMPLATE.name
    EditorProjectSource.Scratch -> DesignSourceType.SCRATCH.name
}

private fun EditorProjectSource.templateIdOrNull(): String? = when (this) {
    is EditorProjectSource.Diy -> templateId
    EditorProjectSource.Scratch -> null
}
