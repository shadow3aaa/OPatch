import java.io.ByteArrayOutputStream
import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
    autowire(libs.plugins.android.application)
    autowire(libs.plugins.kotlin.android)
    autowire(libs.plugins.kotlin.ksp)
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    id("com.autonomousapps.dependency-analysis") version "2.1.4"
}

fun getAndIncrementBuildNumber(): Int {
    val propertiesFile = file("version.properties")
    val properties = Properties()
    // Load existing properties
    if (propertiesFile.exists()) {
        properties.load(FileInputStream(propertiesFile))
    } else {
        // If file doesn't exist, create a new one
        properties["BUILD_NUMBER"] = "1"
    }
    // Get the current build number
    val buildNumber = properties["BUILD_NUMBER"].toString().toInt()
    // Increment the build number
    properties["BUILD_NUMBER"] = (buildNumber + 1).toString()
    // Save the updated build number back to the properties file
    properties.store(FileOutputStream(propertiesFile), null)
    return buildNumber
}

fun getGitCommitHash(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = "git rev-parse --short HEAD".split(" ")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

fun commitCount(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = "git rev-list --count HEAD".split(" ")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

android {
    namespace = property.project.app.packageName
    compileSdk = 35

    lint {
        baseline = file("lint-baseline.xml")
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val name = "OShin"
                var abi = output.getFilter(com.android.build.OutputFile.ABI)
                if (abi == null) abi = "all" //兼容
                val version = variant.versionName
                val versionCode = variant.versionCode
                val outputFileName = "${name}_${abi}_${"v"}${version}(${versionCode}).apk"
                output.outputFileName = outputFileName
            }
    }
    val number = commitCount().toInt()// + getAndIncrementBuildNumber()
    defaultConfig {
        applicationId = property.project.app.packageName
        minSdk = property.project.android.minSdk
        targetSdk = property.project.android.targetSdk
        versionName = property.project.app.versionName+".b"+number+"."+getGitCommitHash()
        versionCode = number
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    val keystoreFile = System.getenv("KEYSTORE_PATH")
    signingConfigs {
        if (keystoreFile != null) {
            create("ci") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
                enableV4Signing = true
            }
        } else {
            create("release") {
                enableV4Signing = true
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName(if (keystoreFile != null) "ci" else "release")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            isCrunchPngs = true
            multiDexEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    lint { checkReleaseBuilds = false }
    ndkVersion = "27.0.11718014 rc1"
    buildToolsVersion = "35.0.0"
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        noCompress("webp")
    }
    // TODO Please visit https://highcapable.github.io/YukiHookAPI/en/api/special-features/host-inject
    // TODO 请参考 https://highcapable.github.io/YukiHookAPI/zh-cn/api/special-features/host-inject
    // androidResources.additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x64")
}

dependencies {
    implementation(libs.lottie.compose)
    implementation(libs.ezxhelper)
    runtimeOnly(libs.androidx.room.runtime)
    implementation(libs.androidx.palette.ktx)
    annotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
    implementation(libs.haze)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.androidx.datastore.preferences.core.jvm)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.miuix)
    implementation(libs.gson)
    implementation(libs.compose.shimmer)
    implementation(libs.expandablebottombar)
    implementation(libs.composeneumorphism)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx.v282)
    implementation(libs.androidx.activity.compose.v190)
    implementation(platform(libs.androidx.compose.bom.v20240600))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    compileOnly(de.robv.android.xposed.api)
    implementation(com.highcapable.yukihookapi.api)
    ksp(com.highcapable.yukihookapi.ksp.xposed)
    implementation(com.github.duanhong169.drawabletoolbox)
    implementation(androidx.core.core.ktx)
    implementation(androidx.appcompat.appcompat)
    implementation(com.google.android.material.material)
    implementation(androidx.constraintlayout.constraintlayout)
    testImplementation(junit.junit)
    androidTestImplementation(androidx.test.ext.junit)
    androidTestImplementation(androidx.test.espresso.espresso.core)
}
