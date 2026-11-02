import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val appVersionName = providers.gradleProperty("appVersionName").getOrElse("0.1.0")
val appVersionCode = providers.gradleProperty("appVersionCode").map(String::toInt).getOrElse(1)

// Google's public TEST AdMob ids. Replace before publishing (see README).
val admobAppId = providers.gradleProperty("__GAME_ID__.admob.appId")
    .getOrElse("ca-app-pub-3940256099942544~3347511713")
val admobRewarded = providers.gradleProperty("__GAME_ID__.admob.rewarded")
    .getOrElse("ca-app-pub-3940256099942544/5224354917")
val admobInterstitial = providers.gradleProperty("__GAME_ID__.admob.interstitial")
    .getOrElse("ca-app-pub-3940256099942544/1033173712")

val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

android {
    namespace = "com.sathishkumar.games.__GAME_ID__"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sathishkumar.games.__GAME_ID__"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_REWARDED", "\"$admobRewarded\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL", "\"$admobInterstitial\"")
    }

    buildFeatures { buildConfig = true }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
}
