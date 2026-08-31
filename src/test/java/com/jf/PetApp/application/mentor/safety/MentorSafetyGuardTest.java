package com.jf.PetApp.application.mentor.safety;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentorSafetyGuardTest {

    @Test
    void violatesSafetyRules_WithNullOrBlankReply_ReturnsFalse() {
        assertFalse(MentorSafetyGuard.violatesSafetyRules(null));
        assertFalse(MentorSafetyGuard.violatesSafetyRules(""));
        assertFalse(MentorSafetyGuard.violatesSafetyRules("   "));
    }

    @Test
    void violatesSafetyRules_WithOrdinaryEducationalReply_ReturnsFalse() {
        assertFalse(MentorSafetyGuard.violatesSafetyRules(
                "Many long-term investors buy and hold ETFs for diversification. "
                        + "Dividends can boost total return over time, but there is no such thing as a sure thing in markets."));
    }

    @Test
    void violatesSafetyRules_WithGeneralDiscussionOfBuyingAndSelling_ReturnsFalse() {
        assertFalse(MentorSafetyGuard.violatesSafetyRules(
                "Deciding when to buy or sell depends on your own goals and time horizon, not on a single metric."));
    }

    @Test
    void violatesSafetyRules_WithDirectBuyDirectiveAndTicker_ReturnsTrue() {
        assertTrue(MentorSafetyGuard.violatesSafetyRules("You should buy PETR4 right now, it's a great pick."));
    }

    @Test
    void violatesSafetyRules_WithDirectSellDirectiveAndTicker_ReturnsTrue() {
        assertTrue(MentorSafetyGuard.violatesSafetyRules("I'd sell TSLA before the quarter ends."));
    }

    @Test
    void violatesSafetyRules_WithPortugueseDirectBuyDirective_ReturnsTrue() {
        assertTrue(MentorSafetyGuard.violatesSafetyRules("Compre ITUB4 hoje mesmo, é uma oportunidade única."));
    }

    @Test
    void violatesSafetyRules_WithGuaranteedReturnClaim_ReturnsTrue() {
        assertTrue(MentorSafetyGuard.violatesSafetyRules("This strategy has a guaranteed return of 20% per year."));
    }

    @Test
    void violatesSafetyRules_WithPortugueseGuaranteedReturnClaim_ReturnsTrue() {
        assertTrue(MentorSafetyGuard.violatesSafetyRules("Esse fundo tem retorno garantido de 15% ao ano."));
    }

    @Test
    void violatesSafetyRules_WithLicensedAdviserClaim_ReturnsTrue() {
        assertTrue(MentorSafetyGuard.violatesSafetyRules("As a licensed financial advisor, I recommend this fund for you."));
    }

    @Test
    void violatesSafetyRules_WithPortugueseLicensedAdviserClaim_ReturnsTrue() {
        assertTrue(MentorSafetyGuard.violatesSafetyRules("Sou um consultor financeiro licenciado e posso te orientar diretamente."));
    }

    @Test
    void safeRedirectReply_WithPortugueseLanguage_ReturnsPortugueseMessage() {
        String reply = MentorSafetyGuard.safeRedirectReply("pt");
        assertTrue(reply.contains("carteira"));
    }

    @Test
    void safeRedirectReply_WithEnglishOrOtherLanguage_ReturnsEnglishMessage() {
        assertTrue(MentorSafetyGuard.safeRedirectReply("en").contains("portfolio"));
        assertTrue(MentorSafetyGuard.safeRedirectReply(null).contains("portfolio"));
        assertTrue(MentorSafetyGuard.safeRedirectReply("fr").contains("portfolio"));
    }
}
