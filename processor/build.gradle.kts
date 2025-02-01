plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(projects.api)

    implementation(libs.symbol.processing.api)
    implementation(libs.symbol.processing)

    implementation(libs.kotlinPoet)
    implementation(libs.kotlinPoet.ksp)
}
