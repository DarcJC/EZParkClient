import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0-beta5"
}

group = "pro.darc.park"
version = "1.0.0"

val djlVersion = "0.14.0"
val ktorVersion = "1.6.7"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
    implementation(compose.desktop.currentOs)
    implementation("org.bytedeco", "javacv-platform", "1.5.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("ai.djl", "api", djlVersion)
    implementation("ai.djl.pytorch", "pytorch-engine", djlVersion)
    implementation("ai.djl.pytorch", "pytorch-model-zoo", djlVersion)
    implementation("ai.djl.pytorch", "pytorch-native-auto", "1.9.1")
    implementation("net.sourceforge.tess4j", "tess4j", "4.4.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "16"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "EZParkClient"
            packageVersion = "1.0.0"
        }
    }
}