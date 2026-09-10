package com.jf.PetApp.infrastructure.repository.investment;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.investment.port.InvestmentRepositoryPort;
import com.jf.PetApp.core.domain.Investment;
import com.jf.PetApp.infrastructure.entity.InvestmentJpaEntity;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.InvestmentRepository;
import com.jf.PetApp.infrastructure.repository.user.SpringUserJpaRepository;

/**
 * The only place in the codebase that knows investments are stored as JPA
 * entities. Implements {@link InvestmentRepositoryPort} so every use case
 * upstream works with the plain {@link Investment} domain record instead.
 */
@Repository
public class InvestmentRepositoryAdapter implements InvestmentRepositoryPort {

    private final InvestmentRepository investmentRepository;
    private final SpringUserJpaRepository userJpaRepository;

    public InvestmentRepositoryAdapter(InvestmentRepository investmentRepository, SpringUserJpaRepository userJpaRepository) {
        this.investmentRepository = investmentRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public List<Investment> findByUserEmail(String email) {
        return investmentRepository.findByUser_Email(email).stream()
                .map(entity -> toDomain(entity, email))
                .toList();
    }

    @Override
    @Transactional
    public void deleteByUserEmail(String email) {
        investmentRepository.deleteByUserEmail(email);
    }

    @Override
    @Transactional
    public void saveAll(String userEmail, List<Investment> investments) {
        UserJpaEntity user = userJpaRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + userEmail));

        List<InvestmentJpaEntity> entities = investments.stream()
                .map(investment -> toEntity(investment, user))
                .toList();

        investmentRepository.saveAll(entities);
    }

    @Override
    @Transactional
    public Investment create(String userEmail, Investment investment) {
        UserJpaEntity user = userJpaRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found for email: " + userEmail));

        InvestmentJpaEntity saved = investmentRepository.save(toEntity(investment, user));
        return toDomain(saved, userEmail);
    }

    @Override
    @Transactional
    public Investment update(Integer id, String userEmail, Investment investment) {
        InvestmentJpaEntity entity = ownedOrThrow(id, userEmail);
        entity.setName(investment.name());
        entity.setQuantity(investment.quantity());
        entity.setPurchasePrice(investment.purchasePrice());
        entity.setPurchaseDate(investment.purchaseDate());
        entity.setType(investment.type());
        entity.setUpdatedAt(Instant.now());
        return toDomain(investmentRepository.save(entity), userEmail);
    }

    /**
     * Resolves a lot only when it belongs to {@code userEmail}. Anything else is reported as
     * missing rather than forbidden, so an id cannot be used to probe for the existence of
     * another user's lot — same pattern as {@code MentorConversationRepositoryAdapter}.
     */
    private InvestmentJpaEntity ownedOrThrow(Integer id, String userEmail) {
        return investmentRepository.findByIdAndUser_Email(id, userEmail)
                .orElseThrow(() -> new com.jf.PetApp.application.common.exception.ResourceNotFoundException(
                        "Investment not found: " + id));
    }

    private Investment toDomain(InvestmentJpaEntity entity, String userEmail) {
        return new Investment(
                entity.getId(),
                userEmail,
                entity.getName(),
                entity.getQuantity(),
                entity.getPurchasePrice(),
                entity.getPurchaseDate(),
                entity.getType()
        );
    }

    private InvestmentJpaEntity toEntity(Investment investment, UserJpaEntity user) {
        InvestmentJpaEntity entity = new InvestmentJpaEntity();
        entity.setUser(user);
        entity.setName(investment.name());
        entity.setQuantity(investment.quantity());
        entity.setPurchasePrice(investment.purchasePrice());
        entity.setPurchaseDate(investment.purchaseDate());
        entity.setType(investment.type());
        // Only used for brand-new rows (create, and saveAll's always-delete-then-insert), so
        // created_at and updated_at start identical — #update() is the only path that changes
        // updated_at afterwards, and it never calls this method.
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
