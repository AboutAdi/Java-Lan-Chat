plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
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
}
