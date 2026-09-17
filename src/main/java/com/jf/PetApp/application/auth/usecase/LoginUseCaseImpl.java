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

    /**
     * Verified against when the submitted identifier matches no account, or matches one with no
     * local password at all (a Google-created account — see {@code User.createFromGoogle}).
     *
     * <p>Without it, a miss returns before any hashing happens while a hit pays a full BCrypt
     * verification, and that difference is measurable from outside: it turns login into an
     * account-enumeration oracle, telling an attacker which emails are registered here — the
     * exact leak {@code RequestPasswordResetUseCaseImpl} goes out of its way to avoid on the
     * forgot-password path. Hashing against this constant makes both paths cost the same.
     *
     * <p>It is a real cost-10 BCrypt hash of 32 random bytes that were discarded on generation,
     * so no input can match it — and {@code execute} still refuses on "user was not found"
     * independently of what {@code matches} returns, so nothing rests on that alone. Generated
     * with the same encoder the adapter uses, so it stays cost-comparable to a genuine hit; if
     * {@code BCryptPasswordEncoderAdapter}'s strength is ever raised, regenerate this to match
     * or the equalization silently stops holding.
     */
    static final String NO_SUCH_USER_PASSWORD_HASH =
            "$2a$10$rV.sXT/isyiZVYCPWJE87eGCILecq6NHbIUAMA2zInyLstk4H4nIq";

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
                .orElse(null);

        // Both checks are evaluated before either can reject: the encoder is called even on a
        // miss (see NO_SUCH_USER_PASSWORD_HASH), and the result is only read afterwards, so
        // neither `user == null` nor a short-circuit can skip the hashing work.
        String storedHash = (user != null && user.getPassword() != null)
                ? user.getPassword()
                : NO_SUCH_USER_PASSWORD_HASH;
        boolean passwordMatches = passwordEncoder.matches(command.password(), storedHash);

        if (user == null || !passwordMatches) {
            throw new AuthenticationException();
        }

        demoAccountResetPort.resetIfDemoAccount(user.getUsername());

        RefreshTokenResult tokens = refreshTokenIssuerService.issueFor(user, command.appContext());
        streakService.recordActivity(user.getId());

        return new LoginResult(tokens.accessToken(), tokens.refreshToken());
    }
}