description = "Dispatcher, routers and update processing orchestration"

dependencies {
    api(project(":max-model"))
    api(project(":max-client-core"))
    api(project(":max-fsm"))
    api("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.mockito:mockito-core:5.16.0")
    testImplementation(project(":max-testkit"))
}
