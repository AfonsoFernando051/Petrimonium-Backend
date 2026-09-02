package com.jf.PetApp.application.auth.usecase;

import org.springframework.dao.DataIntegrityViolationException;

import com.jf.PetApp.application.auth.dto.RegisterCommand;
import com.jf.PetApp.application.auth.dto.RegisterResult;
import com.jf.PetApp.application.auth.exception.UserAlreadyExistsException;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.RoleEnum;

public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    public RegisterUserUseCaseImpl(
        UserRepository userRepository,
        PasswordEncoderPort passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResult execute(RegisterCommand command) {

        userRepository.findByEmail(command.email())
            .ifPresent(u -> {
                throw new UserAlreadyExistsException();
            });

        User user = User.create(
            command.username(),
            command.email(),
            passwordEncoder.encode(command.password()),
            RoleEnum.USER
        );

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Lost the race: another registration for this email committed between our
            // findByEmail check and this save. The unique constraint on jf_users.email is
            // the real guarantee; this translates its violation into the same exception the
            // fast-path check throws, so callers never see a persistence-layer exception.
            throw new UserAlreadyExistsException();
        }

        return new RegisterResult(
            saved.getId(),
            saved.getUsername(),
            saved.getEmail()
        );
    }
}