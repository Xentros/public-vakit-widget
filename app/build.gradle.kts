plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.xentros.vakitwidget"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xentros.vakitwidget"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.0.4"
    }

    signingConfigs {
        create("release") {
            // Priority: env vars > ~/.gradle/gradle.properties (findProperty) > local keystore.properties
            val storeFileProp = System.getenv("VAKIT_STORE_FILE")
                ?: (findProperty("vakit.storeFile") as? String)
                ?: keystoreProperties.getProperty("storeFile")
            val storePasswordProp = System.getenv("VAKIT_STORE_PASSWORD")
                ?: (findProperty("vakit.storePassword") as? String)
                ?: keystoreProperties.getProperty("storePassword")
            val keyAliasProp = System.getenv("VAKIT_KEY_ALIAS")
                ?: (findProperty("vakit.keyAlias") as? String)
                ?: keystoreProperties.getProperty("keyAlias")
            val keyPasswordProp = System.getenv("VAKIT_KEY_PASSWORD")
                ?: (findProperty("vakit.keyPassword") as? String)
                ?: keystoreProperties.getProperty("keyPassword")
            if (storeFileProp != null && storePasswordProp != null) {
                storeFile = rootProject.file(storeFileProp)
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val hasSigning = System.getenv("VAKIT_STORE_FILE") != null
                || findProperty("vakit.storeFile") != null
                || keystorePropertiesFile.exists()
            signingConfig = if (hasSigning) signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.datastore.preferences)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
}
