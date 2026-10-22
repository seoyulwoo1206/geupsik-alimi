import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// API 키 같은 비밀값은 local.properties(깃에 올라가지 않음)에서 읽는다.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProperty(name: String): String =
    localProperties.getProperty(name)?.trim().orEmpty()

// 릴리스 서명 키 정보. keystore.properties는 깃에 올라가지 않는다 (README 참고).
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseKey = keystoreProperties.getProperty("storeFile") != null

// 업데이트 확인에 쓰는 깃허브 저장소 ("아이디/저장소이름"). gradle.properties에서 정한다.
val githubRepo: String = (findProperty("githubRepo") as String?)?.trim().orEmpty()

android {
    namespace = "com.geupsik.alimi"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.geupsik.alimi"
        minSdk = 26
        targetSdk = 36
        // 새 버전을 낼 때마다 versionCode는 1씩 올리고, versionName은 깃허브 릴리스 태그(v0.2.0)와 맞춘다
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "NEIS_API_KEY", "\"${localProperty("NEIS_API_KEY")}\"")
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    // 배포처별 빌드.
    // github: 깃허브 릴리스로 APK 배포. 앱을 켤 때 새 버전을 확인한다.
    // play:   플레이스토어용. 스토어 밖 업데이트 안내는 정책 위반이라 업데이트 확인을 뺀다.
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "true")
            buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "UPDATE_CHECK_ENABLED", "false")
            buildConfigField("String", "GITHUB_REPO", "\"\"")
        }
    }

    buildTypes {
        release {
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    // DB 구조를 파일로 남겨 두면 나중에 업데이트할 때 마이그레이션을 확인할 수 있다
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
