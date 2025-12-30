import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY") ?: ""


android {
    namespace = "com.example.asystent_ekologiczny"
    compileSdk = 36  // Usuń "version = release(36)"

    defaultConfig {
        applicationId = "com.example.asystent_ekologiczny"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        resValue("string", "google_maps_key", mapsApiKey)

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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.camera.mlkit.vision)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // CameraX - w Kotlin DSL też używaj nawiasów
    implementation("androidx.camera:camera-core:1.5.1")
    implementation("androidx.camera:camera-camera2:1.5.1")
    implementation("androidx.camera:camera-lifecycle:1.5.1")
    implementation("androidx.camera:camera-view:1.5.1")

    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // ViewModel i LiveData
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")

    // Google Play Services Maps
    implementation(libs.play.services.maps)

    // MPAndroidChart - Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // JSON - Gson do parsowania pliku edukacyjnego
    implementation("com.google.code.gson:gson:2.10.1")

    // ExoPlayer do odtwarzania wideo
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
}
