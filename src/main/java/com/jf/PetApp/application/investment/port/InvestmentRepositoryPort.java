package com.jf.PetApp.application.investment.port;

import com.jf.PetApp.core.domain.Investment;

import java.util.List;

/**
 * Application-layer boundary for investment persistence. Use cases depend on
 * this port, never on Spring Data or JPA entities directly — the adapter in
 * {@code infrastructure.repository.investment} is the only place that knows
 * how investments are actually stored.
 */
public interface InvestmentRepositoryPort {

    List<Investment> findByUserEmail(String email);

    void deleteByUserEmail(String email);

    /**
     * Replaces nothing by itself — callers are expected to pair this with
     * {@link #deleteByUserEmail(String)} inside a transaction, mirroring the
     * "replace the whole portfolio" semantics of the configure-investments
     * use case. {@code investments} elements carry a {@code null} id; the
     * adapter assigns real ids on save.
     */
    void saveAll(String userEmail, List<Investment> investments);

    /** Appends one lot. {@code investment.id()} must be {@code null}; the adapter assigns it. */
    Investment create(String userEmail, Investment investment);

    /**
     * Updates a lot in place, preserving its {@code createdAt}. Throws
     * {@link com.jf.PetApp.application.common.exception.ResourceNotFoundException} when
     * {@code id} doesn't exist or doesn't belong to {@code userEmail} — the two cases are
     * indistinguishable on purpose, so an id from another user is reported as "not found",
     * never "forbidden".
     */
    Investment update(Integer id, String userEmail, Investment investment);

    /**
     * Removes one lot. Same "not found, never forbidden" scoping and exception as
     * {@link #update(Integer, String, Investment)}.
     */
    void delete(Integer id, String userEmail);
}
