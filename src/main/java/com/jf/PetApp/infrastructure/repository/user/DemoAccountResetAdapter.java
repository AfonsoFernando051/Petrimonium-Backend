package com.jf.PetApp.infrastructure.repository.user;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jf.PetApp.application.user.port.DemoAccountResetPort;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.repository.InvestmentRepository;
import com.jf.PetApp.infrastructure.repository.gamification.AchievementUnlockJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.ActivityLogJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.MissionCompletionJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.XpEventJpaRepository;
import com.jf.PetApp.infrastructure.repository.learning.LessonProgressJpaRepository;
import com.jf.PetApp.infrastructure.repository.mentor.SpringMentorConversationJpaRepository;

/**
 * Usernames listed here are wiped back to a brand-new-signup state on every
 * login (see UserJpaEntity#resetToFreshSignupState) — currently just the
 * admin2 demo account (V5__seed_admin2_user.sql), used to exercise the
 * onboarding/empty-portfolio flows and expected to never accumulate
 * progress across sessions. admin3 (V17__seed_admin3_user.sql) is
 * deliberately NOT in this set — it's a plain USER-role account meant to
 * behave like a real user and keep whatever state a test session leaves it
 * in across logins.
 */
@Repository
public class DemoAccountResetAdapter implements DemoAccountResetPort {

    private static final Set<String> DEMO_USERNAMES = Set.of("admin2");

    private final SpringUserJpaRepository userJpaRepository;
    private final InvestmentRepository investmentRepository;
    private final LessonProgressJpaRepository lessonProgressRepository;
    private final XpEventJpaRepository xpEventRepository;
    private final AchievementUnlockJpaRepository achievementUnlockRepository;
    private final ActivityLogJpaRepository activityLogRepository;
    private final MissionCompletionJpaRepository missionCompletionRepository;
    private final SpringMentorConversationJpaRepository mentorConversationRepository;

    public DemoAccountResetAdapter(
            SpringUserJpaRepository userJpaRepository,
            InvestmentRepository investmentRepository,
            LessonProgressJpaRepository lessonProgressRepository,
            XpEventJpaRepository xpEventRepository,
            AchievementUnlockJpaRepository achievementUnlockRepository,
            ActivityLogJpaRepository activityLogRepository,
            MissionCompletionJpaRepository missionCompletionRepository,
            SpringMentorConversationJpaRepository mentorConversationRepository) {
        this.userJpaRepository = userJpaRepository;
        this.investmentRepository = investmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.xpEventRepository = xpEventRepository;
        this.achievementUnlockRepository = achievementUnlockRepository;
        this.activityLogRepository = activityLogRepository;
        this.missionCompletionRepository = missionCompletionRepository;
        this.mentorConversationRepository = mentorConversationRepository;
    }

    @Override
    @Transactional
    public void resetIfDemoAccount(String username) {
        if (!DEMO_USERNAMES.contains(username)) {
            return;
        }

        Optional<UserJpaEntity> userOpt = userJpaRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }

        UserJpaEntity user = userOpt.get();
        User domainUser = user.toDomain();
        Long userId = domainUser.getId();

        investmentRepository.deleteByUserEmail(domainUser.getEmail());
        lessonProgressRepository.deleteByUserId(userId);
        xpEventRepository.deleteByUserId(userId);
        achievementUnlockRepository.deleteByUserId(userId);
        activityLogRepository.deleteByUserId(userId);
        missionCompletionRepository.deleteByUserId(userId);
        mentorConversationRepository.deleteByUserId(userId);

        user.resetToFreshSignupState();
        userJpaRepository.save(user);
    }
}
