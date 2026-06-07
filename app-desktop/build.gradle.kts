plugins {
    application
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":app-server"))
    implementation(project(":localization"))
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls")
}

application {
    mainClass.set("com.chatlycode.desktop.DesktopLauncher")
}
