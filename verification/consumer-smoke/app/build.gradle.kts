plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

android {
    namespace = "io.github.joelromanpr.brace.consumer"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.joelromanpr.brace.consumer"
        minSdk = 26
        targetSdk = 36
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

val braceVersion = providers.gradleProperty("braceVersion").orElse("0.1.0-SNAPSHOT").get()

dependencies {
    implementation("io.github.joelromanpr.brace:brace-foundation:$braceVersion")
    implementation("io.github.joelromanpr.brace:brace-core:$braceVersion")
    implementation("io.github.joelromanpr.brace:brace-icons:$braceVersion")
    implementation("io.github.joelromanpr.brace:brace-blueprint-icons:$braceVersion")
    implementation("io.github.joelromanpr.brace:brace-blueprint-icons-next:$braceVersion")
    implementation("io.github.joelromanpr.brace:brace-select:$braceVersion")
    implementation("io.github.joelromanpr.brace:brace-datetime:$braceVersion")
    implementation("io.github.joelromanpr.brace:brace-table:$braceVersion")
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
}
