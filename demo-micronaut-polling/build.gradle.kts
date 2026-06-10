plugins {
    application
}

description = "Manual Micronaut polling demo for MAX bot framework"

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
    implementation(project(":max-micronaut-starter"))
    implementation("io.micronaut:micronaut-context:4.9.1")
    implementation("io.micronaut:micronaut-runtime:4.9.1")
    implementation("io.micronaut:micronaut-http-server-netty:4.9.1")
    implementation("io.micronaut:micronaut-jackson-databind:4.9.1")
    implementation("io.micronaut.validation:micronaut-validation:4.8.1")
    implementation("io.micronaut.redis:micronaut-redis-lettuce:6.9.0")

    annotationProcessor("io.micronaut:micronaut-inject-java:4.9.1")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java:4.9.1")

    testImplementation("io.micronaut:micronaut-context:4.9.1")
    testImplementation("io.micronaut.test:micronaut-test-junit5:4.7.0")
}

sourceSets {
    named("main") {
        java {
            srcDirs("src/main/java", generatedDemoSources)
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(syncSharedDemoSources)
}

application {
    mainClass.set("ru.tardyon.botframework.demo.micronautpolling.DemoMicronautPollingApplication")
}
