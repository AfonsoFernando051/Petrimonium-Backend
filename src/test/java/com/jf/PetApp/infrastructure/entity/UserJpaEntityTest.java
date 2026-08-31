package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserJpaEntityTest {

    private User aUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("investor");
        user.setEmail("investor@test.com");
        user.setPassword("hash");
        user.setRole(RoleEnum.USER);
        user.setActive(true);
        user.setHasAnsweredOnboarding(true);
        user.setInvestorProfile(InvestorProfile.TACTICIAN);
        user.setPreferredLanguage("en");
        return user;
    }

    @Test
    void fromDomainThenToDomain_RoundTripsEveryScalarField() {
        UserJpaEntity entity = UserJpaEntity.fromDomain(aUser());

        User result = entity.toDomain();

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("investor");
        assertThat(result.getEmail()).isEqualTo("investor@test.com");
        assertThat(result.getPassword()).isEqualTo("hash");
        assertThat(result.getRole()).isEqualTo(RoleEnum.USER);
        assertThat(result.isActive()).isTrue();
        assertThat(result.hasAnsweredOnboarding()).isTrue();
        assertThat(result.getInvestorProfile()).isEqualTo(InvestorProfile.TACTICIAN);
        assertThat(result.getPreferredLanguage()).isEqualTo("en");
    }

    @Test
    void fromDomain_WhenUserHasNoPet_LeavesEntityPetNull() {
        UserJpaEntity entity = UserJpaEntity.fromDomain(aUser());

        assertThat(entity.toDomain().getPet()).isNull();
    }

    @Test
    void toDomain_WhenPetPresent_MapsPetBackWithThisUserAttached() {
        User user = aUser();
        Pet pet = new Pet();
        pet.setName("Rex");
        pet.setHealth(100);
        pet.setSpecie(PetSpecieEnum.DOG);
        user.setPet(pet);
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);

        User result = entity.toDomain();

        assertThat(result.getPet()).isNotNull();
        assertThat(result.getPet().getName()).isEqualTo("Rex");
        assertThat(result.getPet().getUser()).isSameAs(result);
    }

    @Test
    void resetToFreshSignupState_ClearsOnboardingAndDropsPet() {
        User user = aUser();
        Pet pet = new Pet();
        pet.setName("Rex");
        pet.setHealth(100);
        pet.setSpecie(PetSpecieEnum.DOG);
        user.setPet(pet);
        UserJpaEntity entity = UserJpaEntity.fromDomain(user);

        entity.resetToFreshSignupState();
        User result = entity.toDomain();

        assertThat(result.hasAnsweredOnboarding()).isFalse();
        assertThat(result.getInvestorProfile()).isNull();
        assertThat(result.getPet()).isNull();
    }
}
