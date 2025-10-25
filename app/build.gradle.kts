plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)

}

android {
    namespace = "com.example.couple_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.couple_app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Firebase BOM
    implementation(platform(libs.firebase.bom))

    // Firebase dependencies
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)

    // Firebase App Check (IMPORTANT for fixing error 17093)
    implementation("com.google.firebase:firebase-appcheck-playintegrity:19.0.1")

    // Google Play Services for Phone Auth (REQUIRED for SMS verification)
    implementation(libs.play.services.auth)
    implementation("com.google.android.gms:play-services-auth-api-phone:18.3.0")

    // Play Integrity API (replaces deprecated SafetyNet)
    implementation("com.google.android.play:integrity:1.5.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(libs.firebase.storage)

    // Lifecycle components (ViewModel & LiveData)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}