import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 签名信息从 keystore.properties 读取（该文件已在 .gitignore 中，不入库）
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropsFile.exists()

android {
    namespace = "com.iteam.examproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.iteam.examproject"
        minSdk = 21
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // 有 keystore.properties 时用正式签名，否则保持未签名（便于他人克隆后构建）
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
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