plugins {
    `java-library`
}

dependencies {
    api(project(":problem-detector"))
    api(project(":project-domain"))
    api(project(":shared-kernel"))
}
