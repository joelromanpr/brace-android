import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

android {
    namespace = "io.github.joelromanpr.brace.blueprinticons"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }
    testOptions { targetSdk = 36 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    api(project(":brace-icons"))
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.foundation)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(libs.compose.ui.test.junit4.accessibility)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    debugImplementation(libs.compose.ui.test.manifest)
}


mavenPublishing {
    configure(AndroidSingleVariantLibrary("release", true, false))
    coordinates("io.github.joelromanpr.brace", "brace-blueprint-icons", project.version.toString())
    publishToMavenCentral(automaticRelease = false)
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    pom {
        name.set("Brace Blueprint Legacy Icon Pack")
        description.set("Optional Apache-2.0 Blueprint 6.18.0 pinned legacy glyph pack for Brace Android.")
        inceptionYear.set("2026")
        url.set("https://github.com/joelromanpr/brace-android")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("joelromanpr")
                name.set("Joel Roman")
                url.set("https://github.com/joelromanpr")
            }
        }
        scm {
            url.set("https://github.com/joelromanpr/brace-android")
            connection.set("scm:git:https://github.com/joelromanpr/brace-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/joelromanpr/brace-android.git")
        }
    }
}


// Vanniktech 0.35's default Android Javadoc task uses an older Dokka runtime.
// Package the KDoc output of our pinned Dokka plugin instead.
val braceJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaHtml"))
}

afterEvaluate {
    publishing {
        publications.withType(MavenPublication::class.java).configureEach {
            artifact(braceJavadocJar)
        }
    }
}
