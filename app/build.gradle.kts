


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "edu.pmdm.mortahil_fatimaimdbapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.pmdm.mortahil_fatimaimdbapp"
        minSdk = 23
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




    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }


}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.auth)
    implementation(libs.activity)
    implementation(libs.firebase.firestore)
    implementation(libs.lifecycle.process)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.firebase.auth.v2130)
    implementation(libs.play.services.auth)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.json)
    implementation(libs.gbutton)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.android.facebook.android.sdk)
    implementation (libs.appcompat)
    implementation (libs.material.v180)
    implementation (libs.android.facebook.android.sdk)
    implementation (libs.lifecycle.runtime)
    implementation (libs.places)
    implementation (libs.play.services.maps)
    implementation (libs.ccp)







}

