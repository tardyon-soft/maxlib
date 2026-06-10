description = "Micronaut auto-configuration for MAX framework runtime"

dependencies {
    api(project(":max-dispatcher"))

    compileOnly("io.micronaut:micronaut-context:4.9.1")
    compileOnly("io.micronaut:micronaut-http:4.9.1")
    compileOnly("io.micronaut:micronaut-http-server:4.9.1")
    compileOnly("io.micronaut:micronaut-inject:4.9.1")
    compileOnly("io.micronaut.validation:micronaut-validation:4.8.1")
    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("io.micronaut.redis:micronaut-redis-lettuce:6.9.0")

    annotationProcessor("io.micronaut:micronaut-inject-java:4.9.1")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java:4.9.1")

    testImplementation("io.micronaut:micronaut-context:4.9.1")
    testImplementation("io.micronaut:micronaut-http-client:4.9.1")
    testImplementation("io.micronaut:micronaut-http-server-netty:4.9.1")
    testImplementation("io.micronaut:micronaut-jackson-databind:4.9.1")
    testImplementation("io.micronaut.test:micronaut-test-junit5:4.7.0")
    testImplementation("io.micronaut.validation:micronaut-validation:4.8.1")
    testImplementation("org.mockito:mockito-core:5.16.0")
    testImplementation("io.micronaut.redis:micronaut-redis-lettuce:6.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.1")
}
