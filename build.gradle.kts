plugins {
    kotlin("jvm") version "1.3.71"
    java
    id("com.github.johnrengelman.shadow") version "5.2.0"
}


group = "mamoe.net"
version = "1.0.0"

apply(plugin = "com.github.johnrengelman.shadow")

fun ktor(id: String, version: String = "1.3.1") = "io.ktor:ktor-$id:$version"

fun DependencyHandlerScope.kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url = "https://mirrors.huaweicloud.com/repository/maven")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jsoup:jsoup:1.12.1")
    implementation(ktor("server-cio"))
    implementation(ktor("client-cio"))
    implementation(ktor("client-core"))
    implementation(ktor("http-jvm"))
    compile(ktor("client-auth"))
    implementation("com.google.code.gson:gson:2.8.6")

    implementation(kotlinx("coroutines-core", "1.3.5"))
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}