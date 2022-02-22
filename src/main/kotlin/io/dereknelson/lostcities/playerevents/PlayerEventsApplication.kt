package io.dereknelson.lostcities.playerevents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import javax.annotation.PostConstruct

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
