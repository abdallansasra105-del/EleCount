plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.Abdallansasra"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fiveuntprojectbase"
        minSdk = 24
        targetSdk = 36
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    // JSON handling
    implementation("com.google.code.gson:gson:2.10.1")

    // Java SDK for Appwrite (Alternative from GitHub) - Temporarily removed
    // implementation("online.bingzi:sdk-for-java:0.8")
    
    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.activity)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}