import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Firma fija (secreto de GitHub) para que cada versión se instale encima de la anterior.
val keystorePath: String? = System.getenv("KYRO_KEYSTORE")?.takeIf { File(it).exists() }

android {
    namespace = "com.kyro.avimp4"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kyro.avimp4"
        minSdk = 24
        targetSdk = 35
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "2.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    signingConfigs {
        if (keystorePath != null) {
            create("kyro") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KYRO_STORE_PASSWORD")
                keyAlias = System.getenv("KYRO_KEY_ALIAS")
                keyPassword = System.getenv("KYRO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) signingConfigs.getByName("kyro") else signingConfigs.getByName("debug")
        }
    }

    // La interfaz es la misma carpeta web/ que se publica en GitHub Pages.
    sourceSets["main"].assets.srcDirs("../web")

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.webkit:webkit:1.11.0")
    // FFmpeg nativo: lee AVI, arma la vista previa completa y convierte rápido en el celular
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-min:8.1.7")
    implementation("com.arthenica:smart-exception-java:0.2.1")
    // Android pasa el video a H.264 con su propio codificador (el que el WebView sí muestra)
    implementation("androidx.media3:media3-transformer:1.4.1")
    implementation("androidx.media3:media3-effect:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
}
