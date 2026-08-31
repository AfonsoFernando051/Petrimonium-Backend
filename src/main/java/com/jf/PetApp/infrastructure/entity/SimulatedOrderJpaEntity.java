package com.jf.PetApp.infrastructure.entity;

import com.jf.PetApp.core.domain.enums.SimulatedOrderSide;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "simulated_orders", schema = "simulated_portfolio")
public class SimulatedOrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private SimulatedPortfolioJpaEntity portfolio;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private SimulatedOrderSide side;

    @Column(precision = 19, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "client_order_id", nullable = false, length = 100)
    private String clientOrderId;

    public Long getId() {
        return id;
    }

    public SimulatedPortfolioJpaEntity getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(SimulatedPortfolioJpaEntity portfolio) {
        this.portfolio = portfolio;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public SimulatedOrderSide getSide() {
        return side;
    }

    public void setSide(SimulatedOrderSide side) {
        this.side = side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }
}
