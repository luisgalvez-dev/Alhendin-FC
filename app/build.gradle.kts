import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

fun escapeBuildConfig(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.luis.alhendinfc"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.luis.alhendinfc"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"\"")
        buildConfigField("String", "SUPABASE_BUCKET", "\"alhendin-files\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("stable") {
            dimension = "environment"
            isDefault = true
            applicationId = "com.luis.alhendinfc"
        }
        create("dev") {
            dimension = "environment"
            applicationId = "com.luis.alhendinfc.dev"
            val local = rootProject.file("supabase.local.properties")
            val props = Properties()
            if (local.exists()) {
                local.inputStream().use { props.load(it) }
            }
            val url = props.getProperty("SUPABASE_URL", "").trim()
            val key = props.getProperty("SUPABASE_PUBLISHABLE_KEY", "").trim()
            require(!key.contains("service_role", ignoreCase = true)) {
                "supabase.local.properties no puede contener service_role"
            }
            buildConfigField("String", "SUPABASE_URL", "\"${escapeBuildConfig(url)}\"")
            buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${escapeBuildConfig(key)}\"")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.lifecycle.process)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Alias de testStableDebugUnitTest (flavor por defecto)."
    dependsOn("testStableDebugUnitTest")
}

// JSON oficial por flavor: app/src/dev/google-services.json (com.luis.alhendinfc.dev).
// Stable no tiene cliente Firebase todavía: no procesar Google Services en esos variants.
tasks.configureEach {
    if (name.startsWith("process") && name.endsWith("GoogleServices") && !name.contains("Dev")) {
        enabled = false
    }
}
