package io.dereknelson.lostcities.userevents

import io.dereknelson.lostcities.common.auth.LostCitiesAuthenticationToken
import io.dereknelson.lostcities.common.auth.PublicTokenValidator
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {


    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(JwtChannelInterceptor());
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/games-broker")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/player-events/lost-cities").setAllowedOriginPatterns("*")
        registry.addEndpoint("/player-events/lost-cities").setAllowedOriginPatterns("*").withSockJS()
    }
}

class JwtChannelInterceptor : ChannelInterceptor {
    val tokenValidator = PublicTokenValidator()
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor: StompHeaderAccessor? = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (StompCommand.CONNECT == accessor?.command) {
            val authToken = accessor.getFirstNativeHeader("Authorization")
            // 1. Remove "Bearer " prefix
            // 2. Validate token using your JwtService
            // 3. Create Authentication object
            val preparedToken = authToken?.split("Bearer ")[1]
            val user: LostCitiesAuthenticationToken? = tokenValidator.getAuthentication(preparedToken)

            // Critical: Set the user in the accessor so Spring knows who this session belongs to
            if(user != null) {
                accessor.user = user.userDetails
            }
        }
        return message
    }
}
