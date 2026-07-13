package com.example.diywallpaper.domain.usecase.design

import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.repository.UserDesignRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUserDesignsUseCase @Inject constructor(
    private val userDesignRepository: UserDesignRepository
) {
    operator fun invoke(): Flow<List<UserDesign>> = userDesignRepository.observeDesigns()
}
