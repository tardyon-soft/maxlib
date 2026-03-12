description = "Core DTO and enums representing MAX API domain model"

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.18.2")

    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.18.2")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    testImplementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.18.2")
}
