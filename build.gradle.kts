plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.10.6"
    // Bytecode enhancement em build-time: lazy associations sem HibernateProxy
    // em runtime (compatível com BytecodeProvider=none do native image).
    id("org.hibernate.orm") version "7.4.1.Final"
}

group = "br.com.adaneinstein"
version = project.findProperty("appVersion")?.toString() ?: "0.0.1-SNAPSHOT"
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
    // Suporte de metadata/hints do Hibernate para GraalVM native image
    runtimeOnly("org.hibernate.orm:hibernate-graalvm")
    runtimeOnly("org.xerial:sqlite-jdbc:3.50.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Anotações @TargetClass/@Substitute para substitution do native image (só compile)
    compileOnly("org.graalvm.nativeimage:svm:25.0.2")
    // Source: https://mvnrepository.com/artifact/org.projectlombok/lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")
    // Source: https://mvnrepository.com/artifact/com.googlecode.lanterna/lanterna
    implementation("com.googlecode.lanterna:lanterna:3.1.5")
}

val jvmNativeArgs = listOf("--enable-native-access=ALL-UNNAMED")

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(jvmNativeArgs)
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs(jvmNativeArgs)
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("wheresmymoney")
            buildArgs.addAll(
                "--enable-native-access=ALL-UNNAMED",
                "-H:+ReportExceptionStackTraces"
            )
        }
    }
}

tasks.processResources {
    filesMatching("version.properties") {
        expand("appVersion" to project.version)
    }
}

// Enhancement estático nas entidades (lazy + dirty tracking, defaults true) no compileJava.
hibernate {
    enhancement {}
}
