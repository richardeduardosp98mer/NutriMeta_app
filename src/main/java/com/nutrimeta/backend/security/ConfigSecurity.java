package com.nutrimeta.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfigSecurity {

    // Las contraseñas de `administrador.password_hash` están en BCrypt puro ($2a$...).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Servicios REST para la app: sin sesión ni formulario de login.
    // Todo lo de /api exige un ID token de Firebase valido (Authorization: Bearer ...), salvo el catalogo,
    // que es publico. El token se verifica en FirebaseJwtConfig.
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/catalogo").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    // Portal administrativo: login con formulario.
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/portal/login", "/error", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
        ).formLogin(from -> from
                .loginPage("/portal/login")
                .loginProcessingUrl("/portal/login")
                .defaultSuccessUrl("/portal", true)
                .failureUrl("/portal/login?error")
                .permitAll()
        ).logout(logout -> logout
                .logoutUrl("/portal/logout")
                .logoutSuccessUrl("/portal/login?salio")
        );

        return http.build();
    }
}
