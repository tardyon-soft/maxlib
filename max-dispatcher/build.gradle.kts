description = "Dispatcher, routers and update processing orchestration"

dependencies {
    api(project(":max-model"))
    api(project(":max-client-core"))
    api(project(":max-fsm"))

    testImplementation("org.mockito:mockito-core:5.16.0")
}
