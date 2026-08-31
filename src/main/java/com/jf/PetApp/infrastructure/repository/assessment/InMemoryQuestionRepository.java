package com.jf.PetApp.infrastructure.repository.assessment;

import com.jf.PetApp.core.domain.assessment.Option;
import com.jf.PetApp.core.domain.assessment.Question;
import com.jf.PetApp.core.port.QuestionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class InMemoryQuestionRepository implements QuestionRepository {
    
    // Canonical source language is pt-BR. Other languages are produced on demand
    // by TranslationCacheService (see application/translation) from this text.
    private static final List<Question> QUESTIONS = List.of(
        new Question("q1", "Qual é o seu principal objetivo ao investir?", List.of(
            new Option("q1_1", "Preservar meu dinheiro a todo custo", 0),
            new Option("q1_2", "Crescimento constante com risco moderado", 2),
            new Option("q1_3", "Maximizar retornos, eu aguento a volatilidade", 4)
        )),
        new Question("q2", "Como você reagiria se sua carteira caísse 20% em uma semana?", List.of(
            new Option("q2_1", "Venderia tudo imediatamente por medo", 0),
            new Option("q2_2", "Manteria a posição e esperaria a recuperação", 2),
            new Option("q2_3", "Compraria mais aproveitando o desconto", 4)
        )),
        new Question("q3", "Qual é o seu horizonte de tempo para investir?", List.of(
            new Option("q3_1", "Menos de 1 ano (preciso de liquidez em breve)", 0),
            new Option("q3_2", "De 1 a 5 anos", 2),
            new Option("q3_3", "Mais de 5 anos (pensando no longo prazo)", 4)
        )),
        new Question("q4", "Quando o mercado está volátil, qual ação mais combina com você?", List.of(
            new Option("q4_1", "Evitar riscos e focar na proteção do capital", 0),
            new Option("q4_2", "Rebalancear gradualmente mantendo um plano de longo prazo", 2),
            new Option("q4_3", "Aumentar a exposição quando os preços caem, aceitando oscilações", 4)
        )),
        new Question("q5", "Quão importante é para você a possibilidade de grandes perdas no curto prazo?", List.of(
            new Option("q5_1", "Muito importante - prefiro resultados menores e mais estáveis", 0),
            new Option("q5_2", "Moderadamente importante - tolero perdas em troca de mais retorno", 2),
            new Option("q5_3", "Pouco importante - priorizo o crescimento mesmo que doa", 4)
        ))
    );

    @Override
    public List<Question> findAll() {
        return QUESTIONS;
    }
}
