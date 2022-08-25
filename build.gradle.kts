import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    application
}
application {
    mainClass.set("project.MainKt")
}
group = "ir.amirab"
version = "0.1"

repositories {
    mavenCentral()
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
    implementation(libs.kotlin.reflect)

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)

    implementation(libs.arrow.core)

    implementation(libs.gson)
}

sourceSets.main {
    java.srcDirs("build/generated/ksp/main/kotlin")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}