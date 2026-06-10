plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.runtime") version "1.13.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

javafx {
    version = "21.0.4"
    modules("javafx.controls", "javafx.fxml", "javafx.graphics")
}

application {
    mainClass.set("com.voibiz.lanchat.desktop.App")
}

dependencies {
    implementation(project(":core"))
    implementation(libs.atlantafx.base)
    implementation(libs.ikonli.javafx)
    implementation(libs.ikonli.materialdesign2)
    implementation(libs.sqlite.jdbc)

    // Testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.testfx.junit5)
    testImplementation(libs.testfx.core)
    testRuntimeOnly(libs.testfx.monocle)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    modules.set(listOf("java.sql", "jdk.unsupported", "java.desktop", "java.logging"))
    
    jpackage {
        appVersion = "1.0.0"
        imageName = "LANChat"
        
        var baseInstallerOptions = listOf("--vendor", "LAN Chat")
        var baseImageOptions = listOf<String>()
        
        if (org.gradle.internal.os.OperatingSystem.current().isWindows() && file("src/main/resources/icons/icon.ico").exists()) {
            baseImageOptions = baseImageOptions + listOf("--icon", "src/main/resources/icons/icon.ico")
        } else if (org.gradle.internal.os.OperatingSystem.current().isMacOsX() && file("src/main/resources/icons/icon.icns").exists()) {
            baseImageOptions = baseImageOptions + listOf("--icon", "src/main/resources/icons/icon.icns")
        } else if (org.gradle.internal.os.OperatingSystem.current().isLinux()) {
            if (file("src/main/resources/icons/icon.png").exists()) {
                baseImageOptions = baseImageOptions + listOf("--icon", "src/main/resources/icons/icon.png")
            }
            baseInstallerOptions = baseInstallerOptions + listOf("--linux-shortcut", "--type", "deb")
        }

        imageOptions = baseImageOptions
        installerOptions = baseInstallerOptions
    }
}
