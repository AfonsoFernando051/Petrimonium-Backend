package com.jf.PetApp.infrastructure.security.google;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.port.GoogleTokenVerifierPort;
import com.jf.PetApp.application.auth.port.GoogleUserInfo;

import java.io.IOException;

@Component
public class GoogleTokenVerifierAdapter implements GoogleTokenVerifierPort {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierAdapter(
        @Value("${google.oauth.client-ids:}") String clientIds
    ) {
        List<String> audiences = Arrays.stream(clientIds.split(","))
            .map(String::trim)
            .filter(id -> !id.isEmpty())
            .toList();

        try {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance()
                )
                .setAudience(audiences)
                .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to initialize Google ID token verifier", e);
        }
    }

    @Override
    public GoogleUserInfo verify(String idToken) throws AuthenticationException {
        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new AuthenticationException("Invalid Google token");
        }

        if (token == null) {
            throw new AuthenticationException("Invalid Google token");
        }

        GoogleIdToken.Payload payload = token.getPayload();
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new AuthenticationException("Google account email is not verified");
        }

        return new GoogleUserInfo(
            payload.getSubject(),
            payload.getEmail(),
            (String) payload.get("name")
        );
    }
}
