package gameservermanager.shared.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository

@Configuration
class SecurityConfiguration {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val csrfRepository = HttpSessionCsrfTokenRepository()
        csrfRepository.setHeaderName("X-CSRF-TOKEN")

        http
            .csrf { csrf -> csrf.csrfTokenRepository(csrfRepository) }
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().permitAll()
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .accessDeniedHandler { _, response, _ ->
                        response.sendError(403)
                    }
            }
            .requestCache { cache -> cache.disable() }

        return http.build()
    }
}
