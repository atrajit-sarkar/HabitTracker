import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Load keystore properties securely
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "it.atraj.habittracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "it.atraj.habittracker"
        minSdk = 29
        targetSdk = 36
    versionCode = 16
    versionName = "6.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add GitHub token from keystore.properties to BuildConfig
        val githubToken = keystoreProperties.getProperty("GITHUB_TOKEN") ?: ""
        buildConfigField("String", "GITHUB_TOKEN", "\"$githubToken\"")
        
        // Add Email credentials from keystore.properties to BuildConfig
        // SMTP_AUTH_EMAIL: The actual Gmail account for authentication
        val smtpAuthEmail = keystoreProperties.getProperty("SMTP_AUTH_EMAIL") ?: ""
        // EMAIL_APP_PASSWORD: App password for the Gmail account
        val emailPassword = keystoreProperties.getProperty("EMAIL_APP_PASSWORD") ?: ""
        // EMAIL_FROM_ADDRESS: Optional alias email
        val emailFromAddress = keystoreProperties.getProperty("EMAIL_FROM_ADDRESS") ?: ""
        
        buildConfigField("String", "SMTP_AUTH_EMAIL", "\"$smtpAuthEmail\"")
        buildConfigField("String", "EMAIL_APP_PASSWORD", "\"$emailPassword\"")
        buildConfigField("String", "EMAIL_FROM_ADDRESS", "\"$emailFromAddress\"")
    }

    signingConfigs {
        create("release") {
            // Load signing config from keystore.properties (not committed to git)
            if (keystoreProperties.containsKey("RELEASE_STORE_FILE")) {
                storeFile = file(keystoreProperties.getProperty("RELEASE_STORE_FILE"))
                storePassword = keystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = keystoreProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = keystoreProperties.getProperty("RELEASE_KEY_PASSWORD")
            } else {
                // Fallback for CI/CD or if keystore.properties doesn't exist
                println("⚠️ WARNING: keystore.properties not found!")
                println("ℹ️  Release signing will be disabled.")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.splashscreen)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.play.services.auth)

    // Image Loading
    implementation(libs.coil.compose)

    // WorkManager for reliable background tasks
    implementation(libs.androidx.work.runtime.ktx)
    
    // Media for background music playback
    implementation("androidx.media:media:1.7.0")
    
    // Security - Encrypted SharedPreferences (for secure token storage)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)
    
    // OkHttp for update checks and SMTP
    implementation(libs.okhttp)
    
    // Moshi for JSON parsing (dynamic music loading)
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    
    // Lottie Animations
    implementation(libs.lottie.compose)
    
    // Apache Commons Codec for Base64 encoding (for SMTP)
    implementation("commons-codec:commons-codec:1.15")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}