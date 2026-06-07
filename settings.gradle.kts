pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "chatly-code"

include(
    "app-cli",
    "app-desktop",
    "app-server",
    "shared-kernel",
    "project-domain",
    "language-spi",
    "language-java",
    "code-graph",
    "architecture-engine",
    "problem-detector",
    "task-manager",
    "workspace-safety",
    "runtime-service",
    "git-service",
    "conversation-service",
    "agent-runtime",
    "llm-gateway",
    "localization"
)
