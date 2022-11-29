import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "com.kraskaska.minecraft.shinytech"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        name = "Ktor EAP"
    }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation("io.ktor:ktor-server:2.2.0-eap-555")
    implementation("io.ktor:ktor-server-netty:2.2.0-eap-555")
    implementation("io.ktor:ktor-server-status-pages:2.2.0-eap-555")
    implementation("org.slf4j:slf4j-reload4j:2.0.4")
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("io.ktor:ktor-server-auth:2.2.0-eap-559")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.14.0")
    implementation("io.ktor:ktor-client-core:2.2.0-eap-557")
    implementation("io.ktor:ktor-client-cio:2.2.0-eap-557")
    implementation("io.ktor:ktor-serialization-jackson:2.2.0-eap-559")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.0-eap-557")
    implementation("io.ktor:ktor-server-sessions:2.2.0-eap-559")
    implementation("com.soywiz.korlibs.klock:klock:3.4.0")
}



tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}