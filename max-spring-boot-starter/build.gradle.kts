description = "Spring Boot auto-configuration for MAX framework runtime"

dependencies {
    api(project(":max-dispatcher"))

    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.3.7")
    compileOnly("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("org.springframework:spring-web:6.1.16")
    compileOnly("org.springframework.data:spring-data-redis:3.3.7")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.3.7")

    testImplementation("org.springframework.boot:spring-boot-test:3.3.7")
    testImplementation("org.springframework.data:spring-data-redis:3.3.7")
    testImplementation("org.springframework.boot:spring-boot-starter-validation:3.3.7")
    testImplementation("org.springframework.boot:spring-boot-starter-web:3.3.7")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.7")
}
