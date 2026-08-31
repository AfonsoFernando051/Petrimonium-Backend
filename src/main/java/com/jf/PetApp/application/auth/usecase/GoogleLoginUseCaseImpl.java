package com.jf.PetApp.application.auth.usecase;

import com.jf.PetApp.application.auth.dto.GoogleLoginCommand;
import com.jf.PetApp.application.auth.dto.LoginResult;
import com.jf.PetApp.application.auth.dto.RefreshTokenResult;
import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.port.GoogleTokenVerifierPort;
import com.jf.PetApp.application.auth.port.GoogleUserInfo;
import com.jf.PetApp.application.auth.service.RefreshTokenIssuerService;
import com.jf.PetApp.application.gamification.service.StreakService;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AuthProviderEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

public class GoogleLoginUseCaseImpl implements GoogleLoginUseCase {

    private final GoogleTokenVerifierPort googleTokenVerifier;
    private final UserRepository userRepository;
    private final RefreshTokenIssuerService refreshTokenIssuerService;
    private final StreakService streakService;

    public GoogleLoginUseCaseImpl(
            GoogleTokenVerifierPort googleTokenVerifier,
            UserRepository userRepository,
            RefreshTokenIssuerService refreshTokenIssuerService,
            StreakService streakService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.refreshTokenIssuerService = refreshTokenIssuerService;
        this.streakService = streakService;
    }

    @Override
    public LoginResult execute(GoogleLoginCommand command) throws AuthenticationException {
        GoogleUserInfo googleUser = googleTokenVerifier.verify(command.idToken());

        User user = userRepository.findByProviderId(googleUser.sub())
                .or(() -> linkExistingLocalAccount(googleUser))
                .orElseGet(() -> registerFromGoogle(googleUser));

        RefreshTokenResult tokens = refreshTokenIssuerService.issueFor(user);
        streakService.recordActivity(user.getId());

        return new LoginResult(tokens.accessToken(), tokens.refreshToken());
    }

    // An existing LOCAL account with the same, Google-verified email is linked
    // to this Google identity rather than creating a duplicate account.
    private java.util.Optional<User> linkExistingLocalAccount(GoogleUserInfo googleUser) {
        return userRepository.findByEmail(googleUser.email())
                .map(existing -> {
                    existing.setProvider(AuthProviderEnum.GOOGLE);
                    existing.setProviderId(googleUser.sub());
                    return userRepository.save(existing);
                });
    }

    private User registerFromGoogle(GoogleUserInfo googleUser) {
        User user = User.createFromGoogle(
                googleUser.name() != null ? googleUser.name() : googleUser.email(),
                googleUser.email(),
                googleUser.sub(),
                RoleEnum.USER
        );
        return userRepository.save(user);
    }
}
