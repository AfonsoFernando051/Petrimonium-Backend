package com.jf.PetApp.infrastructure.repository.pet;

import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AppContextEnum;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import com.jf.PetApp.infrastructure.entity.PetAppLinkJpaEntity;
import com.jf.PetApp.infrastructure.entity.PetJpaEntity;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.PetRepository;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @Import, not a hand-built `new PetRepositoryAdapter(...)` like the other adapter tests in this
// package use: this test specifically exercises the class's own @Transactional boundary (see the
// test method's doc comment), which only exists on the Spring-managed AOP proxy. A manually
// constructed instance would silently skip that proxy and the test would pass even with the
// annotation removed — pointless as a regression test for the exact bug it's guarding against.
@DataJpaTest
@Import(PetRepositoryAdapter.class)
class PetRepositoryAdapterTest {

    @Autowired
    private PetAppLinkRepository linkRepository;

    @Autowired
    private PetRepository petJpaRepository;

    @Autowired
    private SpringUserJpaRepository userJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PetRepositoryAdapter adapter;

    private static final String EMAIL = "pet-owner@test.com";

    /**
     * Regression test for the LazyInitializationException a real HTTP request hits (see
     * {@code GetMentorReplyUseCaseImpl -> GetMyPetUseCaseImpl -> this method}):
     * {@code PetAppLinkJpaEntity.pet} became {@code FetchType.LAZY}, so {@code link.getPet()} now
     * returns a Hibernate proxy, and this method had no {@code @Transactional} of its own — it
     * only ever worked because the old EAGER default papered over the missing boundary.
     * {@code @DataJpaTest} wraps the whole test method in one transaction by default, which would
     * hide this bug entirely (the proxy would always find a session), so {@link TestTransaction#end()}
     * below ends that wrapping transaction before calling the method under test — reproducing the
     * same "no active session" gap a real controller request hits once each repository call gets
     * its own short-lived transaction.
     */
    @Test
    void findByUserIdAndAppContext_WhenCalledWithNoActiveTransaction_StillInitializesTheLazyPetProxy() {
        User user = new User();
        user.setUsername("pet-owner");
        user.setEmail(EMAIL);
        user.setPassword("hash");
        UserJpaEntity savedUser = userJpaRepository.save(UserJpaEntity.fromDomain(user));

        Pet pet = new Pet();
        pet.setName("Rex");
        pet.setSpecie(PetSpecieEnum.CAT);
        pet.setHealth(100);
        PetJpaEntity petEntity = PetJpaEntity.fromDomain(pet);
        petEntity.setUser(savedUser);
        PetJpaEntity savedPet = petJpaRepository.save(petEntity);

        PetAppLinkJpaEntity link = new PetAppLinkJpaEntity();
        link.setUser(savedUser);
        link.setPet(savedPet);
        link.setAppContext(AppContextEnum.WALLET);
        linkRepository.save(link);

        entityManager.flush();
        // Evicts the persistence context so link.getPet() below really returns a lazy proxy
        // instead of the instance this test just saved (which Hibernate would otherwise keep
        // resolved in its first-level cache).
        entityManager.clear();

        // @DataJpaTest defaults every test transaction to roll back — flag this one for commit
        // first, or TestTransaction.end() below would silently wipe the rows just saved and the
        // adapter call would just see an empty table instead of exercising the lazy proxy at all.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        Optional<Pet> found = adapter.findByUserIdAndAppContext(savedUser.getId(), AppContextEnum.WALLET);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Rex");

        // Lets @DataJpaTest's rollback machinery clean up normally instead of leaving the test
        // transaction ended.
        TestTransaction.start();
    }
}
