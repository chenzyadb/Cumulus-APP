plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// noinspection GradleDependency
android {
    compileSdk = 34

    // noinspection OldTargetApi
    defaultConfig {
        applicationId = "cumulus.battery.stats"
        minSdk = 23
        targetSdk = 34
        versionCode = 100070
        versionName = "1.0.7"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes.add("META-INF/**")
            excludes.add("okhttp3/**")
            excludes.add("schema/**")
            excludes.add("assets/dexopt/**")
            excludes.add("DebugProbesKt.bin")
            excludes.add("kotlin-tooling-metadata.json")
            excludes.add("**/*.kotlin_builtins")
            excludes.add("**/*.kotlin_module")
            excludes.add("**/*.properties")
            excludes.add("**/*.txt")
        }
    }

    namespace = "cumulus.battery.stats"
    buildToolsVersion = "34.0.0"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.material)
}
