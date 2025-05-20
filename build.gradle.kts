
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript{
    dependencies {
        classpath ("com.google.gms:google-services:4.4.2")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.9")
    }



}

//buildscript {
//    repositories {
//        google()
//        mavenCentral()
//    }
//    dependencies {
//        classpath ("com.android.tools.build:gradle:7.0.4") // Use your current version
//        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10") // Use your current version
//
//        // Add Safe Args plugin classpath
//        classpath ("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7") // Use latest stable version
//    }
//}


plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}