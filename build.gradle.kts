plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "br.com.adaneinstein"
version = "0.0.1-SNAPSHOT"
description = "WheresMyMoney"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Jackson para falar com o Ollama (JSON) sem trazer spring-web
    implementation("org.springframework.boot:spring-boot-starter-json")
    // Dialeto SQLite mantido pela comunidade Hibernate
    implementation("org.hibernate.orm:hibernate-community-dialects")
    runtimeOnly("org.xerial:sqlite-jdbc:3.50.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Source: https://mvnrepository.com/artifact/org.projectlombok/lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")
    // Source: https://mvnrepository.com/artifact/com.googlecode.lanterna/lanterna
    implementation("com.googlecode.lanterna:lanterna:3.1.5")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
