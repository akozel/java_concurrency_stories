plugins {
    id("java")
    id("me.champeau.jmh") version "0.7.2"
}

group = "by.akozel"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

jmh {
    jvmArgs.set(listOf("-Xms1g", "-Xmx8g"))
    includes.set(listOf(".*OopAndCacheMissBench.*"))
}