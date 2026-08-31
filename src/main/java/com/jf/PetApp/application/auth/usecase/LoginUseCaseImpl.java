package com.jf.PetApp.application.auth.usecase;


import com.jf.PetApp.application.auth.dto.LoginCommand;
import com.jf.PetApp.application.auth.dto.LoginResult;
import com.jf.PetApp.application.auth.dto.RefreshTokenResult;
import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.auth.service.RefreshTokenIssuerService;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.user.port.DemoAccountResetPort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;

public class LoginUseCaseImpl implements LoginUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final RefreshTokenIssuerService refreshTokenIssuerService;
    private final StreakService streakService;
    private final DemoAccountResetPort demoAccountResetPort;

    public LoginUseCaseImpl(
            UserRepository userRepository,
            PasswordEncoderPort passwordEncoder,
            RefreshTokenIssuerService refreshTokenIssuerService,
            StreakService streakService,
            DemoAccountResetPort demoAccountResetPort) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenIssuerService = refreshTokenIssuerService;
        this.streakService = streakService;
        this.demoAccountResetPort = demoAccountResetPort;
    }

    @Override
    public LoginResult execute(LoginCommand command) throws AuthenticationException {

        User user = userRepository.findByEmail(command.email())
                .or(() -> userRepository.findByUsername(command.email()))
                .orElseThrow(AuthenticationException::new);

        if (!passwordEncoder.matches(
                command.password(),
                user.getPassword())) {
            throw new AuthenticationException();
        }

        demoAccountResetPort.resetIfDemoAccount(user.getUsername());

        RefreshTokenResult tokens = refreshTokenIssuerService.issueFor(user, command.appContext());
        streakService.recordActivity(user.getId());

        return new LoginResult(tokens.accessToken(), tokens.refreshToken());
    }
}