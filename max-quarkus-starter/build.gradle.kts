description = "Quarkus starter for MAX framework runtime"

val quarkusVersion = "3.17.7"

dependencies {
    api(project(":max-dispatcher"))

    implementation("io.quarkus:quarkus-arc:$quarkusVersion")
    implementation("io.quarkus:quarkus-spring-boot-properties:$quarkusVersion")
    implementation("io.quarkus:quarkus-hibernate-validator:$quarkusVersion")
    implementation("io.quarkus:quarkus-rest:$quarkusVersion")
    implementation("io.quarkus:quarkus-rest-jackson:$quarkusVersion")
    implementation("io.smallrye:jandex:3.2.3")

    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.3.7")
    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    compileOnly("io.quarkus:quarkus-redis-client:$quarkusVersion")

    testImplementation("io.quarkus:quarkus-junit5:$quarkusVersion")
    testImplementation("org.springframework.boot:spring-boot:3.3.7")
    testImplementation("io.quarkus:quarkus-redis-client:$quarkusVersion")
    testImplementation("io.quarkus:quarkus-junit5-component:$quarkusVersion")
}
