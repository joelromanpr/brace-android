plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.dokka) apply false
}

apiValidation {
    ignoredProjects.add("catalog")
}

group = "io.github.joelromanpr.brace"
version = providers.gradleProperty("releaseVersion").orElse("1.0.0-SNAPSHOT").get()

subprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register<Exec>("checkInventory") {
    description = "Checks the pinned Blueprint inventory and generated coverage pages."
    group = "verification"
    commandLine("python3", "scripts/generate_coverage.py", "--check")
}

tasks.register<Exec>("checkTokenGeneration") {
    description = "Checks that generated Kotlin design tokens match the versioned token source."
    group = "verification"
    commandLine("python3", "scripts/generate_tokens.py", "--check")
}

tasks.register<Exec>("checkLinkContrast") {
    description = "Checks Link text contrast against every default token theme and state."
    group = "verification"
    commandLine("python3", "scripts/check_link_contrast.py")
}

tasks.named("checkTokenGeneration") { dependsOn("checkLinkContrast") }

tasks.register<Exec>("checkBlueprintIconGeneration") {
    description = "Checks the pinned Blueprint glyph manifest and generated Kotlin names."
    group = "verification"
    commandLine("python3", "scripts/generate_blueprint_icons.py", "--check")
}


tasks.register<Exec>("checkBlueprintNextIconGeneration") {
    description = "Checks the pinned Blueprint /next glyph manifest and generated Kotlin names."
    group = "verification"
    commandLine("python3", "scripts/generate_blueprint_next_icons.py", "--check")
}
