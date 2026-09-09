package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.enums.AppContextEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Which pet currently answers for which app, per user (V34__pet_app_links.sql).
 * A pet can hold several of these at once (linked to more than one app); the
 * {@code (user, app_context)} unique constraint is what actually matters —
 * at most one pet answers for a given app at a time, and relinking a
 * different pet to that app replaces this row rather than adding another.
 */
@Entity
@Table(name = "jf_pet_app_links", schema = "pet",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "app_context"}))
public class PetAppLinkJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private PetJpaEntity pet;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_context", nullable = false)
    private AppContextEnum appContext;

    public Integer getId() {
        return id;
    }

    public PetJpaEntity getPet() {
        return pet;
    }

    public void setPet(PetJpaEntity pet) {
        this.pet = pet;
    }

    public UserJpaEntity getUser() {
        return user;
    }

    public void setUser(UserJpaEntity user) {
        this.user = user;
    }

    public AppContextEnum getAppContext() {
        return appContext;
    }

    public void setAppContext(AppContextEnum appContext) {
        this.appContext = appContext;
    }
}
