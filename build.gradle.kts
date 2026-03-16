import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

plugins {
    base
}

allprojects {
    group = "ru.tardyon.botframework"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.12.1")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    if (name.startsWith("max-")) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set("MAX Java Bot Framework module: ${project.name}")
                        url.set("https://github.com/tardyon/max-bot-framework")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                        developers {
                            developer {
                                id.set("tardyon")
                                name.set("Tardyon Team")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/tardyon-soft/maxlib.git")
                            developerConnection.set("scm:git:ssh://git@github.com:tardyon-soft/maxlib.git")
                            url.set("https://github.com/tardyon-soft/maxlib")
                        }
                    }
                }
            }
            repositories {
                maven {
                    name = "mavenCentral"
                    url = uri(
                        if (version.toString().endsWith("SNAPSHOT")) {
                            "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                        } else {
                            "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                        }
                    )
                    credentials {
                        username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
                        password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
                    }
                }
            }
        }

        extensions.configure<SigningExtension> {
            val signingKey = providers.environmentVariable("MAVEN_GPG_PRIVATE_KEY").orNull
            val signingPassphrase = providers.environmentVariable("MAVEN_GPG_PASSPHRASE").orNull
            if (!signingKey.isNullOrBlank()) {
                useInMemoryPgpKeys(signingKey, signingPassphrase)
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}

tasks.register("publishVanillaJavaToMavenCentral") {
    group = "publishing"
    description = "Publishes vanilla Java runtime modules to Maven Central (without Spring starter)."
    dependsOn(
        ":max-model:publishMavenJavaPublicationToMavenCentralRepository",
        ":max-client-core:publishMavenJavaPublicationToMavenCentralRepository",
        ":max-fsm:publishMavenJavaPublicationToMavenCentralRepository",
        ":max-dispatcher:publishMavenJavaPublicationToMavenCentralRepository",
        ":max-testkit:publishMavenJavaPublicationToMavenCentralRepository"
    )
}

tasks.register("publishStarterToMavenCentral") {
    group = "publishing"
    description = "Publishes Spring Boot starter stack to Maven Central."
    dependsOn(
        "publishVanillaJavaToMavenCentral",
        ":max-spring-boot-starter:publishMavenJavaPublicationToMavenCentralRepository"
    )
}
