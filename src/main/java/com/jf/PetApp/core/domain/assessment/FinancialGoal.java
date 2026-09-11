package com.jf.PetApp.core.domain.assessment;

/**
 * The user's main financial-life objective, chosen during Academy's onboarding
 * ("Qual é o seu objetivo agora?" step) — mirrors the Flutter client's
 * {@code PetGoalEnum} one-to-one, serialized as this enum's {@code name()}.
 */
public enum FinancialGoal {
    EMERGENCY_FUND,
    GET_OUT_OF_DEBT,
    BUY_IMPORTANT_THING,
    INVEST_WITH_CONFIDENCE,
    JUST_WANT_TO_LEARN
}
