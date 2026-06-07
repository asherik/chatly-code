plugins {
    application
}

dependencies {
    implementation(project(":app-server"))
}

application {
    mainClass.set("com.chatlycode.cli.ChatlyCodeCli")
}
