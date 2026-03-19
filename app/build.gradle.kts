import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.example.tobisoappnative"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tobiso.tobisoapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "2.0"

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
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            
            // Debug build config fields
            buildConfigField("boolean", "IS_PRODUCTION", "false")
            buildConfigField("String", "API_BASE_URL", "\"https://www.tobiso.com/api/\"")
            buildConfigField("String", "API_USERNAME", "\"${localProperties["API_USERNAME"] ?: ""}\"")
            buildConfigField("String", "API_PASSWORD", "\"${localProperties["API_PASSWORD"] ?: ""}\"")
            buildConfigField("String", "CERT_FINGERPRINT", "\"\"")
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
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.activity:activity:1.6.0-alpha05")
    
    // API dependencies
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.halilibo.compose-richtext:richtext-ui-material3:1.0.0-alpha03")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:1.0.0-alpha03")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")
    implementation("com.google.android.gms:play-services-oss-licenses:17.0.1")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.8.1")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.33.2-alpha")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation(libs.androidx.compose.foundation)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}