import java.util.Properties

plugins {
    id("com.android.application") // AGP 9 内置 Kotlin，无需 kotlin.android 插件
    id("org.jetbrains.kotlin.plugin.compose")
}

// 正式签名信息存于 android/keystore.properties（已 gitignore，勿提交）
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
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

    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 有 keystore.properties 用正式签名，否则回退 debug 签名（CI/他人克隆仍可构建）
            signingConfig = if (keystoreProps.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
