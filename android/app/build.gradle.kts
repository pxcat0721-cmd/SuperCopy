plugins {
    id("com.android.application") // AGP 9 内置 Kotlin，无需 kotlin.android 插件
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.supercopy.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.supercopy.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // 暂用 debug 签名便于直接安装
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-navigation3-ui:0.9.3") // miuix 过渡动画 + 可预测返回
    implementation("top.yukonga.miuix.kmp:miuix-shader:0.9.3")         // 流体光 RuntimeShader
    implementation("top.yukonga.miuix.kmp:miuix-blur:0.9.3")           // 卡片 textureBlur 毛玻璃
    implementation("androidx.navigation3:navigation3-runtime:1.1.4")
    implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0") // collectAsStateWithLifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
}
