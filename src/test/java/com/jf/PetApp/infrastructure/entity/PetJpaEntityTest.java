package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PetJpaEntityTest {

    private Pet aPet() {
        Pet pet = new Pet();
        pet.setId(3);
        pet.setName("Rex");
        pet.setHealth(80);
        pet.setSpecie(PetSpecieEnum.DOG);
        return pet;
    }

    @Test
    void fromDomain_MapsAllFieldsButNotUser() {
        PetJpaEntity entity = PetJpaEntity.fromDomain(aPet());

        Pet result = entity.toDomain(null);

        assertThat(result.getId()).isEqualTo(3);
        assertThat(result.getName()).isEqualTo("Rex");
        assertThat(result.getHealth()).isEqualTo(80);
        assertThat(result.getSpecie()).isEqualTo(PetSpecieEnum.DOG);
    }

    @Test
    void toDomain_AssignsGivenUser() {
        PetJpaEntity entity = PetJpaEntity.fromDomain(aPet());
        User user = new User();

        Pet result = entity.toDomain(user);

        assertThat(result.getUser()).isSameAs(user);
    }

    @Test
    void toDomain_WhenIdIsNull_DefaultsToZeroRatherThanNull() {
        Pet withoutId = new Pet();
        withoutId.setName("Unsaved");
        withoutId.setHealth(50);
        withoutId.setSpecie(PetSpecieEnum.CAT);
        PetJpaEntity entity = PetJpaEntity.fromDomain(withoutId);

        Pet result = entity.toDomain(new User());

        assertThat(result.getId()).isZero();
    }
}
