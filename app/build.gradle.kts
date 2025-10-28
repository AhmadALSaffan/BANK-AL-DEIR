plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "bankal_deir.com"
    compileSdk = 36

    defaultConfig {
        applicationId = "bankal_deir.com"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures{
        viewBinding = true
        dataBinding = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
    implementation(libs.firebase.storage)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation("com.github.1902shubh:SendMail:1.0.0")
    implementation("com.airbnb.android:lottie:6.6.6")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.qamarelsafadi:CurvedBottomNavigation:0.1.3")
    implementation("com.google.android.material:material:1.1.0-alpha08")
    implementation("com.google.android.material:material:1.6.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.firebase:firebase-storage:22.0.1")
    implementation("com.github.bumptech.glide:glide:5.0.5")
}
apply(plugin = "com.google.gms.google-services")