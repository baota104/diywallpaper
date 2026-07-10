plugins {
    id("com.android.application") version "8.9.1" apply false
    id("com.google.dagger.hilt.android") version "2.58" apply false
    id ("com.google.gms.google-services") version "4.4.1" apply false
    id ("com.google.firebase.crashlytics") version "2.9.9" apply false
    id ("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.kapt") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0" apply false
    // 2. Compose Plugin (Bắt buộc trùng version với Kotlin)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false

    // 3. KSP (Tương thích với Kotlin 2.2.0)
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
}
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
