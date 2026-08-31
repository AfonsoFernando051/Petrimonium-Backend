package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.enums.InvestmentType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name = "jf_investments")
public class InvestmentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private Double quantity;

    @Column(name = "purchase_price")
    private Double purchasePrice;

    @Column(name = "purchase_date")
    private java.time.LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    private InvestmentType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public java.time.LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(java.time.LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public InvestmentType getType() {
        return type;
    }

    public void setType(InvestmentType type) {
        this.type = type;
    }

    public UserJpaEntity getUser() {
        return user;
    }

    public void setUser(UserJpaEntity user) {
        this.user = user;
    }
}
