description = "Spring Boot auto-configuration for MAX framework runtime"

dependencies {
    api(project(":max-dispatcher"))
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.3.7")
}
