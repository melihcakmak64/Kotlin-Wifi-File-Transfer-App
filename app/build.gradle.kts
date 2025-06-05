plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.wififiletransfer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wififiletransfer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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