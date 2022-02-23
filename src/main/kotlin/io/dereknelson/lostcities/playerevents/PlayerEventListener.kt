package io.dereknelson.lostcities.playerevents

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
    fun createGame() = Queue("player-event")

    @RabbitListener(queues = ["player-event"])
    fun createGame(gameEvent: AmqpMessage) = objectMapper
        .readValue<Map<String, PlayerViewDto>>(String(gameEvent.body))
        .forEach { (player, view) ->
            websocketTemplate.convertAndSend(
                "/games-broker/${view.id}/$player",
                view
            )
        }
}
