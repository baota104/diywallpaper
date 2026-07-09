Markdown
# 🌐 System Skill: Seamless In-App Language Switcher (Jetpack Compose)

**Target Audience:** AI Agents / Senior Android Developers.
**Objective:** Implement a robust, dynamic, and flicker-free language switching mechanism in Android Jetpack Compose without recreating the Activity.
**Architecture Context:** Clean Architecture, MVVM, 100% Jetpack Compose.

---

## 🧠 1. Core Principles (Nguyên lý cốt lõi)

To change the language in a modern Android Compose app flawlessly, the AI Agent MUST implement a **Two-Tier Update System**:
1.  **System Level (Context Wrapper):** Intercept and wrap the base context (`attachBaseContext`) in both `Application` and `MainActivity`. This ensures system-level calls (like traditional XML, Intents, Services) get the correct localized strings.
2.  **Compose Level (CompositionLocal):** Inject a custom localized `Configuration` via `CompositionLocalProvider`. By binding this to a mutable state (`currentLanguage`), changing the language forces an immediate, seamless UI recomposition without `Activity.recreate()`.

---

## 🛠 2. Implementation Steps (Các bước triển khai)

### Step 1: The `LocaleManager` (Lớp Quản lý cốt lõi)
**Agent Instruction:** Create a singleton/object to handle SharedPreferences and Context wrapping. Do NOT use deprecated `updateConfiguration`.

```kotlin
object LocaleManager {
    private const val PREF_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    fun saveLocale(context: Context, languageCode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getLocale(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en") ?: "en"
    }

    // VITAL: Wraps the context with the new language configuration
    fun wrapContext(context: Context): Context {
        val languageCode = getLocale(context)
        val locale = java.util.Locale.forLanguageTag(languageCode)
        java.util.Locale.setDefault(locale)
        
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    // VITAL: Creates a localized configuration for Compose LocalConfiguration
    fun createConfiguration(baseConfig: android.content.res.Configuration, languageCode: String): android.content.res.Configuration {
        val locale = java.util.Locale.forLanguageTag(languageCode)
        return android.content.res.Configuration(baseConfig).apply {
            setLocale(locale)
        }
    }
}
Step 2: System-Level Interception (Bọc Context)
Agent Instruction: Override attachBaseContext in BOTH the custom Application class and the MainActivity.

In App.kt (Application Class):

Kotlin
override fun attachBaseContext(base: Context) {
    super.attachBaseContext(LocaleManager.wrapContext(base))
}
In MainActivity.kt:

Kotlin
override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(LocaleManager.wrapContext(newBase))
}
Step 3: Compose-Level Injection (Tiêm ngôn ngữ vào UI)
Agent Instruction: Inside MainActivity.onCreate's setContent, wrap the main app theme with CompositionLocalProvider and bind it to a mutableStateOf.

In MainActivity.kt:

Kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        // 1. State holding current language
        var currentLanguage by remember { mutableStateOf(LocaleManager.getLocale(this)) }
        
        // 2. Reactively derive a localized Configuration
        val localizedConfiguration = remember(currentLanguage) {
            LocaleManager.createConfiguration(resources.configuration, currentLanguage)
        }

        // 3. Inject it down the Compose tree
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalConfiguration provides localizedConfiguration
        ) {
            MyAppTheme {
                AppNavGraph(
                    onLanguageChanged = { newLangCode ->
                        // The Action Flow
                        LocaleManager.saveLocale(this, newLangCode)
                        currentLanguage = newLangCode // Triggers immediate recomposition!
                    }
                )
            }
        }
    }
}
⚡ 3. AI Agent Directives (Quy tắc cho AI khi tái sử dụng)
When tasked to "add language support" or "fix language switching" in a new Compose project, the AI MUST strictly follow these rules:

NEVER use Activity.recreate(): It causes a black screen flash and poor UX. Use the CompositionLocalProvider state-driven approach instead.

String Resources Only: Ensure all UI text is accessed via stringResource(R.string.key). Never hardcode strings.

ViewModel Independence: ViewModels should NEVER hold UI strings or contexts. If a ViewModel needs a string (e.g., for error messages), it should return an @StringRes Int resource ID, and the Compose UI layer will resolve it using stringResource().

Language Code Standard: Use standard ISO 639-1 language codes (e.g., "en", "vi", "es").