import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.example.wififiletransfer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.solardevtech.wififiletransfer"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/*.kotlin_module",
                "META-INF/io.netty.versions.properties",
                "META-INF/INDEX.LIST"
            )
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures { viewBinding=true }


    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
   // implementation (libs.androidasync)
    implementation(libs.androidx.constraintlayout)
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation ("androidx.databinding:databinding-runtime:8.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation ("io.ktor:ktor-server-core:2.2.3")
    implementation ("io.ktor:ktor-server-cio:2.2.3")
    implementation ("io.ktor:ktor-server-sessions:2.2.3")
    implementation ("io.ktor:ktor-server-auth:2.2.3")
    implementation ("io.ktor:ktor-server-auth-jwt:2.2.3")
    implementation ("io.ktor:ktor-server-content-negotiation:2.2.3")
    implementation ("io.ktor:ktor-serialization-kotlinx-json:2.2.3")



    implementation ("io.ktor:ktor-client-core:2.2.3")
    implementation ("io.ktor:ktor-client-cio:2.2.3")

}