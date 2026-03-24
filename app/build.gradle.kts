import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt")
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.tobiso.tobisoappnative"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tobiso.tobisoapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "2.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("app/keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(keystorePropertiesFile.inputStream())
                
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Produkční bezpečnostní nastavení
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            
            // Přidání build config fields pro rozlišení prostředí
            buildConfigField("boolean", "IS_PRODUCTION", "true")
            buildConfigField("String", "API_BASE_URL", "\"https://www.tobiso.com/api/\"")
            buildConfigField("String", "API_USERNAME", "\"${localProperties["API_USERNAME"] ?: ""}\"")
            buildConfigField("String", "API_PASSWORD", "\"${localProperties["API_PASSWORD"] ?: ""}\"")
            buildConfigField("String", "CERT_FINGERPRINT", "\"${localProperties["CERT_FINGERPRINT"] ?: ""}\"")
            buildConfigField("String", "SECURITY_TOKEN_SECRET", "\"${localProperties["SECURITY_TOKEN_SECRET"] ?: ""}\"")
            // Comma-separated certificate pins (sha256/...) to allow rotation without code changes.
            buildConfigField("String", "CERT_PINS", "\"sha256/i0rpPYzV8YE/KbZ7yWnCBqTdW5LcUhWRXomSrxWFkEU=,sha256/r/tLBf9qkHs3KP7qtA2tjoDCw4GSKnyoxjEycJRblyg=\"")
            // Optional comma-separated backup pins (CA pins or extra pins) to act as failover during rotation.
            buildConfigField("String", "CERT_PINS_BACKUP", "\"sha256/9rmBackupCAExampleBase64==\"")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            
            // Debug build config fields
            buildConfigField("boolean", "IS_PRODUCTION", "false")
            buildConfigField("String", "API_BASE_URL", "\"https://www.tobiso.com/api/\"")
            buildConfigField("String", "API_USERNAME", "\"${localProperties["API_USERNAME"] ?: ""}\"")
            buildConfigField("String", "API_PASSWORD", "\"${localProperties["API_PASSWORD"] ?: ""}\"")
            buildConfigField("String", "CERT_FINGERPRINT", "\"\"")
            buildConfigField("String", "SECURITY_TOKEN_SECRET", "\"${localProperties["SECURITY_TOKEN_SECRET"] ?: ""}\"")
            // Debug builds do not pin by default to ease local development.
            buildConfigField("String", "CERT_PINS", "\"\"")
            // Debug: no backup pins by default
            buildConfigField("String", "CERT_PINS_BACKUP", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    lint {
        disable += listOf("AutoboxingStateCreation", "MutableCollectionMutableState")
        abortOnError = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.foundation)
    
    // API dependencies
    implementation(libs.retrofit)
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
    implementation(libs.retrofit.coroutines.adapter)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.richtext.ui.material3)
    implementation(libs.richtext.commonmark)
    implementation(libs.play.services.oss.licenses)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    // Needed for kotlinx.serialization runtime lookups used by retrofit serializer
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.10")
    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Logging
    implementation(libs.timber)
}