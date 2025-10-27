plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.btl_mobileapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.btl_mobileapp"
        minSdk = 26
        targetSdk = 36 
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

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {

    implementation(platform(libs.firebase.bom)) // ✅ Sử dụng alias từ libs.versions.toml

    // Firebase Authentication
    implementation(libs.firebase.auth)

    // Firebase Firestore
    implementation(libs.firebase.firestore)

    // Firebase Realtime Database
    implementation(libs.firebase.database)

    // Firebase Cloud Messaging
    implementation(libs.firebase.messaging)

    // Firebase Cloud Storage - ✅ Chỉ cần dòng này (dùng alias)
    implementation(libs.firebase.storage)

    // Google Play Services for Google Sign-In
    implementation(libs.play.services.auth)

    // Circle ImageView
    implementation("de.hdodenhof:circleimageview:3.1.0") // Giữ nguyên vì không có trong toml

    implementation(libs.glide)

    // AndroidX & Material Components
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.cardview)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}