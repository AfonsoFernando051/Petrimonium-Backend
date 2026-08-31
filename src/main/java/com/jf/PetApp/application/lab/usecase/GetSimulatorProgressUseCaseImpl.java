package com.jf.PetApp.application.lab.usecase;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.gamification.port.XpEventRepositoryPort;
import com.jf.PetApp.application.gamification.service.LevelCalculator;
import com.jf.PetApp.application.gamification.service.TotalXpCalculator;
import com.jf.PetApp.application.lab.dto.SimulatorProgressResult;
import com.jf.PetApp.application.user.port.UserRepository;
import com.jf.PetApp.core.domain.User;
import com.jf.PetApp.core.domain.gamification.PlayerLevel;
import com.jf.PetApp.core.domain.gamification.XpEventType;

@Service
public class GetSimulatorProgressUseCaseImpl implements GetSimulatorProgressUseCase {

    private final UserRepository userRepository;
    private final XpEventRepositoryPort xpEventRepository;
    private final TotalXpCalculator totalXpCalculator;

    public GetSimulatorProgressUseCaseImpl(
            UserRepository userRepository,
            XpEventRepositoryPort xpEventRepository,
            TotalXpCalculator totalXpCalculator) {
        this.userRepository = userRepository;
        this.xpEventRepository = xpEventRepository;
        this.totalXpCalculator = totalXpCalculator;
    }

    @Override
    public SimulatorProgressResult execute(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Long userId = user.getId();

        Set<String> completedSimulatorIds =
                xpEventRepository.sourceIdsByUserIdAndEventType(userId, XpEventType.SIMULATOR_COMPLETED);

        int totalXp = totalXpCalculator.totalXpFor(userId);
        PlayerLevel level = LevelCalculator.fromXp(totalXp);

        return new SimulatorProgressResult(
                completedSimulatorIds, totalXp, level.level(), level.xpIntoLevel(), level.xpForNextLevel());
    }
}
