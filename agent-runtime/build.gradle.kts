plugins {
    `java-library`
}

dependencies {
    api(project(":code-graph"))
    api(project(":conversation-service"))
    api(project(":git-service"))
    api(project(":llm-gateway"))
    api(project(":problem-detector"))
    api(project(":project-domain"))
    api(project(":runtime-service"))
    api(project(":shared-kernel"))
    api(project(":task-manager"))
    api(project(":workspace-safety"))
}
