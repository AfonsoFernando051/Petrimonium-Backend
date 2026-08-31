package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.AuthProviderEnum;
import com.jf.PetApp.core.domain.enums.RoleEnum;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "jf_users", schema = "identity")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long id;

    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    private boolean isActive;

    @Column(name = "has_answered_onboarding")
    private boolean hasAnsweredOnboarding;

    @Enumerated(EnumType.STRING)
    @Column(name = "investor_profile")
    private InvestorProfile investorProfile;

    @Column(name = "preferred_language")
    private String preferredLanguage;

    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    @Enumerated(EnumType.STRING)
    private AuthProviderEnum provider;

    @Column(name = "provider_id")
    private String providerId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private FinanceJpaEntity finance;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private PetJpaEntity pet;

    /* ---------- Mapping ---------- */

    public static UserJpaEntity fromDomain(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = user.getId();
        entity.username = user.getUsername();
        entity.email = user.getEmail();
        entity.password = user.getPassword();
        entity.role = user.getRole();
        entity.isActive = user.isActive();
        entity.hasAnsweredOnboarding = user.hasAnsweredOnboarding();
        entity.investorProfile = user.getInvestorProfile();
        entity.preferredLanguage = user.getPreferredLanguage();
        entity.provider = user.getProvider();
        entity.providerId = user.getProviderId();

        if (user.getPet() != null) {
            entity.pet = PetJpaEntity.fromDomain(user.getPet());
            entity.pet.setUser(entity);
        }
        
        return entity;
    }

    public User toDomain() {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setActive(isActive);
        user.setHasAnsweredOnboarding(hasAnsweredOnboarding);
        user.setInvestorProfile(investorProfile);
        user.setPreferredLanguage(preferredLanguage);
        user.setProvider(provider);
        user.setProviderId(providerId);

        if (pet != null) {
            user.setPet(pet.toDomain(user));
        }

        return user;
    }

    /**
     * Reverts this row to the same shape a genuinely brand-new signup has
     * (see RegisterUserUseCaseImpl / V5__seed_admin2_user.sql): onboarding
     * not done, no investor profile, no pet or finance row. orphanRemoval on
     * the pet/finance associations deletes the previous rows on save.
     */
    public void resetToFreshSignupState() {
        this.hasAnsweredOnboarding = false;
        this.investorProfile = null;
        this.pet = null;
        this.finance = null;
    }
}
