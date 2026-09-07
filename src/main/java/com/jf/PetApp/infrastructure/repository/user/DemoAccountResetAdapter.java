package com.jf.PetApp.infrastructure.repository.user;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.user.port.DemoAccountResetPort;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;

/**
 * Usernames listed here are wiped back to a brand-new-signup state on every
 * login (see UserJpaEntity#resetToFreshSignupState) — currently just the
 * admin2 demo account (V5__seed_admin2_user.sql), used to exercise the
 * onboarding/empty-portfolio flows and expected to never accumulate
 * progress across sessions. admin3 (V17__seed_admin3_user.sql) is
 * deliberately NOT in this set — it's a plain USER-role account meant to
 * behave like a real user and keep whatever state a test session leaves it
 * in across logins.
 */
@Repository
public class DemoAccountResetAdapter implements DemoAccountResetPort {

    private static final Set<String> DEMO_USERNAMES = Set.of("admin2");

    private final SpringUserJpaRepository userJpaRepository;
    private final UserDataEraser userDataEraser;

    public DemoAccountResetAdapter(SpringUserJpaRepository userJpaRepository, UserDataEraser userDataEraser) {
        this.userJpaRepository = userJpaRepository;
        this.userDataEraser = userDataEraser;
    }

    @Override
    @Transactional
    public void resetIfDemoAccount(String username) {
        if (!DEMO_USERNAMES.contains(username)) {
            return;
        }

        Optional<UserJpaEntity> userOpt = userJpaRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }

        UserJpaEntity user = userOpt.get();
        User domainUser = user.toDomain();
        Long userId = domainUser.getId();

        // Uma chamada em vez de sete: antes isto cobria só metade das
        // tabelas e a conta de demonstração ia acumulando o resto.
        userDataEraser.eraseAll(userId, domainUser.getEmail());

        user.resetToFreshSignupState();
        userJpaRepository.save(user);
    }
}
