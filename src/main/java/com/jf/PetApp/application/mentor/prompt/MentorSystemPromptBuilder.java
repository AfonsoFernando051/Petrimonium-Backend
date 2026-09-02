package com.jf.PetApp.application.mentor.prompt;

import com.jf.PetApp.application.investment.dto.AllocationSliceDTO;
import com.jf.PetApp.application.investment.dto.PortfolioSummaryDTO;
import com.jf.PetApp.application.learning.dto.LearningProgressResult;
import com.jf.PetApp.application.mentor.dto.MentorClientContextDTO;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPortfolioSummaryDTO;
import com.jf.PetApp.application.simulatedportfolio.dto.SimulatedPositionDTO;
import com.jf.PetApp.core.domain.Pet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the mentor's system prompt from server-known context (portfolio, pet) plus
 * client-supplied signals that only exist on-device (pet goal, horizon, screen, language).
 * Pure functions, no framework dependency, so they can be unit tested directly.
 *
 * <p>Split into {@link #buildForWallet} and {@link #buildForAcademy} (Stage 6, Pet/XP/Mentor
 * context separation) rather than one method taking both real- and simulated-portfolio
 * parameters: before the split, a single {@code build(...)} always assembled both blocks
 * regardless of which app the session belonged to, so a Wallet session's system prompt could
 * include Academy lesson/level content and an Academy session's could include real portfolio
 * numbers. Each entry point below can only ever see the DTOs relevant to its own context — there
 * is no parameter through which the other context's data could reach it.
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
            {response_format_instruction}
            Respond naturally and conversationally in {language}. Keep replies focused — do not mechanically march through a template, but where it fits naturally: answer the question, relate it to the user's own portfolio/goals, and end on an encouraging note.
            """;

    // Academy only: asks the model to separate the objective explanation from its own
    // personalized read, using fixed English markers regardless of {language} so the client can
    // parse them without a per-language marker table. Markers are optional by design — a short
    // conversational reply (greeting, acknowledgment) has no real "interpretation" layer to
    // separate out, and forcing one would mean fabricating a distinction that isn't there; the
    // client falls back to rendering the whole reply as plain text when neither marker appears.
    private static final String STRUCTURED_RESPONSE_INSTRUCTION = """

            RESPONSE FORMAT
            When explaining a concept from the user's Academy curriculum, structure your answer in two parts using these exact markers verbatim (in English, regardless of what language you reply in), each alone on its own line:
            [[CONTENT]]
            The objective, factual explanation — true regardless of who's asking, no personalization.
            [[INTERPRETATION]]
            Your own personalized read: why this matters for this user right now, tied to their real lesson/progress/goal — clearly framed as your interpretation, not a fact.
            For short replies that aren't explaining a concept (greetings, acknowledgments, clarifying questions), skip the markers entirely and answer normally in one block.
            """;

    // Wallet only: the design system's "camadas dado/cálculo/interpretação" guardrail, applied
    // to every reply that touches the user's real portfolio — not just a citation footer, but the
    // reply itself broken into up to three labeled parts. Same fixed-English-marker convention as
    // Academy's own instruction above, and just as optional: a plain conversational reply skips
    // all three markers rather than fabricating a data/calculation split that isn't there. Markers
    // may appear in any subset (a reply can be data-only, data+calculation, or add the Mentor's
    // own read on top) — the client parses whichever are present.
    private static final String WALLET_STRUCTURED_RESPONSE_INSTRUCTION = """

            RESPONSE FORMAT
            When your answer touches the user's real portfolio data or a computation derived from it, structure your answer using these exact markers verbatim (in English, regardless of what language you reply in), each alone on its own line, in this order — include only the ones that actually apply:
            [[DATA]]
            The raw fact from the user's real portfolio context above — true regardless of interpretation, no computation applied.
            [[CALCULATION]]
            A deterministic computation derived from that data (a change over a period, a percentage, a total) — never a prediction or a made-up figure.
            [[INTERPRETATION]]
            Your own personalized read: what this means for the user right now, clearly framed as your interpretation, never presented as fact.
            For replies that don't touch real portfolio data (greetings, acknowledgments, general questions), skip the markers entirely and answer normally in one block.
            """;

    private MentorSystemPromptBuilder() {
    }

    /** Wallet (real money): real portfolio + pet only. Never sees Academy/learning data. */
    public static String buildForWallet(
            Pet pet,
            PortfolioSummaryDTO portfolioSummary,
            List<AllocationSliceDTO> allocation,
            MentorClientContextDTO clientContext,
            String fallbackLanguage
    ) {
        String petName = resolvePetName(pet);
        String language = resolveLanguage(clientContext, fallbackLanguage);

        StringBuilder context = new StringBuilder();
        appendRealPortfolioBlock(context, portfolioSummary, allocation);
        appendPetBlock(context, pet, petName);
        appendClientContextBlock(context, clientContext);

        return render(petName, language, context.toString(), WALLET_STRUCTURED_RESPONSE_INSTRUCTION);
    }

    /**
     * Wallet only — which real data sources actually fed {@link #buildForWallet}'s system prompt
     * for this call, using the exact same conditionals as the block-appending methods above. Powers
     * the client's per-reply "Why am I seeing this?" citation list (Wallet design system guardrail:
     * every Mentor interpretation must cite its real sources). Stable English keys — the client maps
     * each to a translated label, the same convention as this class's own fixed-English markers.
     */
    public static List<String> walletSourcesFor(
            Pet pet, PortfolioSummaryDTO portfolioSummary, List<AllocationSliceDTO> allocation,
            MentorClientContextDTO clientContext) {
        List<String> sources = new ArrayList<>();
        if (portfolioSummary != null && portfolioSummary.totalAssets() != null && portfolioSummary.totalAssets() > 0) {
            sources.add("portfolio_summary");
        }
        if (allocation != null && !allocation.isEmpty()) {
            sources.add("portfolio_allocation");
        }
        if (pet != null) {
            sources.add("pet");
        }
        if (clientContext != null) {
            if (isPresent(clientContext.petGoal())) {
                sources.add("client_goal");
            }
            if (isPresent(clientContext.investmentHorizon())) {
                sources.add("client_horizon");
            }
            if (isPresent(clientContext.currentScreen())) {
                sources.add("client_screen");
            }
        }
        return sources;
    }

    /**
     * Academy (simulated): simulated portfolio + pet + learning progress only. Never sees real
     * portfolio data. The simulated portfolio is always framed as practice/virtual money so the
     * mentor's own language reinforces the app's on-screen disclaimer rather than contradicting it.
     */
    public static String buildForAcademy(
            Pet pet,
            SimulatedPortfolioSummaryDTO simulatedPortfolio,
            MentorClientContextDTO clientContext,
            String fallbackLanguage,
            LearningProgressResult learningProgress,
            String nextLessonTitle,
            String nextModuleTitle
    ) {
        String petName = resolvePetName(pet);
        String language = resolveLanguage(clientContext, fallbackLanguage);

        StringBuilder context = new StringBuilder();
        appendSimulatedPortfolioBlock(context, simulatedPortfolio);
        appendPetBlock(context, pet, petName);
        appendLearningProgressBlock(context, learningProgress, nextLessonTitle, nextModuleTitle);
        appendClientContextBlock(context, clientContext);

        return render(petName, language, context.toString(), STRUCTURED_RESPONSE_INSTRUCTION);
    }

    private static String render(String petName, String language, String contextBlock, String responseFormatInstruction) {
        return SYSTEM_PROMPT_TEMPLATE
                .replace("{petName}", petName)
                .replace("{language}", language)
                .replace("{context_block}", contextBlock)
                .replace("{response_format_instruction}", responseFormatInstruction);
    }

    private static String resolvePetName(Pet pet) {
        return (pet != null && pet.getName() != null && !pet.getName().isBlank())
                ? pet.getName()
                : "your pet";
    }

    private static void appendRealPortfolioBlock(
            StringBuilder context, PortfolioSummaryDTO portfolioSummary, List<AllocationSliceDTO> allocation) {
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
    }

    private static void appendSimulatedPortfolioBlock(StringBuilder context, SimulatedPortfolioSummaryDTO simulatedPortfolio) {
        if (simulatedPortfolio == null || simulatedPortfolio.positions() == null || simulatedPortfolio.positions().isEmpty()) {
            context.append(String.format(Locale.US,
                    "- Simulated practice portfolio (virtual money, NOT real — always make this clear): no positions yet, virtual balance %.2f %s.%n",
                    simulatedPortfolio == null ? 0.0 : simulatedPortfolio.virtualBalance().doubleValue(),
                    simulatedPortfolio == null ? "" : simulatedPortfolio.currency()));
            return;
        }

        context.append(String.format(Locale.US,
                "- Simulated practice portfolio (virtual money, NOT real — always make this clear): %d position(s), virtual balance %.2f %s.%n",
                simulatedPortfolio.positions().size(), simulatedPortfolio.virtualBalance().doubleValue(), simulatedPortfolio.currency()));
        context.append("- Simulated holdings by ticker:\n");
        for (SimulatedPositionDTO position : simulatedPortfolio.positions()) {
            context.append(String.format(Locale.US, "  - %s: %.6f shares @ avg %.2f (%.1f%% of the simulated portfolio)%n",
                    position.ticker(), position.quantity().doubleValue(), position.averagePrice().doubleValue(),
                    position.allocationPercent().doubleValue()));
        }
    }

    private static void appendPetBlock(StringBuilder context, Pet pet, String petName) {
        if (pet != null) {
            context.append(String.format("- Pet: %s (%s).%n", petName, pet.getSpecie()));
        }
    }

    private static void appendLearningProgressBlock(
            StringBuilder context, LearningProgressResult learningProgress, String nextLessonTitle, String nextModuleTitle) {
        if (learningProgress == null) {
            return;
        }
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

    private static void appendClientContextBlock(StringBuilder context, MentorClientContextDTO clientContext) {
        if (clientContext == null) {
            return;
        }
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
