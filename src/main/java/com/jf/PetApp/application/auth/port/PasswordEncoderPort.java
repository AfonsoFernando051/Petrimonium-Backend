package com.jf.PetApp.application.auth.port;

public interface PasswordEncoderPort {

    boolean matches(String rawPassword, String encodedPassword);

    String encode(String rawPassword);
}