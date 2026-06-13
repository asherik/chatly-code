plugins {
    `java-library`
}

dependencies {
    api(project(":language-spi"))
    api(project(":project-domain"))
    api(project(":shared-kernel"))
}
