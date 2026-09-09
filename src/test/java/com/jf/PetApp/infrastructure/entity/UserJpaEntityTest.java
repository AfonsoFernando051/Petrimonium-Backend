package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
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
    void resetToFreshSignupState_ClearsOnboardingState() {
        UserJpaEntity entity = UserJpaEntity.fromDomain(aUser());

        entity.resetToFreshSignupState();
        User result = entity.toDomain();

        assertThat(result.hasAnsweredOnboarding()).isFalse();
        assertThat(result.getInvestorProfile()).isNull();
    }
}
