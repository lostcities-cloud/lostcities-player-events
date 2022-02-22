package io.dereknelson.lostcities.playerevents.listener

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.dereknelson.lostcities.models.PlayerViewDto
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Bean
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.amqp.core.Message as AmqpMessage

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
        println("Retrieved gameEvent: ${String(gameEvent.body)}")

        val events = objectMapper.readValue<Map<String, PlayerViewDto>>(String(gameEvent.body))

        events.forEach { (player, view) ->
            websocketTemplate.convertAndSend(
                "/games-broker/${view.id}/$player",
                view
            )
        }
    }
}
