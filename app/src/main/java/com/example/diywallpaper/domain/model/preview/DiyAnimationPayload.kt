package com.example.diywallpaper.domain.model.preview

enum class DiyAnimationState {
    NOT_REQUESTED,
    LOADING,
    READY,
    UNSUPPORTED,
    ERROR
}

data class DiyAnimationPayload(
    val templateId: String,
    val animationUrl: String,
    val rawJson: String?,
    val state: DiyAnimationState
)
