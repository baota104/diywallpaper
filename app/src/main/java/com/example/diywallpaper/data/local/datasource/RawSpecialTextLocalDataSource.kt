package com.example.diywallpaper.data.local.datasource

import android.content.Context
import com.example.diywallpaper.R
import com.example.diywallpaper.data.local.dto.SpecialTextDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.serialization.json.Json

class RawSpecialTextLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SpecialTextLocalDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun getSpecialTexts(): List<SpecialTextDto> {
        return runCatching {
            val rawJson = context.resources
                .openRawResource(R.raw.special_text)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            json.decodeFromString<List<SpecialTextDto>>(rawJson)
                .filter { it.data.isNotBlank() }
                .sortedWith(compareBy<SpecialTextDto> { it.rank }.thenBy { it.id })
        }.getOrDefault(emptyList())
    }
}
