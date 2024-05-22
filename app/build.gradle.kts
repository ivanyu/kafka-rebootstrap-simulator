plugins {
    application
    java
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.apache.kafka", "kafka-clients", "3.8.0-SNAPSHOT")

    implementation("org.slf4j", "slf4j-api", "1.7.36")
    runtimeOnly("org.slf4j", "slf4j-log4j12", "1.7.36")
    implementation("commons-cli", "commons-cli", "1.7.0")
    implementation("org.apache.commons", "commons-math3", "3.6.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "org.apache.kafka.clients.RebootstrapSimulator"
}
