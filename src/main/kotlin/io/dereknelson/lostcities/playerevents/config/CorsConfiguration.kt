package io.dereknelson.lostcities.playerevents.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfiguration {

    companion object {
        const val allowedOrigins = "*"
    }

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()



        if (allowedOrigins.isNotEmpty()) {
            //source.registerCorsConfiguration("/swagger-ui/**", corsConfiguration())
            source.registerCorsConfiguration("/**", corsConfiguration())
            source.registerCorsConfiguration("/management/**", corsConfiguration())
            //source.registerCorsConfiguration("/v3/api-docs", corsConfiguration())
        }



        return CorsFilter(source)
    }

    fun corsConfiguration(): CorsConfiguration {
        val config = CorsConfiguration();
        config.allowedOriginPatterns = mutableListOf("http://jxy.me")

        config.allowCredentials = true

        config.addAllowedMethod("OPTIONS")
        config.addAllowedMethod("HEAD")
        config.addAllowedMethod("GET")
        config.addAllowedMethod("PUT")
        config.addAllowedMethod("POST")
        config.addAllowedMethod("DELETE")
        config.addAllowedMethod("PATCH")

        return config
    }
}