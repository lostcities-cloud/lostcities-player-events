package io.dereknelson.lostcities.playerevents

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.Message as AmqpMessage
import org.springframework.amqp.rabbit.annotation.Queue
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
        println("Message read from player-event: ${String(gameEvent.body)}\n\n\n\n\n\n\n\n\n\n\n\n")
        //val message = message //objectMapper.readValue(gameMessage.body, String::class.java)

        websocketTemplate.convertAndSend("/games-broker/1", gameEvent.body)

    }
}