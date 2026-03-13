description = "Spring Boot auto-configuration for MAX framework runtime"

dependencies {
    api(project(":max-dispatcher"))

    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.3.7")
    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.3.7")

    testImplementation("org.springframework.boot:spring-boot-test:3.3.7")
    testImplementation("org.springframework.boot:spring-boot-starter-validation:3.3.7")
}
