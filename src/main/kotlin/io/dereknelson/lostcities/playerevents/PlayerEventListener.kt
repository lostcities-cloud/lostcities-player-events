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

    companion object {
        const val PLAYER_EVENT = "player-event"
    }

    @Bean
    fun playerEventQueue() = Queue(PLAYER_EVENT)

    @RabbitListener(queues = [PLAYER_EVENT])
    fun sendPlayerEvents(playerEvent: AmqpMessage) = objectMapper
        .readValue<Map<String, PlayerViewDto>>(String(playerEvent.body))
        .forEach { (player, view) ->
            websocketTemplate.convertAndSend(
                "/games-broker/${view.id}/$player",
                view
            )
        }
}
