package com.jf.PetApp.infrastructure.repository.user;

import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.assessment.InvestorProfile;
import com.jf.PetApp.core.domain.enums.InvestmentType;
import com.jf.PetApp.core.domain.gamification.XpEventType;
import com.jf.PetApp.infrastructure.entity.AchievementUnlockJpaEntity;
import com.jf.PetApp.infrastructure.entity.ActivityLogJpaEntity;
import com.jf.PetApp.infrastructure.entity.InvestmentJpaEntity;
import com.jf.PetApp.infrastructure.entity.LessonProgressJpaEntity;
import com.jf.PetApp.infrastructure.entity.MentorConversationJpaEntity;
import com.jf.PetApp.infrastructure.entity.MissionCompletionJpaEntity;
import com.jf.PetApp.infrastructure.entity.UserJpaEntity;
import com.jf.PetApp.infrastructure.entity.XpEventJpaEntity;
import com.jf.PetApp.infrastructure.repository.InvestmentRepository;
import com.jf.PetApp.infrastructure.repository.gamification.AchievementUnlockJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.ActivityLogJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.MissionCompletionJpaRepository;
import com.jf.PetApp.infrastructure.repository.gamification.XpEventJpaRepository;
import com.jf.PetApp.infrastructure.repository.learning.LessonProgressJpaRepository;
import com.jf.PetApp.infrastructure.repository.mentor.SpringMentorConversationJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DemoAccountResetAdapterTest {

    @Autowired
    private SpringUserJpaRepository userJpaRepository;
    @Autowired
    private InvestmentRepository investmentRepository;
    @Autowired
    private LessonProgressJpaRepository lessonProgressRepository;
    @Autowired
    private XpEventJpaRepository xpEventRepository;
    @Autowired
    private AchievementUnlockJpaRepository achievementUnlockRepository;
    @Autowired
    private ActivityLogJpaRepository activityLogRepository;
    @Autowired
    private MissionCompletionJpaRepository missionCompletionRepository;
    @Autowired
    private SpringMentorConversationJpaRepository mentorConversationRepository;

    private DemoAccountResetAdapter adapter;

    private Long admin2Id;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        adapter = new DemoAccountResetAdapter(
                userJpaRepository, investmentRepository, lessonProgressRepository,
                xpEventRepository, achievementUnlockRepository, activityLogRepository,
                missionCompletionRepository, mentorConversationRepository);

        admin2Id = createUser("admin2", "admin2@petinvest.local");
        otherUserId = createUser("investor", "investor@test.com");

        seedProgressFor(admin2Id, "admin2@petinvest.local");
        seedProgressFor(otherUserId, "investor@test.com");
    }

    private Long createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hash");
        user.setHasAnsweredOnboarding(true);
        user.setInvestorProfile(InvestorProfile.TACTICIAN);
        UserJpaEntity saved = userJpaRepository.save(UserJpaEntity.fromDomain(user));
        return saved.toDomain().getId();
    }

    private void seedProgressFor(Long userId, String email) {
        InvestmentJpaEntity investment = new InvestmentJpaEntity();
        investment.setUser(userJpaRepository.findByEmail(email).orElseThrow());
        investment.setName("PETR4");
        investment.setQuantity(10.0);
        investment.setPurchasePrice(30.0);
        investment.setPurchaseDate(LocalDate.now());
        investment.setType(InvestmentType.STOCKS);
        investmentRepository.save(investment);

        LessonProgressJpaEntity lesson = new LessonProgressJpaEntity();
        lesson.setUserId(userId);
        lesson.setLessonId("lesson-1");
        lesson.setCompletedAt(Instant.now());
        lessonProgressRepository.save(lesson);

        XpEventJpaEntity xpEvent = new XpEventJpaEntity();
        xpEvent.setUserId(userId);
        xpEvent.setEventType(XpEventType.LESSON_COMPLETED);
        xpEvent.setAmount(10);
        xpEvent.setSourceId("lesson-1");
        xpEvent.setCreatedAt(Instant.now());
        xpEventRepository.save(xpEvent);

        AchievementUnlockJpaEntity achievement = new AchievementUnlockJpaEntity();
        achievement.setUserId(userId);
        achievement.setAchievementCode("FIRST_LESSON");
        achievement.setXpAwarded(5);
        achievement.setUnlockedAt(Instant.now());
        achievementUnlockRepository.save(achievement);

        ActivityLogJpaEntity activity = new ActivityLogJpaEntity();
        activity.setUserId(userId);
        activity.setActivityDate(LocalDate.now());
        activityLogRepository.save(activity);

        MissionCompletionJpaEntity mission = new MissionCompletionJpaEntity();
        mission.setUserId(userId);
        mission.setMissionCode("DAILY_LOGIN");
        mission.setPeriodKey("2026-08-24");
        mission.setXpAwarded(5);
        mission.setCompletedAt(Instant.now());
        missionCompletionRepository.save(mission);

        MentorConversationJpaEntity conversation = new MentorConversationJpaEntity();
        conversation.setUser(userJpaRepository.findByEmail(email).orElseThrow());
        conversation.setTitle("Chat");
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        mentorConversationRepository.save(conversation);
    }

    @Test
    void resetIfDemoAccount_ForAdmin2_WipesProgressAndOnboardingState() {
        adapter.resetIfDemoAccount("admin2");

        assertThat(investmentRepository.findByUser_Email("admin2@petinvest.local")).isEmpty();
        assertThat(lessonProgressRepository.findByUserId(admin2Id)).isEmpty();
        assertThat(xpEventRepository.sumAmountByUserId(admin2Id)).isZero();
        assertThat(achievementUnlockRepository.findByUserId(admin2Id)).isEmpty();
        assertThat(activityLogRepository.findActivityDatesByUserIdOrderByActivityDateDesc(admin2Id)).isEmpty();
        assertThat(missionCompletionRepository.sumXpAwardedByUserId(admin2Id)).isZero();
        assertThat(mentorConversationRepository.findByUser_EmailOrderByUpdatedAtDesc("admin2@petinvest.local")).isEmpty();

        User reset = userJpaRepository.findByUsername("admin2").orElseThrow().toDomain();
        assertThat(reset.hasAnsweredOnboarding()).isFalse();
        assertThat(reset.getInvestorProfile()).isNull();
    }

    @Test
    void resetIfDemoAccount_ForAdmin2_DoesNotTouchOtherUsersProgress() {
        adapter.resetIfDemoAccount("admin2");

        assertThat(investmentRepository.findByUser_Email("investor@test.com")).isNotEmpty();
        assertThat(lessonProgressRepository.findByUserId(otherUserId)).isNotEmpty();
        assertThat(xpEventRepository.sumAmountByUserId(otherUserId)).isEqualTo(10);
        assertThat(achievementUnlockRepository.findByUserId(otherUserId)).isNotEmpty();
        assertThat(activityLogRepository.findActivityDatesByUserIdOrderByActivityDateDesc(otherUserId)).isNotEmpty();
        assertThat(missionCompletionRepository.sumXpAwardedByUserId(otherUserId)).isEqualTo(5);
        assertThat(mentorConversationRepository.findByUser_EmailOrderByUpdatedAtDesc("investor@test.com")).isNotEmpty();
    }

    @Test
    void resetIfDemoAccount_ForNonDemoUsername_DoesNothing() {
        adapter.resetIfDemoAccount("investor");

        assertThat(lessonProgressRepository.findByUserId(otherUserId)).isNotEmpty();
    }
}
