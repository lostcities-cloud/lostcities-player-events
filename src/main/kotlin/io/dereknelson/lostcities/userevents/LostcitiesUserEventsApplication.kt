package io.dereknelson.lostcities.userevents

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.dereknelson.lostcities.common.auth.PublicTokenValidator
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import java.util.UUID

// Exchange
const val USER_EVENTS_PLAYER_EVENT_EXCHANGE = "user-events.player-event"
const val USER_EVENTS_PLAYER_EVENT_BINDING = "user-events.player-event-binding"
const val USER_EVENTS_COMMAND_ERROR_EXCHANGE = "user-events.command-error"
const val USER_EVENTS_COMMAND_ERROR_BINDING = "user-events.command-error-binding"

// Incoming
const val PLAYER_EVENT = "player-event"
const val PLAYER_EVENT_DLQ = "player-event-dlq"
const val COMMAND_ERROR_QUEUE = "command-error"
const val COMMAND_ERROR_QUEUE_DLQ = "command-error-dlq"

// Outgoing
const val GAME_EVENT = "game-event"
const val GAME_EVENT_DLQ = "game-event-dlq"
val uuid = UUID.randomUUID().toString()
@SpringBootApplication(
    scanBasePackages = [
        "io.dereknelson.lostcities.userevents",
        "io.dereknelson.lostcities.common.auth",
        "io.dereknelson.lostcities.common.auditing",
    ]
)
@EnableRabbit
@Order(1)
class LostcitiesUserEventsApplication() {


    @Bean
    fun mapper() = jacksonObjectMapper().registerKotlinModule()

    @Bean
    fun authentication() = PublicTokenValidator()

}

fun main(args: Array<String>) {
    runApplication<LostcitiesUserEventsApplication>(*args)

}
