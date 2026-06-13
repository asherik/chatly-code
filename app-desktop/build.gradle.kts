plugins {
    application
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":app-server"))
    implementation(project(":localization"))
}

javafx {
    version = "25.0.1"
    modules = listOf("javafx.controls", "javafx.web")
}

application {
    mainClass.set("com.chatlycode.desktop.DesktopLauncher")
}
