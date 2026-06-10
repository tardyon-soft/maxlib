plugins {
    application
}

description = "Manual Quarkus polling demo for MAX bot framework"

val quarkusVersion = "3.17.7"
val generatedDemoSources = layout.buildDirectory.dir("generated/demo-shared-main")
val syncSharedDemoSources by tasks.registering(Sync::class) {
    into(generatedDemoSources)
    from("${rootProject.projectDir}/demo-spring-polling/src/main/java") {
        exclude("ru/tardyon/botframework/demo/springpolling/DemoSpringPollingApplication.java")
        exclude("ru/tardyon/botframework/demo/springpolling/BackgroundTaskMonitorService.java")
        exclude("ru/tardyon/botframework/demo/springpolling/ApiSmokeService.java")
        exclude("ru/tardyon/botframework/demo/springpolling/FacadeScreenController.java")
        exclude("ru/tardyon/botframework/demo/springpolling/DemoCounterWidgetController.java")
    }
    from("${rootProject.projectDir}/demo-shared-polling/src/main/java")
}

dependencies {
    implementation(project(":max-quarkus-starter"))
    implementation("io.quarkus:quarkus-core:$quarkusVersion")
    implementation("io.quarkus:quarkus-arc:$quarkusVersion")
    implementation("io.quarkus:quarkus-config-yaml:$quarkusVersion")
    implementation("io.quarkus:quarkus-rest:$quarkusVersion")
    implementation("io.quarkus:quarkus-rest-jackson:$quarkusVersion")
    implementation("io.quarkus:quarkus-hibernate-validator:$quarkusVersion")
    implementation("io.quarkus:quarkus-redis-client:$quarkusVersion")

    testImplementation("io.quarkus:quarkus-junit5:$quarkusVersion")
    testImplementation("io.quarkus:quarkus-junit5-component:$quarkusVersion")
}

sourceSets {
    named("main") {
        java {
            setSrcDirs(listOf("src/main/java", generatedDemoSources))
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}

application {
    mainClass.set("ru.tardyon.botframework.demo.quarkuspolling.DemoQuarkusPollingApplication")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(syncSharedDemoSources)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(syncSharedDemoSources)
}
