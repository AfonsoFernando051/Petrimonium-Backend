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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "jf_pets")
public class PetJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private int health;
  

    @Enumerated(EnumType.STRING)
    private PetSpecieEnum specie;
    
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserJpaEntity user;

    public void setUser(UserJpaEntity user) {
        this.user = user;
    }

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
