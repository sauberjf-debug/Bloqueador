plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.com.sauber.bloqueador"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.sauber.bloqueador"
        // CallScreeningService + RoleManager (ROLE_CALL_SCREENING) exigem Android 10 (API 29)
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// Sem dependências externas: a tela usa apenas o framework do Android.
