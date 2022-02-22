package io.dereknelson.lostcities.playerevents.listener

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.Message as AmqpMessage
import io.dereknelson.lostcities.models.matches.PlayerEvent
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Bean

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component


@Component
class PlayerEventListener(
    val objectMapper: ObjectMapper,
    val websocketTemplate: SimpMessagingTemplate
) {

    @Bean
    fun createGame(): Queue {
        return Queue("player-event")
    }

    @RabbitListener(queues = ["player-event"])
    fun createGame(gameEvent: AmqpMessage) {
        //println("Message read from player-event: ${String(gameEvent.body)}\n\n\n\n\n\n\n\n\n\n\n\n")
        val message = objectMapper.readValue(gameEvent.body, Array<PlayerEvent>::class.java)

        println("Retrieved gameEvent: ${message}")

        websocketTemplate.convertAndSend("/games-broker/1", gameEvent.body)
    }

}