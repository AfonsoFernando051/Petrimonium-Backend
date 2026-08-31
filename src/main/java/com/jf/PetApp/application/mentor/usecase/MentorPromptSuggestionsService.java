package com.jf.PetApp.application.mentor.usecase;

import com.jf.PetApp.application.translation.service.TranslationCacheService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Server-owned, rotating conversation starters for a new Mentor chat. */
@Service
public class MentorPromptSuggestionsService {

    private static final List<String> PROMPTS = List.of(
            "O que são dividendos?", "Como funciona a renda fixa?", "O que é um ETF?",
            "Como começar a investir do zero?", "Qual a diferença entre ações e FIIs?",
            "O que significa diversificar uma carteira?", "Como montar uma reserva de emergência?",
            "Tesouro Direto é indicado para iniciantes?", "O que é inflação e como ela afeta meu dinheiro?",
            "Como funcionam os juros compostos?", "Qual a diferença entre CDB, LCI e LCA?",
            "O que é risco de mercado?", "Como definir meu objetivo financeiro?",
            "Por que preciso conhecer meu perfil de investidor?", "Como funciona a liquidez de um investimento?",
            "O que é rentabilidade real?", "Quando vale a pena rebalancear a carteira?",
            "O que são taxas de administração e performance?", "Como avaliar se uma empresa é saudável?",
            "O que é uma reserva para curto prazo?", "Como organizar meus primeiros aportes?",
            "Qual a diferença entre investir e poupar?", "Como evitar decisões por impulso ao investir?",
            "O que é exposição internacional?", "Como funcionam fundos de investimento?",
            "Qual missão devo completar agora?", "Como acompanhar a evolução da minha carteira?",
            "Quais cuidados tomar antes de comprar um ativo?", "O que significa volatilidade?",
            "Como posso aprender sobre investimentos no meu ritmo?");

    private final TranslationCacheService translationCacheService;

    public MentorPromptSuggestionsService(TranslationCacheService translationCacheService) {
        this.translationCacheService = translationCacheService;
    }

    public List<String> getRandomSuggestions(String language, int limit) {
        List<String> sample = new ArrayList<>(PROMPTS);
        Collections.shuffle(sample, ThreadLocalRandom.current());
        return sample.subList(0, Math.min(limit, sample.size())).stream()
                .map(prompt -> translationCacheService.translate(prompt, language))
                .toList();
    }
}
