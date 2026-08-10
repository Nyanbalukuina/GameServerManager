package gameservermanager.configuration

import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository

@Configuration
class SecurityConfiguration {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val csrfRepository = HttpSessionCsrfTokenRepository()
        csrfRepository.setHeaderName("X-CSRF-TOKEN")

        http
            .csrf { csrf -> csrf.csrfTokenRepository(csrfRepository) }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        "/api/auth/status",
                        "/api/auth/csrf",
                        "/api/auth/setup",
                        "/api/auth/login",
                        "/",
                        "/index.html",
                        "/servers/new",
                        "/servers/new/palworld",
                        "/servers/palworld",
                        "/assets/**",
                        "/favicon.ico",
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form
                    .loginProcessingUrl("/api/auth/login")
                    .successHandler { _, response, _ -> response.status = HttpServletResponse.SC_NO_CONTENT }
                    .failureHandler { _, response, _ -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED) }
                    .permitAll()
            }
            .logout { logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler { _, response, _ -> response.status = HttpServletResponse.SC_NO_CONTENT }
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { _, response, _ ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                    }
                    .accessDeniedHandler { _, response, _ ->
                        response.sendError(HttpServletResponse.SC_FORBIDDEN)
                    }
            }
            .requestCache { cache -> cache.disable() }

        return http.build()
    }
}
