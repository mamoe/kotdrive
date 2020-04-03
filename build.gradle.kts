import java.util.*

buildscript {
    repositories {
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://mirrors.huaweicloud.com/repository/maven")
        jcenter()
        mavenCentral()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:5.2.0")
        classpath("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4") // don"t use any other.
    }
}

plugins {
    kotlin("jvm") version "1.3.71"
    java
    id("com.github.johnrengelman.shadow") version "5.2.0"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
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

description = "OneDrive upload API in kotlin"

bintray {
    val keyProps = Properties()
    val keyFile = file("keys.properties")
    if (keyFile.exists()) keyFile.inputStream().use { keyProps.load(it) }
    if (keyFile.exists()) keyFile.inputStream().use { keyProps.load(it) }

    user = keyProps.getProperty("bintrayUser")
    key = keyProps.getProperty("bintrayKey")
    setPublications("mavenJava")
    setConfigurations("archives")

    pkg.apply {
        userOrg = "mamoe"
        repo = "kotdrive"
        name = "kotdrive"
        setLicenses("GPLv3")
        publicDownloadNumbers = true
        vcsUrl = "https://github.com/mamoe/kotdrive"
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

val publishVersion = "1.0.0"

publishing {
    /*
    repositories {
        maven {
            // change to point to your repo, e.g. http://my.org/repo
            url = uri("$buildDir/repo")
        }
    }*/
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])

            groupId = "mamoe.net"
            artifactId = "kotdrive"
            version = publishVersion

            pom.withXml {
                val root = asNode()
                root.appendNode("description", description)
                root.appendNode("name", "kotdrive")
                root.appendNode("url", "https://github.com/mamoe/kotdrive")
                root.children().last()
            }

            artifact(sourcesJar.get())
        }
    }
}