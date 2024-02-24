plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    java
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    maven("https://repo.jukeboxmc.eu/snapshots")
}

dependencies {
    api(project(":api"))
    compileOnly("org.jukeboxmc:JukeboxMC-API:1.0.0-SNAPSHOT")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("org.redisson:redisson:3.25.2") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}