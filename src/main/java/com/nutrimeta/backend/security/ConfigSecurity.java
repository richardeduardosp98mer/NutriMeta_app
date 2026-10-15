package com.nutrimeta.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfigSecurity {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/portal/login", "/css/**", "/js/**").permitAll()
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
