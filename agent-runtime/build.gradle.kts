plugins {
    `java-library`
}

dependencies {
    api(project(":llm-gateway"))
    api(project(":task-manager"))
    api(project(":workspace-safety"))
}
