package com.jf.PetApp.application.settings.usecase;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.repository.user.UserDataEraser;

/**
 * Exclusão definitiva, sem período de carência: o utilizador pede e os dados
 * saem. Tudo numa transacção — se qualquer contexto falhar, nada é apagado,
 * em vez de deixar a conta meio removida.
 *
 * <p>Os tokens de sessão saem junto com o resto, por isso o pedido seguinte
 * do mesmo cliente responde 401. Isso é o comportamento certo e não precisa
 * de logout explícito do lado da app.
 */
@Service
public class DeleteAccountUseCaseImpl implements DeleteAccountUseCase {

    private final UserRepository userRepository;
    private final UserDataEraser userDataEraser;

    public DeleteAccountUseCaseImpl(UserRepository userRepository, UserDataEraser userDataEraser) {
        this.userRepository = userRepository;
        this.userDataEraser = userDataEraser;
    }

    @Override
    @Transactional
    public void execute(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        userDataEraser.eraseAll(user.getId(), user.getEmail());
        // jf_finances e jf_pets saem por cascata do UserJpaEntity.
        userRepository.delete(user);
    }
}
