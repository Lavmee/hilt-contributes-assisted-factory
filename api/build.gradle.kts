plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.mavenPublish)
}

dependencies {
    implementation(libs.hilt.core)
}

kotlin {
    explicitApi()
}
