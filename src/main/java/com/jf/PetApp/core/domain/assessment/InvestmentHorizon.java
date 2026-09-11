package com.jf.PetApp.core.domain.assessment;

/**
 * How long the user expects to keep investing before needing the money back,
 * chosen during Academy's onboarding ("Para quando é esse objetivo?" step) —
 * mirrors the Flutter client's {@code InvestmentHorizonEnum} one-to-one,
 * serialized as this enum's {@code name()}.
 */
public enum InvestmentHorizon {
    UP_TO_ONE_YEAR,
    ONE_TO_FIVE_YEARS,
    MORE_THAN_FIVE_YEARS,
    NOT_SURE_YET
}
