pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
val braceRepository = providers.gradleProperty("braceRepository").orElse("local").get()
require(braceRepository == "local" || braceRepository == "central") {
    "braceRepository must be local or central"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (braceRepository == "local") mavenLocal()
        google()
        mavenCentral()
    }
}
rootProject.name = "brace-consumer-smoke"
include(":app")
