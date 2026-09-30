plugins {
    kotlin("jvm")
}

group = "com.goreecloud.gallery"
version = "0.1.0-dev"

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
