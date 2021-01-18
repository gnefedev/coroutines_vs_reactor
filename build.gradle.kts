import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.4.21"
    id("org.jetbrains.kotlin.plugin.spring") version "1.4.21"
}

repositories {
    jcenter()
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_15.toString()
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
    implementation("org.springframework:spring-aspects")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("io.r2dbc:r2dbc-h2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.4.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")
    implementation("io.github.microutils:kotlin-logging:1.4.1")
    implementation("com.michael-bull.kotlin-retry:kotlin-retry:1.0.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
