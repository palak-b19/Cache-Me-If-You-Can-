plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ussdemoproject"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ussdemoproject"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // -----------------------------------------
    //  ENABLE R8 (SHRINK + OBFUSCATION) SAFELY
    // -----------------------------------------
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true        // shrink + obfuscate
            isShrinkResources = true      // remove unused resources

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            isMinifyEnabled = false       // keep debug readable
            isShrinkResources = false
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.23.2")

    val genAiAar = file("libs/onnxruntime-genai-android-0.11.0.aar")
    if (genAiAar.exists()) {
        implementation(files(genAiAar))
    } else {
        logger.warn("onnxruntime-genai AAR missing; run scripts/download_tinyllama_assets.ps1 to enable LLM mode.")
    }

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
