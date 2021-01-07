plugins {
    application
    java
}

repositories {
    jcenter()
}

tasks.test {
    useJUnitPlatform {
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:2.4.1"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.r2dbc:r2dbc-pool")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("io.r2dbc:r2dbc-h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    compileOnly("org.projectlombok:lombok:1.18.16")
    annotationProcessor("org.projectlombok:lombok:1.18.16")

    testCompileOnly("org.projectlombok:lombok:1.18.16")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.16")
}