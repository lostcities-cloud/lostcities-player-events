package io.dereknelson.lostcities.playerevents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.dereknelson.lostcities.models.PlayerViewDto
import io.dereknelson.lostcities.models.commands.CommandError
import mu.KotlinLogging
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.amqp.core.Message as AmqpMessage

private val logger = KotlinLogging.logger {}

@Component
class PlayerEventListener(
    val objectMapper: ObjectMapper,
    val websocketTemplate: SimpMessagingTemplate
) {
    companion object {
        const val PLAYER_EVENT = "player-event"
        const val PLAYER_EVENT_DLQ = "player-event-dlq"
        const val COMMAND_ERROR_QUEUE = "command-error-event"
        const val COMMAND_ERROR_QUEUE_DLQ = "command-error-event-dlq"
    }

    @Bean @Qualifier(PLAYER_EVENT)
    fun playerEventQueue() = QueueBuilder
        .durable(PLAYER_EVENT)
        .ttl(5000)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", PLAYER_EVENT_DLQ)
        .build()!!

    @Bean @Qualifier(PLAYER_EVENT_DLQ)
    fun playerEventDlQueue() = QueueBuilder
        .durable(PLAYER_EVENT_DLQ)
        .build()!!

    @Bean @Qualifier(COMMAND_ERROR_QUEUE)
    fun commandError() = QueueBuilder
        .durable(COMMAND_ERROR_QUEUE)
        .ttl(5000)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", COMMAND_ERROR_QUEUE_DLQ)
        .build()!!

    @Bean @Qualifier(COMMAND_ERROR_QUEUE_DLQ)
    fun commandErrorDlQueue() = QueueBuilder
        .durable(COMMAND_ERROR_QUEUE_DLQ)
        .build()!!

    @RabbitListener(queues = [PLAYER_EVENT])
    fun sendPlayerEvents(playerEvent: AmqpMessage) =
        objectMapper.readValue<Map<String, PlayerViewDto>>(
            String(playerEvent.body)
        )
            .let { logger.info { "Sending player events: $it" }; it }
            .forEach { (player, view) ->
                websocketTemplate.convertAndSend(
                    "/games-broker/${view.id}/$player",
                    view
                )
            }

    @RabbitListener(queues = [COMMAND_ERROR_QUEUE])
    fun sendCommandErrorEvent(playerEvent: AmqpMessage) =

        objectMapper.readValue<CommandError>(
            String(playerEvent.body)
        ).let {
            websocketTemplate.convertAndSend(
                "/games-broker/${it.id}/${it.player}/errors",
                it
            )
        }
}
