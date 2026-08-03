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
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
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
    // Firebase BOM - manages all Firebase versions
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    debugImplementation("com.google.firebase:firebase-appcheck-debug")

    // Braintree PayPal SDK
    implementation("com.braintreepayments.api:braintree-core:4.51.0") {
        exclude(group = "org.jfrog.cardinalcommerce.gradle", module = "cardinalmobilesdk")
    }
    implementation("com.braintreepayments.api:paypal:4.51.0") {
        exclude(group = "org.jfrog.cardinalcommerce.gradle", module = "cardinalmobilesdk")
    }
    implementation("com.braintreepayments.api:card:4.51.0") {
        exclude(group = "org.jfrog.cardinalcommerce.gradle", module = "cardinalmobilesdk")
    }
    implementation("com.braintreepayments.api:google-pay:4.51.0") {
        exclude(group = "org.jfrog.cardinalcommerce.gradle", module = "cardinalmobilesdk")
    }
    // Also provides the official Google Pay button (com.google.android.gms.wallet.button)
    implementation("com.google.android.gms:play-services-wallet:19.4.0")

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Google Sign-In
    implementation(libs.googleid)
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Other libraries
    implementation("io.github.chaosleung:pinview:1.4.4")
    implementation("com.github.1902shubh:SendMail:1.0.0")
    implementation("com.airbnb.android:lottie:6.6.6")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.qamarelsafadi:CurvedBottomNavigation:0.1.3")
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.2")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Test dependencies
    implementation(libs.litert)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("com.github.LottieFiles:dotlottie-android:0.5.0")
}

apply(plugin = "com.google.gms.google-services")