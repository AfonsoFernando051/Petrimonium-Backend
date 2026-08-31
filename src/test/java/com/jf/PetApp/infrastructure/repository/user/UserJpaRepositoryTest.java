package com.jf.PetApp.infrastructure.repository.user;

import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserJpaRepositoryTest {

    @Autowired
    private SpringUserJpaRepository jpaRepository;

    private UserRepository adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserJpaRepository(jpaRepository);
    }

    private User newUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRole(RoleEnum.USER);
        user.setActive(true);
        user.setHasAnsweredOnboarding(false);
        user.setPreferredLanguage("pt");
        return user;
    }

    @Test
    void save_ThenFindById_RoundTripsCoreFields() {
        User saved = adapter.save(newUser("investor", "investor@test.com"));

        Optional<User> found = adapter.findById(saved.getId().intValue());

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("investor");
        assertThat(found.get().getEmail()).isEqualTo("investor@test.com");
        assertThat(found.get().getPassword()).isEqualTo("hashed-password");
        assertThat(found.get().getRole()).isEqualTo(RoleEnum.USER);
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    void findById_WhenUnknown_ReturnsEmpty() {
        assertThat(adapter.findById(999)).isEmpty();
    }

    @Test
    void findByEmail_WhenUnknown_ReturnsEmpty() {
        assertThat(adapter.findByEmail("unknown@test.com")).isEmpty();
    }

    @Test
    void findByEmail_WhenKnown_ReturnsUser() {
        adapter.save(newUser("investor", "investor@test.com"));

        Optional<User> found = adapter.findByEmail("investor@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("investor");
    }

    @Test
    void findByUsername_WhenUnknown_ReturnsEmpty() {
        assertThat(adapter.findByUsername("unknown")).isEmpty();
    }

    @Test
    void findByUsername_WhenKnown_ReturnsUser() {
        adapter.save(newUser("investor", "investor@test.com"));

        Optional<User> found = adapter.findByUsername("investor");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("investor@test.com");
    }

    @Test
    void save_WithPet_PersistsAndReturnsPetMappedBackToTheSavedUser() {
        User user = newUser("investor", "investor@test.com");
        Pet pet = new Pet();
        pet.setName("Rex");
        pet.setHealth(100);
        pet.setSpecie(PetSpecieEnum.DOG);
        user.setPet(pet);

        User saved = adapter.save(user);

        assertThat(saved.getPet()).isNotNull();
        assertThat(saved.getPet().getName()).isEqualTo("Rex");
        assertThat(saved.getPet().getSpecie()).isEqualTo(PetSpecieEnum.DOG);
        assertThat(saved.getPet().getUser()).isSameAs(saved);
    }
}
