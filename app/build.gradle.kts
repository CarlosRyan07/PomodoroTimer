// build.gradle.kts (Module :app)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.pomodorotimer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pomodorotimer"
        minSdk = 23
        targetSdk = 35
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Dependências ESSENCIAIS para Views Tradicionais:
    implementation(libs.androidx.core.ktx) // Já está correto usando libs.versions.toml

    // CORRIGIDO: Agora todas as strings diretas estão com "()"
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Para loading images and GIFs (Glide) - Corrigido, usa libs.versions.toml
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Testes - CORRIGIDO: Agora todas as strings diretas estão com "()"
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}