package com.eazybytes.eazystore.security;

import com.eazybytes.eazystore.filter.JWTTokenValidatorFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class EazyStoreSecurityConfig {

    private final List<String> publicPaths;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            Environment environment
    ) throws Exception {

        return http

                // =========================
                // CSRF CONFIGURATION
                // =========================
                .csrf(csrf -> csrf.disable())

                // =========================
                // CORS CONFIGURATION
                // =========================
                .cors(corsConfig -> corsConfig
                        .configurationSource(corsConfigurationSource())
                )

                // =========================
                // AUTHORIZATION
                // =========================
                .authorizeHttpRequests(requests -> {

                    // Existing public endpoints
                    publicPaths.forEach(path ->
                            requests.requestMatchers(path).permitAll()
                    );

                    // Stripe payment APIs
                    requests.requestMatchers(
                            "/api/v1/payment/**"
                    ).permitAll();

                    // AI Shopping Assistant
                    requests.requestMatchers(
                            "/api/v1/ai/**"
                    ).permitAll();

                    // Admin APIs
                    requests.requestMatchers(
                            "/api/v1/admin/**"
                    ).hasRole("ADMIN");

                    // Actuator
                    requests.requestMatchers(
                            "/eazystore/actuator/**"
                    ).hasRole("OPS_ENG");

                    // Swagger
                    requests.requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs/**"
                    ).hasAnyRole("DEV_ENG", "QA_ENG");

                    // Everything else requires USER or ADMIN
                    requests.anyRequest()
                            .hasAnyRole("USER", "ADMIN");
                })

                // =========================
                // JWT FILTER
                // =========================
                .addFilterBefore(
                        new JWTTokenValidatorFilter(
                                publicPaths,
                                environment
                        ),
                        BasicAuthenticationFilter.class
                )

                // =========================
                // GOOGLE OAUTH2
                // =========================
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                // =========================
                // LOGIN METHODS
                // =========================
                .formLogin(withDefaults())
                .httpBasic(withDefaults())

                .build();
    }

    // =========================
    // AUTHENTICATION MANAGER
    // =========================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationProvider authenticationProvider
    ) {
        return new ProviderManager(authenticationProvider);
    }

    // =========================
    // PASSWORD ENCODER
    // =========================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================
    // COMPROMISED PASSWORD CHECK
    // =========================
    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }

    // =========================
    // CORS
    // =========================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                Arrays.asList(
                        "http://localhost:5173",
                        "https://eazystore-project.vercel.app"
                )
        );

        config.setAllowedMethods(
                Collections.singletonList("*")
        );

        config.setAllowedHeaders(
                Collections.singletonList("*")
        );

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }
}