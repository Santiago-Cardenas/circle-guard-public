plugins {
    id("java")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.4")
    }
}

dependencies {
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("org.hamcrest:hamcrest:2.2")
}

tasks.withType<Test> {
    useJUnitPlatform()

    // Punto de entrada: gateway expuesto por NodePort en Docker Desktop K8s.
    // Se puede sobreescribir con -DgatewayUrl=... o env GATEWAY_URL.
    val gatewayUrl = System.getenv("GATEWAY_URL")
        ?: project.findProperty("gatewayUrl") as String?
        ?: "http://localhost:30087"
    systemProperty("gatewayUrl", gatewayUrl)

    val authUrl = System.getenv("AUTH_URL")
        ?: project.findProperty("authUrl") as String?
        ?: "http://localhost:30180"
    systemProperty("authUrl", authUrl)

    val dashboardUrl = System.getenv("DASHBOARD_URL")
        ?: project.findProperty("dashboardUrl") as String?
        ?: "http://localhost:30084"
    systemProperty("dashboardUrl", dashboardUrl)

    // Las pruebas E2E SOLO corren si se pasa -PrunE2E=true o env RUN_E2E=1.
    // Esto evita que `./gradlew test` global las dispare sin cluster arriba.
    val runE2e = (project.findProperty("runE2E") as String?)?.toBoolean() == true
        || System.getenv("RUN_E2E") == "1"
    onlyIf { runE2e }

    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
