package com.jf.PetApp.application.mentor.prompt;

import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;
import com.jf.PetApp.application.learning.dto.LearningProgressResult;
import com.jf.PetApp.application.mentor.dto.MentorClientContextDTO;
import com.jf.PetApp.core.domain.Pet;

import java.util.List;
import java.util.Locale;

/**
 * Builds the mentor's system prompt from server-known context (portfolio, pet) plus
 * client-supplied signals that only exist on-device (pet goal, horizon, screen, language).
 * Pure function, no framework dependency, so it can be unit tested directly.
 */
public final class MentorSystemPromptBuilder {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are {petName}, the user's personal virtual investment mentor inside Invest Game — not a generic AI assistant.

            WHO YOU ARE
            - A patient, encouraging, emotionally supportive companion who grows alongside the user.
            - A long-term financial educator and psychological coach against emotional investing decisions.
            - Playful and optimistic, but always credible and professional about money.

            YOUR MISSION
            Your job is not to answer questions. Your job is to turn the user into a disciplined long-term investor over years, not days. Every reply should reinforce: consistency, patience, learning, diversification, long-term thinking, and risk awareness.

            INVESTMENT PHILOSOPHY (always teach from this lens)
            Long-term investing, buy & hold, value investing, dividend growth, passive income, compounding, diversification, ETF investing, risk management, asset allocation, continuous learning.
            Never encourage: day trading, speculation, market timing, FOMO, or "guaranteed" outcomes.

            ADAPTIVE TONE
            Infer the user's level from the context provided (portfolio complexity, how they phrase things) and adjust:
            - Beginner: simple language, few technical terms, concrete analogies (building a house, planting a tree, RPG leveling up).
            - Intermediate: metrics, portfolio-level discussion, risk concepts.
            - Advanced: valuation, governance, macro, tax efficiency, portfolio optimization.
            Never bury a beginner in jargon.

            PERSONALIZE, DON'T GENERALIZE
            When portfolio context is available, always connect theory to the user's actual numbers instead of speaking abstractly. Prefer "You currently have X%% in a sector — adding another sector could reduce concentration risk" over "Diversification is important."

            SAFETY RULES (never break these)
            - Never recommend buying or selling a specific security.
            - Never predict prices or promise returns.
            - Never present yourself as a licensed financial advisor.
            - If asked "what should I buy today," redirect to the user's own goals/allocation and teach the underlying methodology instead of answering directly.
            - Frame everything as education, not advice.

            CONTEXT YOU'VE BEEN GIVEN THIS TURN
            {context_block}

            Respond naturally and conversationally in {language}. Keep replies focused — do not mechanically march through a template, but where it fits naturally: answer the question, relate it to the user's own portfolio/goals, and end on an encouraging note.
            """;

    private MentorSystemPromptBuilder() {
    }

    public static String build(
            Pet pet,
            PortfolioSummaryDTO portfolioSummary,
            List<AllocationSliceDTO> allocation,
            MentorClientContextDTO clientContext,
            String fallbackLanguage,
            LearningProgressResult learningProgress,
            String nextLessonTitle,
            String nextModuleTitle
    ) {
        String petName = (pet != null && pet.getName() != null && !pet.getName().isBlank())
                ? pet.getName()
                : "your pet";

        String language = resolveLanguage(clientContext, fallbackLanguage);

        String contextBlock = buildContextBlock(pet, petName, portfolioSummary, allocation, clientContext,
                learningProgress, nextLessonTitle, nextModuleTitle);

        return SYSTEM_PROMPT_TEMPLATE
                .replace("{petName}", petName)
                .replace("{language}", language)
                .replace("{context_block}", contextBlock);
    }

    private static String buildContextBlock(
            Pet pet,
            String petName,
            PortfolioSummaryDTO portfolioSummary,
            List<AllocationSliceDTO> allocation,
            MentorClientContextDTO clientContext,
            LearningProgressResult learningProgress,
            String nextLessonTitle,
            String nextModuleTitle
    ) {
        StringBuilder context = new StringBuilder();

        if (portfolioSummary != null && portfolioSummary.totalAssets() != null && portfolioSummary.totalAssets() > 0) {
            context.append(String.format(Locale.US,
                    "- Portfolio: %d asset(s), invested capital %.2f, current value %.2f, total gain %.2f (%.2f%%).%n",
                    portfolioSummary.totalAssets(), portfolioSummary.investedCapital(), portfolioSummary.currentValue(),
                    portfolioSummary.totalGain(), portfolioSummary.totalGainPercent()));
        } else {
            context.append("- Portfolio: the user hasn't registered any investments yet.\n");
        }

        if (allocation != null && !allocation.isEmpty()) {
            context.append("- Allocation by asset type:\n");
            for (AllocationSliceDTO slice : allocation) {
                context.append(String.format(Locale.US, "  - %s: %.1f%% of the portfolio%n",
                        slice.type(), slice.portfolioPercent()));
            }
        }

        if (pet != null) {
            context.append(String.format("- Pet: %s (%s).%n", petName, pet.getSpecie()));
        }

        if (learningProgress != null) {
            context.append(String.format(Locale.US,
                    "- Academy progress: level %d (%d/%d XP into this level), %d lesson(s) completed, %d module(s) completed.%n",
                    learningProgress.level(), learningProgress.xpIntoLevel(), learningProgress.xpForNextLevel(),
                    learningProgress.completedLessonIds().size(), learningProgress.completedModuleIds().size()));
            if (isPresent(nextLessonTitle)) {
                context.append("- Next lesson to continue: \"").append(nextLessonTitle).append('"');
                if (isPresent(nextModuleTitle)) {
                    context.append(" (module: ").append(nextModuleTitle).append(')');
                }
                context.append('\n');
            } else {
                context.append("- The user has completed every Academy lesson currently available.\n");
            }
        }

        if (clientContext != null) {
            if (isPresent(clientContext.petGoal())) {
                context.append("- User's stated investment goal: ").append(clientContext.petGoal()).append('\n');
            }
            if (isPresent(clientContext.investmentHorizon())) {
                context.append("- User's stated investment horizon: ").append(clientContext.investmentHorizon()).append('\n');
            }
            if (isPresent(clientContext.currentScreen())) {
                context.append("- Currently viewing: ").append(clientContext.currentScreen()).append('\n');
            }
        }

        return context.toString();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Same resolution the system prompt itself uses (client-supplied language, falling back to
     * the user's stored preference, falling back to "pt") — extracted so callers that need to
     * address the user in their own language outside the prompt (e.g. {@code MentorSafetyGuard}'s
     * redirect message) don't duplicate the fallback chain.
     */
    public static String resolveLanguage(MentorClientContextDTO clientContext, String fallbackLanguage) {
        return (clientContext != null && clientContext.language() != null && !clientContext.language().isBlank())
                ? clientContext.language()
                : (fallbackLanguage == null || fallbackLanguage.isBlank() ? "pt" : fallbackLanguage);
    }
}
