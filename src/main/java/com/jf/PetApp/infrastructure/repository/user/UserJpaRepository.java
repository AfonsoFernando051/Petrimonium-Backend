package com.jf.PetApp.infrastructure.repository.user;

import java.util.Optional;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;

import org.springframework.stereotype.Repository;

@Repository
public class UserJpaRepository implements UserRepository {

    private final SpringUserJpaRepository jpa;

    public UserJpaRepository(SpringUserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(int id) {
        return jpa.findById(id)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findByProviderId(String providerId) {
        return jpa.findByProviderId(providerId)
                .map(UserJpaEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);
        jpa.save(entity);
        return entity.toDomain();
    }
}
