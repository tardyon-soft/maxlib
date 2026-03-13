plugins {
    application
}

description = "Manual Spring Boot polling demo for MAX bot framework"

dependencies {
    implementation(project(":max-spring-boot-starter"))
    implementation("org.springframework.boot:spring-boot-starter:3.3.7")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.7")
}

application {
    mainClass.set("ru.tardyon.botframework.demo.springpolling.DemoSpringPollingApplication")
}
