package com.jf.PetApp.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.jf.PetApp.core.domain.enums.PetSpecieEnum;

/**
 * Pet is a plain getter/setter holder with no custom behavior — this just
 * confirms the accessors round-trip correctly.
 */
class PetTest {

    @Test
    void settersAndGetters_RoundTripEveryField() {
        Pet pet = new Pet();
        User owner = new User();

        pet.setId(3);
        pet.setName("Rex");
        pet.setSpecie(PetSpecieEnum.DOG);
        pet.setHealth(80);
        pet.setUser(owner);

        assertEquals(3, pet.getId());
        assertEquals("Rex", pet.getName());
        assertEquals(PetSpecieEnum.DOG, pet.getSpecie());
        assertEquals(80, pet.getHealth());
        assertEquals(owner, pet.getUser());
    }
}
