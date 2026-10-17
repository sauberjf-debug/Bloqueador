plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Assinatura de release: só existe quando o GitHub fornece a chave (variáveis de ambiente).
// Sem a chave, o build de release sai sem assinatura e o de debug continua funcionando.
val keystorePath: String? = System.getenv("KEYSTORE_PATH")
val keystorePassword: String? = System.getenv("KEYSTORE_PASSWORD")
val hasReleaseKey = keystorePath != null && keystorePassword != null && file(keystorePath).exists()

// Senha de administrador das versões de teste. A senha vem do segredo ADMIN_PASSWORD do GitHub
// e nunca é gravada no código: só o hash PBKDF2 (com sal aleatório) vai para dentro do app.
// ADMIN_GATE=0 desliga a trava (usado no pacote da Play Store).
val adminPassword: String? = System.getenv("ADMIN_PASSWORD")?.takeIf { it.isNotBlank() }
val adminGateOn: Boolean = System.getenv("ADMIN_GATE") != "0" && adminPassword != null
val adminIterations = 120_000 // deve bater com AdminGate.kt

fun hexOf(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

val adminSaltHex: String = if (adminGateOn) {
    hexOf(ByteArray(16).also { java.security.SecureRandom().nextBytes(it) })
} else ""

val adminHashHex: String = if (adminGateOn) {
    val salt = ByteArray(adminSaltHex.length / 2) { i -> adminSaltHex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    val spec = javax.crypto.spec.PBEKeySpec(adminPassword!!.toCharArray(), salt, adminIterations, 256)
    hexOf(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded)
} else ""

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
        versionName = "1.2"

        buildConfigField("boolean", "ADMIN_GATE", adminGateOn.toString())
        buildConfigField("String", "ADMIN_SALT", "\"$adminSaltHex\"")
        buildConfigField("String", "ADMIN_HASH", "\"$adminHashHex\"")
    }

    buildFeatures {
        buildConfig = true
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
