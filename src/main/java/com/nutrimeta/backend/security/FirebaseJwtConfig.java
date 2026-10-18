package com.nutrimeta.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Collection;

// Verifica los ID tokens de Firebase sin clave privada: estan firmados por Google (RS256)
// y sus claves publicas se descargan de esta direccion (se guardan en cache).
// Un token es valido si: la firma es correcta, no vencio, el emisor es
// https://securetoken.google.com/<projectId>, la audiencia es el projectId y trae `sub` (el uid).
@Configuration
public class FirebaseJwtConfig {

    private static final String CLAVES_PUBLICAS =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    @Bean
    public JwtDecoder firebaseJwtDecoder(@Value("${firebase.project-id}") String projectId) {

        // La descarga de claves ocurre con el primer token, no al arrancar el backend.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(CLAVES_PUBLICAS).build();

        OAuth2TokenValidator<Jwt> emisorYVencimiento =
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projectId);

        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<Collection<String>>(
                JwtClaimNames.AUD, aud -> aud != null && aud.contains(projectId));

        OAuth2TokenValidator<Jwt> conUid = new JwtClaimValidator<String>(
                JwtClaimNames.SUB, sub -> sub != null && !sub.isBlank());

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(emisorYVencimiento, audiencia, conUid));

        return decoder;
    }
}
