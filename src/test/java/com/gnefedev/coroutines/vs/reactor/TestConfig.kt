package com.gnefedev.coroutines.vs.reactor

import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.web.reactive.server.WebTestClient

@Configuration
class TestConfig {
    @Bean
    fun webTestClient(context: ApplicationContext): WebTestClient {
        return WebTestClient.bindToApplicationContext(context).build()
    }
}
