import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Release versions come from the git tag (see .github/workflows/release.yml):
//   arrowescape-v1.4.2  ->  versionName 1.4.2, versionCode 10402
val appVersionName = providers.gradleProperty("appVersionName").getOrElse("0.1.0")
val appVersionCode = providers.gradleProperty("appVersionCode").map(String::toInt).getOrElse(1)

// AdMob IDs. These defaults are Google's public TEST ids: they show test ads and never pay.
// Before publishing, set real ids in gradle.properties or pass -Parrowescape.admob.appId=... etc.
val admobAppId = providers.gradleProperty("arrowescape.admob.appId")
    .getOrElse("ca-app-pub-3940256099942544~3347511713")
val admobRewarded = providers.gradleProperty("arrowescape.admob.rewarded")
    .getOrElse("ca-app-pub-3940256099942544/5224354917")
val admobInterstitial = providers.gradleProperty("arrowescape.admob.interstitial")
    .getOrElse("ca-app-pub-3940256099942544/1033173712")

// Release signing comes from environment variables (set by CI from GitHub secrets).
val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

android {
    namespace = "com.sathishkumar.games.arrowescape"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sathishkumar.games.arrowescape"
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
            // Without a release keystore, fall back to the debug key so the APK still installs
            // for testing. Play Store uploads need the real keystore.
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
