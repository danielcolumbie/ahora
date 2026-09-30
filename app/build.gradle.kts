plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ahora.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ahora.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 30
        versionName = "1.27.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firma release solo si hay keystore configurado por variables de
            // entorno (nunca se sube el keystore al repo). F-Droid firma con su
            // propia clave, así que esto no le afecta.
            System.getenv("AHORA_KEYSTORE_PATH")?.let { ksPath ->
                signingConfigs.create("release") {
                    storeFile = file(ksPath)
                    storePassword = System.getenv("AHORA_KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("AHORA_KEY_ALIAS")
                    keyPassword = System.getenv("AHORA_KEY_PASSWORD")
                }
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        // El schema Room exportado (app/schemas/) queda disponible como asset
        // de los tests de instrumentación: MigrationTestHelper lo lee de ahí.
        getByName("androidTest").assets.srcDir("schemas")
    }
    // Room: exporta el schema de la BD para poder validar futuras migraciones.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    val bom = platform(libs.androidx.compose.bom)
    implementation(bom)
    androidTestImplementation(bom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    // org.json viene en Android; en JVM (tests) se usa el artefacto real.
    testImplementation(libs.org.json)

    // Solo para el scaffold de tests de migración (instrumentation).
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
