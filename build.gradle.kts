plugins {
    kotlin("jvm") version "1.9.22"
    java
}

java {
    java.sourceCompatibility = JavaVersion.VERSION_17
    java.targetCompatibility = JavaVersion.VERSION_17
}

group = "org.jukeboxmc.plugin.perms"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
}