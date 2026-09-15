package com.jf.PetApp.application.user.port;

/**
 * Wipes every piece of data a user owns, across all bounded contexts, in one call. The two real
 * callers — account deletion and the demo-account reset — need the same guarantee (nothing left
 * behind) but for different reasons, so the erasure logic lives once, behind this port, instead
 * of being duplicated or reached into directly from {@code application}.
 *
 * @see com.jf.PetApp.infrastructure.repository.user.UserDataErasureAdapter for the coverage
 *      guarantee (every table with a {@code user_id} column must be accounted for here).
 */
public interface UserDataErasurePort {

    /**
     * @param userId id do utilizador; {@code email} porque o portfólio real ainda é indexado por
     *               e-mail, não por id.
     */
    void eraseAll(Long userId, String email);
}
