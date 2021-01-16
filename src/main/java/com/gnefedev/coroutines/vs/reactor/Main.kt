package com.gnefedev.coroutines.vs.reactor

import org.springframework.boot.SpringApplication

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplication.run(Application::class.java).start()
    }
}
