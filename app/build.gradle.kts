plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Assinatura de release: só existe quando o GitHub fornece a chave (variáveis de ambiente).
// Sem a chave, o build de release sai sem assinatura e o de debug continua funcionando.
val keystorePath: String? = System.getenv("KEYSTORE_PATH")
val keystorePassword: String? = System.getenv("KEYSTORE_PASSWORD")
val hasReleaseKey = keystorePath != null && keystorePassword != null && file(keystorePath).exists()

android {
    namespace = "br.com.sauber.bloqueador"
    // Google Play exige target Android 16 (API 36) desde 31/08/2026.
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.sauber.bloqueador"
        // CallScreeningService + RoleManager (ROLE_CALL_SCREENING) exigem Android 10 (API 29)
        minSdk = 29
        targetSdk = 36
        // A Play Store exige versionCode crescente: usa o número da execução no GitHub.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.1"
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = "upload"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Sem dependências externas: a tela usa apenas o framework do Android.
