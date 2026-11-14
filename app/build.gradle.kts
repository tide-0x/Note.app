plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.iteam.examproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.iteam.examproject"
        minSdk = 21
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

    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")  // ViewModel
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2") // LiveData（可选）
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")   // Lifecycle（可选）
    implementation ("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.6.2") // SavedState

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}