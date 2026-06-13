plugins {
    `java-library`
}

dependencies {
    api(project(":code-graph"))
    implementation("com.structurizr:structurizr-dsl:6.2.1")
    implementation("com.structurizr:structurizr-client:6.2.1")
}
