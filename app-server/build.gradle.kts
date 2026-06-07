plugins {
    `java-library`
}

dependencies {
    api(project(":agent-runtime"))
    api(project(":architecture-engine"))
    api(project(":code-graph"))
    api(project(":conversation-service"))
    api(project(":git-service"))
    api(project(":language-generic"))
    api(project(":language-java"))
    api(project(":llm-gateway"))
    api(project(":problem-detector"))
    api(project(":project-domain"))
    api(project(":runtime-service"))
    api(project(":task-manager"))
    api(project(":workspace-safety"))
    api(project(":localization"))
}
