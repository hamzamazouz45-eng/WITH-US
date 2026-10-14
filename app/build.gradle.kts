plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.isharah.overlay"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.isharah.overlay"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // مفتاح تصحيح ثابت: يجعل كل إصدار جديد يُثبَّت فوق السابق دون حذفه.
    // (مفتاح تجريبي معروف، وليس سرياً ولا يصلح لنشر التطبيق في المتجر.)
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
