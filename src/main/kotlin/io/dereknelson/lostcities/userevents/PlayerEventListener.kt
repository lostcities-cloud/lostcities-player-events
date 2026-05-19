package io.dereknelson.lostcities.userevents

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.dereknelson.lostcities.common.auth.LostCitiesUserDetails
import io.dereknelson.lostcities.models.gamestate.GameEvent
import io.dereknelson.lostcities.models.matches.commands.CommandError
import io.dereknelson.lostcities.models.matches.state.PlayerViewDto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.QueueBinding

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn

import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import java.lang.Long.parseLong
import java.util.Optional
import java.util.regex.Pattern
import org.springframework.amqp.core.Message as AmqpMessage

const val matchRegexString = "/games-broker/(?<match>[0-9]*)/(?<login>.*)$"
val matchRegex: Pattern = Pattern.compile(matchRegexString)


@Component
class PlayerEventListener(
    private val objectMapper: ObjectMapper,
    private val rabbitTemplate: RabbitTemplate,
    private val websocketTemplate: SimpMessagingTemplate,

) {


    private val logger: Logger = LoggerFactory.getLogger(PlayerEventListener::class.java)


    @Bean
    @Qualifier("user-events-exchange")
    fun userEventsExchange(): DirectExchange {
        return DirectExchange("user-events", true, false)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @EventListener
    fun handleSubscriptionEvent(event: SessionSubscribeEvent) {
        val userDetails: LostCitiesUserDetails? = event.user as LostCitiesUserDetails?

        if(userDetails != null  && userDetails.isAuthenticated) {
            val message: Message<ByteArray> = event.message

            try {
                val destination: CharSequence = message.headers.get("simpDestination")!! as String

                val match = matchRegex.matcher(destination)

                if (match.find()) {
                    //val id = parseLong(match.)
                    val id = parseLong(match.group(1))
                    val gameEvent = GameEvent(id = id)



                    rabbitTemplate.convertAndSend(
                        "user-events",
                        "game-event.game-state-group",
                        ProtoBuf.encodeToByteArray(gameEvent)
                    )
                }
            } catch (e: Exception) {
                println("Could not retrieve native header, or not using StompHeaderAccessor properly.")
            }
        }
    }

    @RabbitListener(bindings = [
        QueueBinding(
            value = Queue(
                name="\${app.playerEventQueue}",
                durable =  "false",
                exclusive = "true",
                autoDelete = "true",
                declare = "true"
            ),
            exchange = Exchange(
                name = "game-events.userevents",
                type = "fanout",
                declare = "true",
                durable = "true"
            ),
            key = ["game-events.userevents.player-event"]
        )
    ])
    fun sendPlayerEvents(playerEvent: AmqpMessage) {
        val dto = objectMapper.readValue<PlayerViewDto>(
            String(playerEvent.body),
        )

        websocketTemplate.convertAndSend(
            "/games-broker/${dto.id}/${dto.player}",
            String(playerEvent.body),
        )
    }

    @RabbitListener(bindings = [
            QueueBinding(
            value = Queue(
                name="\${app.commandErrorQueue}",
                durable =  "false",
                exclusive = "true",
                autoDelete = "true",
                declare = "true"
            ),
            exchange = Exchange(
                name = "game-events.userevents",
                type = "fanout",
                declare = "true",
                durable = "true"
            ),
            key = ["game-events.userevents.command-error"]
        )
    ])
    fun sendCommandErrorEvent(playerEvent: AmqpMessage) {

        val dto = objectMapper.readValue<CommandError>(
            String(playerEvent.body),
        )

        websocketTemplate.convertAndSend(
            "/games-broker/${dto.id}/${dto.player}/errors",
            dto,
        )
    }
}

@Component
@Order(100)
class QueueConfiguration(
    @param:Value("\${app.instanceId}")
    final val instanceUuid: String
) {

    @Bean
    @Qualifier(PLAYER_EVENT_DLQ)
    @Order(90)
    fun playerEventDlQueue() = QueueBuilder
        .durable( PLAYER_EVENT_DLQ)
        .quorum()
        .build()!!

    @Bean
    @Qualifier(COMMAND_ERROR_QUEUE)
    @Order(100)
    fun commandError() = QueueBuilder
        .nonDurable("$COMMAND_ERROR_QUEUE.$instanceUuid")
        .autoDelete()
        .exclusive()
        .ttl(5000)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", COMMAND_ERROR_QUEUE_DLQ)
        .build()!!

    @Bean
    @Qualifier(COMMAND_ERROR_QUEUE_DLQ)
    @Order(90)
    fun commandErrorDlQueue() = QueueBuilder
        .durable(COMMAND_ERROR_QUEUE_DLQ)
        .quorum()
        .build()!!

}


@Component
class SubscriptionInterceptor : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor: StompHeaderAccessor? = MessageHeaderAccessor
            .getAccessor<StompHeaderAccessor>(message, StompHeaderAccessor::class.java)

        if (accessor != null && StompCommand.SUBSCRIBE == accessor.command) {

            // Implement your logic to decide whether to allow the subscription
            val destinationInfo = getMatchInfo(accessor)
            return destinationInfo.map {
                if(it.login == accessor.user?.name) {
                    return@map message
                } else {
                    return@map null
                }
            }.get()
        }
        return message
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class DestinationMatchInfo(
    val matchId: Long,
    val login: String
)

fun getMatchInfo(accessor: StompHeaderAccessor): Optional<DestinationMatchInfo> {
    val destination: CharSequence = accessor.destination as String

    val match = matchRegex.matcher(destination)

    if (match.find()) {
        //val id = parseLong(match.)
        val id = parseLong(match.group(1))
        val login = match.group(2)

        return Optional.of(DestinationMatchInfo(id, login))

    }

    return Optional.empty()
}
