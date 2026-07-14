package com.example.diywallpaper.domain.usecase.wallpaper

import android.content.Intent
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.repository.DesignVideoExporter
import javax.inject.Inject

class SetLiveDesignWallpaperUseCase @Inject constructor(
    private val designVideoExporter: DesignVideoExporter,
    private val setLiveWallpaperUseCase: SetLiveWallpaperUseCase
) {
    suspend operator fun invoke(project: EditorProject): AppResult<Intent> {
        return when (val exportedVideo = designVideoExporter.export(project)) {
            is AppResult.Success -> {
                setLiveWallpaperUseCase(
                    WallpaperApplySource.LiveVideo(
                        itemId = project.id,
                        videoUrl = LOCAL_DESIGN_VIDEO_URL,
                        localPath = exportedVideo.data
                    )
                )
            }

            is AppResult.Error -> AppResult.Error(exportedVideo.error)
        }
    }

    private companion object {
        const val LOCAL_DESIGN_VIDEO_URL = "file://local-design-video.mp4"
    }
}
