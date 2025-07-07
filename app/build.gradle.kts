plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.riprophonic"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.riprophonic"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.core:core-ktx:1.12.0")

    // ExoPlayer Core & MediaSession Extension
    implementation("com.google.android.exoplayer:exoplayer:2.19.0")
    implementation("com.google.android.exoplayer:extension-mediasession:2.19.1")

    // Glide for Image Loading
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // Palette (Optional for color extraction)
    implementation("androidx.palette:palette:1.0.0")

    // Waveform SeekBar
    implementation("com.github.alexei-frolo:WaveformSeekBar:1.1")

    // Glide Transformations (Blur, etc.)
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
