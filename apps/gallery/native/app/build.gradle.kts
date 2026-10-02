plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val goreeCloudGalleryDevelopmentVersionCode =
    System.getenv("GOREECLOUD_GALLERY_DEV_VERSION_CODE")?.toIntOrNull() ?: 19

android {
    namespace = "com.goreecloud.gallery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.goreecloud.gallery"
        minSdk = 29
        targetSdk = 36
        versionCode = goreeCloudGalleryDevelopmentVersionCode
        versionName = "0.8.7-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":android-adapter"))

    testImplementation(kotlin("test"))

    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
