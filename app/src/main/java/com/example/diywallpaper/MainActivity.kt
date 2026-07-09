package com.example.diywallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.navigation.AppNavGraph
import com.example.diywallpaper.core.utils.manager.LocaleManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleManager.wrapContext(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val savedLanguage = LocaleManager.getLocale(this).ifBlank {
                java.util.Locale.getDefault().language
            }
            var currentLanguage by remember { mutableStateOf(savedLanguage) }
            val localizedConfiguration = remember(currentLanguage) {
                LocaleManager.createConfiguration(resources.configuration, currentLanguage)
            }
            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration
            ) {
                DIYWallpaperTheme() {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavGraph(
                            onLanguageChanged = { newLang ->
                                LocaleManager.setLocale(this@MainActivity, newLang)
                                currentLanguage = newLang
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DIYWallpaperTheme() {
        Greeting("Android")
    }
}
