plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val deliveryStore = System.getenv("MAIN_SIGNING_STORE_FILE")
val deliveryStorePassword = System.getenv("MAIN_SIGNING_STORE_PASSWORD")
val deliveryAlias = System.getenv("MAIN_SIGNING_KEY_ALIAS")
val deliveryKeyPassword = System.getenv("MAIN_SIGNING_KEY_PASSWORD")
val deliveryReady = listOf(deliveryStore, deliveryStorePassword, deliveryAlias, deliveryKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "pe.tecsup.main.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "pe.tecsup.main.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        if (deliveryReady) create("delivery") {
            storeFile = file(deliveryStore!!)
            storePassword = deliveryStorePassword
            keyAlias = deliveryAlias
            keyPassword = deliveryKeyPassword
        }
    }
    buildTypes {
        getByName("release") { if(deliveryReady) signingConfig = signingConfigs.getByName("delivery") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

tasks.matching { it.name == "packageRelease" || it.name == "packageReleaseBundle" }.configureEach {
    doFirst {
        require(deliveryReady && file(deliveryStore!!).isFile) {
            "Configura las cuatro variables MAIN_SIGNING_* y el archivo de firma antes de generar una entrega."
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
