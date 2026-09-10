package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.Pet;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.enums.PetSpecieEnum;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * A pet a user owns. Which app(s) it currently answers for is not a column
 * here — see {@link PetAppLinkJpaEntity} — a user can own several pets and
 * relink which one answers for a given app later (V34__pet_app_links.sql), so
 * {@code user} is many-to-one rather than a per-app unique relationship.
 */
@Entity
@Table(name = "jf_pets", schema = "pet")
public class PetJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Integer id;

    private String name;

    private int health;

    @Enumerated(EnumType.STRING)
    private PetSpecieEnum specie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Getter
    @Setter
    private UserJpaEntity user;

    public static PetJpaEntity fromDomain(Pet pet) {
        PetJpaEntity entity = new PetJpaEntity();
        entity.id = pet.getId();
        entity.name = pet.getName();
        entity.health = pet.getHealth();
        entity.specie = pet.getSpecie();
        return entity;
    }

    public Pet toDomain(User user) {
        Pet pet = new Pet();
        pet.setId(id != null ? id : 0);
        pet.setName(name);
        pet.setHealth(health);
        pet.setSpecie(specie);
        pet.setUser(user);
        return pet;
    }
}
