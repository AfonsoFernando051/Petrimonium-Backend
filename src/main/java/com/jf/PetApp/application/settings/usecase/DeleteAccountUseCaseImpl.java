package com.jf.PetApp.application.settings.usecase;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.port.GoogleTokenVerifierPort;
import com.jf.PetApp.application.auth.port.GoogleUserInfo;
import com.jf.PetApp.application.auth.port.PasswordEncoderPort;
import com.jf.PetApp.application.settings.dto.DeleteAccountCommand;
import com.jf.PetApp.application.user.port.UserDataErasurePort;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;

/**
 * Exclusão definitiva, sem período de carência: o utilizador pede e os dados
 * saem. Tudo numa transacção — se qualquer contexto falhar, nada é apagado,
 * em vez de deixar a conta meio removida.
 *
 * <p>Os tokens de sessão saem junto com o resto, por isso o pedido seguinte
 * do mesmo cliente responde 401. Isso é o comportamento certo e não precisa
 * de logout explícito do lado da app.
 *
 * <p>O bearer token sozinho não basta para chegar aqui: o pedido tem de trazer a senha atual
 * ou um ID token Google fresco da mesma identidade. Antes disso, a única operação irreversível
 * e sem carência do produto estava a uma janela de uma hora de um access token roubado —
 * uma barreira mais baixa do que substituir a carteira, que já exigia confirmação explícita.
 */
@Service
public class DeleteAccountUseCaseImpl implements DeleteAccountUseCase {

    private final UserRepository userRepository;
    private final UserDataErasurePort userDataEraser;
    private final PasswordEncoderPort passwordEncoder;
    private final GoogleTokenVerifierPort googleTokenVerifier;

    public DeleteAccountUseCaseImpl(UserRepository userRepository,
                                    UserDataErasurePort userDataEraser,
                                    PasswordEncoderPort passwordEncoder,
                                    GoogleTokenVerifierPort googleTokenVerifier) {
        this.userRepository = userRepository;
        this.userDataEraser = userDataEraser;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Override
    @Transactional
    public void execute(DeleteAccountCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        requireReauthentication(user, command);

        userDataEraser.eraseAll(user.getId(), user.getEmail());
        // jf_finances e jf_pets saem por cascata do UserJpaEntity.
        userRepository.delete(user);
    }

    /**
     * Aceita a credencial que corresponde à forma como esta conta entra: senha, para quem tem
     * uma; ID token Google, para uma conta criada pelo Google ({@code password == null}, ver
     * {@code User.createFromGoogle}). Uma conta local mais tarde ligada ao Google mantém a
     * senha, por isso serve-lhe qualquer uma das duas.
     */
    private void requireReauthentication(User user, DeleteAccountCommand command) {
        if (hasText(command.googleIdToken())) {
            verifyGoogleIdentity(user, command.googleIdToken());
            return;
        }

        if (hasText(command.password()) && user.getPassword() != null) {
            if (!passwordEncoder.matches(command.password(), user.getPassword())) {
                throw new AuthenticationException("Credenciais inválidas");
            }
            return;
        }

        throw new AuthenticationException("Credenciais inválidas");
    }

    /**
     * Um token válido prova que quem o traz é dono de <em>alguma</em> conta Google, não desta.
     * Sem amarrar a identidade verificada à conta a apagar, bastaria a um atacante juntar o seu
     * próprio ID token a um bearer token roubado para apagar os dados de outra pessoa.
     */
    private void verifyGoogleIdentity(User user, String idToken) {
        GoogleUserInfo verified = googleTokenVerifier.verify(idToken);

        boolean sameGoogleAccount = user.getProviderId() != null
                ? Objects.equals(user.getProviderId(), verified.sub())
                : Objects.equals(user.getEmail(), verified.email());

        if (!sameGoogleAccount) {
            throw new AuthenticationException("Credenciais inválidas");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
