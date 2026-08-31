package com.jf.PetApp.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.enums.RoleEnum;

class UserTest {

    @Test
    void create_SetsProvidedFieldsAndSensibleDefaults() {
        User user = User.create("investor", "investor@test.com", "secret", RoleEnum.USER);

        assertEquals("investor", user.getUsername());
        assertEquals("investor@test.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals(RoleEnum.USER, user.getRole());
        assertTrue(user.isActive());
        assertFalse(user.hasAnsweredOnboarding());
        assertNull(user.getInvestorProfile());
        assertEquals("pt", user.getPreferredLanguage());
    }

    @Test
    void create_AdminRole_IsPreservedAsGiven() {
        User user = User.create("admin", "admin@test.com", "secret", RoleEnum.ADMIN);

        assertEquals(RoleEnum.ADMIN, user.getRole());
    }

    @Test
    void newInstance_DefaultPreferredLanguage_IsPt() {
        User user = new User();

        assertEquals("pt", user.getPreferredLanguage());
    }

    @Test
    void settersAndGetters_RoundTripEveryField() {
        User user = new User();
        Pet pet = new Pet();
        Finance finance = new Finance();

        user.setId(5L);
        user.setUsername("name");
        user.setEmail("email@test.com");
        user.setPassword("pw");
        user.setPet(pet);
        user.setFinance(finance);
        user.setRole(RoleEnum.ADMIN);
        user.setActive(false);
        user.setHasAnsweredOnboarding(true);
        user.setInvestorProfile(InvestorProfile.TACTICIAN);
        user.setPreferredLanguage("en");

        assertEquals(5L, user.getId());
        assertEquals("name", user.getUsername());
        assertEquals("email@test.com", user.getEmail());
        assertEquals("pw", user.getPassword());
        assertEquals(pet, user.getPet());
        assertEquals(finance, user.getFinance());
        assertEquals(RoleEnum.ADMIN, user.getRole());
        assertFalse(user.isActive());
        assertTrue(user.hasAnsweredOnboarding());
        assertEquals(InvestorProfile.TACTICIAN, user.getInvestorProfile());
        assertEquals("en", user.getPreferredLanguage());
    }
}
