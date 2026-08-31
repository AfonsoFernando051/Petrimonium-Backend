package com.jf.PetApp.infrastructure.security.password;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.jf.PetApp.application.auth.port.PasswordEncoderPort;

@Component
public class BCryptPasswordEncoderAdapter
        implements PasswordEncoderPort {

    private final PasswordEncoder encoder =
        new BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String raw, String encoded) {
        return encoder.matches(raw, encoded);
    }
}
