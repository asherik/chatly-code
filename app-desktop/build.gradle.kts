plugins {
    application
    id("org.openjfx.javafxplugin")
}

dependencies {
    implementation(project(":app-server"))
    implementation(project(":localization"))
}

javafx {
    version = "26"
    modules = listOf("javafx.controls")
}

application {
    mainClass.set("com.chatlycode.desktop.DesktopLauncher")
}
