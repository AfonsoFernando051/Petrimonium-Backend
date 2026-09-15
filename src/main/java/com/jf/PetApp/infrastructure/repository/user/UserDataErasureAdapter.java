package com.jf.PetApp.infrastructure.repository.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.health.port.HealthStore;
import com.jf.PetApp.infrastructure.entity.SimulatedPortfolioJpaEntity;
import com.jf.PetApp.infrastructure.repository.InvestmentRepository;
import com.jf.PetApp.infrastructure.repository.PetRepository;
import com.jf.PetApp.infrastructure.repository.SimulatedOrderRepository;
import com.jf.PetApp.infrastructure.repository.SimulatedPortfolioRepository;
import com.jf.PetApp.infrastructure.repository.SimulatedPositionRepository;
import com.jf.PetApp.infrastructure.repository.gamification.AchievementUnlockJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.ActivityLogJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.MissionCompletionJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.XpEventJpaRepository;
import com.jf.PetApp.infrastructure.repository.learning.LessonProgressJpaRepository;
import com.jf.PetApp.infrastructure.repository.pet.PetAppLinkRepository;
import com.jf.PetApp.infrastructure.repository.mentor.SpringMentorConversationJpaRepository;

/**
 * Apaga tudo o que pertence a um utilizador, em todos os contextos.
 *
 * <p>Um só sítio, usado por dois chamadores com intenções diferentes: a
 * exclusão de conta (que a seguir apaga a própria linha do utilizador) e o
 * reset da conta de demonstração (que a seguir a devolve ao estado de
 * inscrição). Antes de existir, o reset do demo apagava sete das vinte e uma
 * tabelas e ia acumulando finanças, pet, portfólios simulados, tokens e todo
 * o Health entre sessões, ao contrário do que prometia.
 *
 * <p><b>Uma tabela não aparece aqui de propósito:</b> {@code jf_finances} é
 * {@code @OneToOne(cascade = ALL, orphanRemoval = true)} no
 * {@link com.jf.PetApp.infrastructure.entity.UserJpaEntity}, por isso sai com
 * o utilizador — e o reset do demo trata-a via {@code resetToFreshSignupState}.
 * {@code jf_pets} e {@code jf_pet_app_links} já não são assim: desde que um
 * pet passou a poder responder por mais de uma app e a mudar de app ao longo
 * do tempo, deixaram de ser associação {@code @OneToOne} e passam a ser
 * apagados aqui, explicitamente — os links primeiro, por causa da FK para
 * {@code jf_pets}. {@code jf_mentor_messages} sai pela
 * {@code on delete cascade} da FK para as conversas.
 *
 * <p>A cobertura está travada por {@code UserDataErasureCoverageTest}: se
 * alguém criar uma tabela com {@code user_id} e não a tratar aqui, o teste
 * falha.
 */
@Component
public class UserDataEraser {

    private final InvestmentRepository investmentRepository;
    private final PetRepository petRepository;
    private final PetAppLinkRepository petAppLinkRepository;
    private final LessonProgressJpaRepository lessonProgressRepository;
    private final XpEventJpaRepository xpEventRepository;
    private final AchievementUnlockJpaRepository achievementUnlockRepository;
    private final ActivityLogJpaRepository activityLogRepository;
    private final MissionCompletionJpaRepository missionCompletionRepository;
    private final SpringMentorConversationJpaRepository mentorConversationRepository;
    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final PasswordResetTokenJpaRepository passwordResetTokenRepository;
    private final SimulatedPortfolioRepository simulatedPortfolioRepository;
    private final SimulatedOrderRepository simulatedOrderRepository;
    private final SimulatedPositionRepository simulatedPositionRepository;
    private final HealthStore healthStore;

    public UserDataEraser(
            InvestmentRepository investmentRepository,
            PetRepository petRepository,
            PetAppLinkRepository petAppLinkRepository,
            LessonProgressJpaRepository lessonProgressRepository,
            XpEventJpaRepository xpEventRepository,
            AchievementUnlockJpaRepository achievementUnlockRepository,
            ActivityLogJpaRepository activityLogRepository,
            MissionCompletionJpaRepository missionCompletionRepository,
            SpringMentorConversationJpaRepository mentorConversationRepository,
            RefreshTokenJpaRepository refreshTokenRepository,
            PasswordResetTokenJpaRepository passwordResetTokenRepository,
            SimulatedPortfolioRepository simulatedPortfolioRepository,
            SimulatedOrderRepository simulatedOrderRepository,
            SimulatedPositionRepository simulatedPositionRepository,
            HealthStore healthStore) {
        this.investmentRepository = investmentRepository;
        this.petRepository = petRepository;
        this.petAppLinkRepository = petAppLinkRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.xpEventRepository = xpEventRepository;
        this.achievementUnlockRepository = achievementUnlockRepository;
        this.activityLogRepository = activityLogRepository;
        this.missionCompletionRepository = missionCompletionRepository;
        this.mentorConversationRepository = mentorConversationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.simulatedPortfolioRepository = simulatedPortfolioRepository;
        this.simulatedOrderRepository = simulatedOrderRepository;
        this.simulatedPositionRepository = simulatedPositionRepository;
        this.healthStore = healthStore;
    }

    /**
     * @param userId id do utilizador; {@code email} porque o portfólio real
     *               ainda é indexado por e-mail, não por id.
     */
    @Transactional
    public void eraseAll(Long userId, String email) {
        // Health primeiro: é o único contexto fora do JPA e tem a cadeia de
        // FKs mais funda, resolvida internamente pelo JdbcHealthStore.
        healthStore.deleteAllForUser(userId);

        // Simulado: filhos antes do portfólio que os referencia.
        simulatedPortfolioRepository.findByUser_Id(userId).ifPresent(this::erasePortfolio);

        investmentRepository.deleteByUserEmail(email);
        petAppLinkRepository.deleteByUser_Id(userId);
        petRepository.deleteByUser_Id(userId);
        lessonProgressRepository.deleteByUserId(userId);
        xpEventRepository.deleteByUserId(userId);
        achievementUnlockRepository.deleteByUserId(userId);
        activityLogRepository.deleteByUserId(userId);
        missionCompletionRepository.deleteByUserId(userId);
        mentorConversationRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteByUserId(userId);
        passwordResetTokenRepository.deleteByUserId(userId);
    }

    private void erasePortfolio(SimulatedPortfolioJpaEntity portfolio) {
        simulatedOrderRepository.deleteByPortfolioId(portfolio.getId());
        simulatedPositionRepository.deleteByPortfolioId(portfolio.getId());
        simulatedPortfolioRepository.delete(portfolio);
    }
}
