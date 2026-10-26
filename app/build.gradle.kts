plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.paperscreen"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.paperscreen"
        minSdk = 31
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }

    buildTypes {
        release {
            // Signed with the debug key, so the release APK installs straight away when
            // sideloaded. Use your own key before publishing it anywhere.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // No libraries: framework APIs and the Kotlin stdlib only, to keep the APK tiny.
    testImplementation("junit:junit:4.13.2")
}
