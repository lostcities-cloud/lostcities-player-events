package io.dereknelson.lostcities.playerevents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableRabbit
class LostcitiesPlayerEventsApplication

@Bean
fun mapper(): ObjectMapper {
    val mapper = jacksonObjectMapper()

    mapper.registerKotlinModule()

    return mapper
}

fun main(args: Array<String>) {
    runApplication<LostcitiesPlayerEventsApplication>(*args)
}
