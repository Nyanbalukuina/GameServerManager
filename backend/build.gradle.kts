plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.nyanbalukuina"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty(
        "steamcmd.external.test",
        providers.gradleProperty("steamcmdExternalTest").getOrElse("false"),
    )
    systemProperty(
        "palworld.external.test",
        providers.gradleProperty("palworldExternalTest").getOrElse("false"),
    )
}

val frontendBuild by tasks.registering(Exec::class) {
    workingDir(rootProject.file("../frontend"))
    commandLine("npm.cmd", "run", "build")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    dependsOn(frontendBuild)
    from(rootProject.file("../frontend/dist")) {
        into("BOOT-INF/classes/static")
    }
}

tasks.register<Sync>("windowsDistribution") {
    dependsOn(tasks.bootJar)
    into(layout.buildDirectory.dir("distributions/windows"))
    from(tasks.bootJar.flatMap { it.archiveFile }) {
        rename { "game-server-manager.jar" }
    }
    from(rootProject.file("../windows/GameServerManager.WindowsSetup.ps1"))
}
