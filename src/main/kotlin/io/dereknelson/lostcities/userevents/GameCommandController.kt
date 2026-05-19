package io.dereknelson.lostcities.userevents

import io.dereknelson.lostcities.models.gamestate.GameEvent
import io.dereknelson.lostcities.models.gamestate.TurnCommandRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
class GameCommandController(
    private val rabbitTemplate: RabbitTemplate,
) {
    @OptIn(ExperimentalSerializationApi::class)
    @MessageMapping("/commands/{matchId}")
    fun turnCommandRequest(
        @DestinationVariable matchId: Long,
        @Payload turnCommandRequest: TurnCommandRequest,
        principal: Principal
    ) {
        val playerTurnCommand = turnCommandRequest.withPlayer(principal.name)

        val gameEvent = GameEvent(id = matchId, turn = playerTurnCommand, login = principal.name)

        rabbitTemplate.convertAndSend(
            "$GAME_EVENT.game-state-group",
            ProtoBuf.encodeToByteArray(gameEvent),
        )
    }
}
