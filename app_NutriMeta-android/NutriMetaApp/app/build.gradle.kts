import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    //para firebase
    id("com.google.gms.google-services")
}

// URL del backend en desarrollo. Se define en local.properties (no se sube a git):
//   api.baseUrl=http://192.168.1.50:8080/
// Si no existe, usa 10.0.2.2, que es la PC vista desde el emulador.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) {
        archivo.inputStream().use { load(it) }
    }
}
val apiBaseUrl = propiedadesLocales.getProperty("api.baseUrl") ?: "http://10.0.2.2:8080/"

android {
    namespace = "com.app.nutrimeta"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.app.nutrimeta"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }


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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }


    buildFeatures{
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //nuevas dependencias :)

    // Google sincronizacion
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    //correo
    implementation("com.google.firebase:firebase-auth:23.0.0")

    // Consumo de la API REST (sincronizacion)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}