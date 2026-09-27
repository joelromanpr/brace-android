pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "brace-android"

include(":brace-foundation")
include(":brace-core")
include(":brace-icons")
include(":brace-blueprint-icons")
include(":brace-blueprint-icons-next")
include(":brace-select")
include(":brace-datetime")
include(":catalog")
