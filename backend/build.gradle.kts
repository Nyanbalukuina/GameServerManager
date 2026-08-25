plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.nyanbalukuina"
version = "0.0.1"

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

val windowsPublishDirectory = rootProject.file("../windows/publish")
val windowsPackageInputDirectory = layout.buildDirectory.dir("jpackage/windows")
val java21Launcher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
}

val cleanWindowsDistribution by tasks.registering(Delete::class) {
    delete(windowsPublishDirectory, windowsPackageInputDirectory)
}

val stageWindowsDistribution by tasks.registering(Sync::class) {
    dependsOn(cleanWindowsDistribution, tasks.bootJar)
    into(windowsPackageInputDirectory)
    from(tasks.bootJar.flatMap { it.archiveFile }) {
        rename { "game-server-manager.jar" }
    }
}

tasks.register<Exec>("windowsDistribution") {
    dependsOn(stageWindowsDistribution)
    outputs.dir(windowsPublishDirectory)

    doFirst {
        val javaHome = java21Launcher.get().metadata.installationPath.asFile
        val executableName = if (System.getProperty("os.name").startsWith("Windows")) {
            "jpackage.exe"
        } else {
            "jpackage"
        }
        val jpackage = javaHome.resolve("bin/$executableName")
        require(jpackage.isFile) { "JDK 21のjpackageが見つかりません: $jpackage" }
        windowsPublishDirectory.parentFile.mkdirs()
        commandLine(
            jpackage,
            "--type", "app-image",
            "--name", "GameServerManager",
            "--app-version", project.version.toString().removeSuffix("-SNAPSHOT"),
            "--vendor", "GameServerManager OSS",
            "--description", "ARK and Palworld dedicated server manager",
            "--input", windowsPackageInputDirectory.get().asFile,
            "--main-jar", "game-server-manager.jar",
            "--dest", windowsPublishDirectory,
            "--java-options", "-Dfile.encoding=UTF-8",
            "--java-options", "-Dgame-server-manager.open-browser=true",
            "--java-options", "-Dgame-server-manager.features.demo-enabled=false",
        )
    }

    doLast {
        copy {
            from(
                rootProject.file("../windows/GameServerManager.WindowsSetup.ps1"),
                rootProject.file("../windows/GameServerManager.FirewallHelper.ps1"),
            )
            into(windowsPublishDirectory.resolve("GameServerManager"))
        }
    }
}

val windowsInstallerDirectory = rootProject.file("../windows/installer")

val cleanWindowsInstaller by tasks.registering(Delete::class) {
    delete(windowsInstallerDirectory)
}

tasks.register<Exec>("windowsInstaller") {
    dependsOn(cleanWindowsInstaller, "windowsDistribution")
    outputs.dir(windowsInstallerDirectory)

    doFirst {
        val javaHome = java21Launcher.get().metadata.installationPath.asFile
        val jpackage = javaHome.resolve("bin/jpackage.exe")
        val appImage = windowsPublishDirectory.resolve("GameServerManager")

        require(jpackage.isFile) {
            "JDK 21のjpackage.exeが見つかりません: $jpackage"
        }
        require(appImage.isDirectory) {
            "Windowsアプリイメージが見つかりません: $appImage"
        }

        windowsInstallerDirectory.mkdirs()

        commandLine(
            jpackage,
            "--type", "exe",
            "--name", "GameServerManager",
            "--app-version", project.version.toString().removeSuffix("-SNAPSHOT"),
            "--app-image", appImage,
            "--dest", windowsInstallerDirectory,
            "--win-dir-chooser",
            "--win-menu",
            "--win-menu-group", "GameServerManager",
            "--win-shortcut",
        )
    }
}
