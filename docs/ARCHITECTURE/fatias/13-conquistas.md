# Fatia 13 — Conquistas

> Verificado em 2026-09-03 lendo o código. Toda linha aqui é rastreável a um arquivo.

Esta é a fatia mais completa e mais invisível do produto. Existe um catálogo de
10 conquistas, uma tabela no Postgres, uma avaliação server-side rodando a cada
abertura do painel, um overlay de celebração animado com partículas, um widget
de card e três suítes de teste.

**Nenhuma tela do Wallet mostra uma conquista.**

---

## 1. O que o usuário vê

No **Wallet**: nada. Uma frase do pet, uma única vez, no momento em que a
conquista é desbloqueada — e só se ele estiver na tela naquele instante.

No **Academy**: a aba Perfil tem uma seção "Conquistas" com um selo
`UnavailableBadge()` e o texto *"Suas conquistas e marcos vão aparecer aqui
conforme você progride."* — honesto, e correto, porque no Academy elas de fato
nunca desbloqueiam (§5).

Na **configuração inicial de carteira** do Wallet, o painel "Adicione seu
primeiro ativo para desbloquear:" lista, como item da checklist:

```
✓ Emblema de Primeiro Investidor
✓ +0 XP
✓ Missões Diárias
```

Sim, literalmente `+0 XP`. Isso está em produção. A §4.2 explica por quê.

---

## 2. Caminho do dado

```
DashboardScreen (loadAll)
  └─ PortfolioController._evaluateGamification()
       └─ AchievementsRepository.evaluate()
            └─ AchievementsRemoteDataSource.evaluate()
                 └─ GET /api/v1/achievements
                      [SecurityConfig: hasAuthority("APP_WALLET")]
                      └─ AchievementController.getAchievements()
                           └─ EvaluateAchievementsUseCaseImpl.execute(email)
                                ├─ buildContext(email)
                                │    ├─ GetPortfolioHoldingsUseCase   (lotes)
                                │    ├─ GetPortfolioSummaryUseCase    (valor, ganho)
                                │    └─ GetPortfolioAllocationUseCase (por categoria)
                                ├─ para cada AchievementDefinition:
                                │    isUnlocked? → qualifies.test(context)? → unlock()
                                └─ AchievementRepositoryAdapter
                                     └─ gamification.achievement_unlocks
  └─ (volta) newlyUnlocked = ...  ← ninguém lê no Wallet
       └─ EventBus.emit(AchievementUnlockedEvent)
            └─ PetCompanionController → uma fala do pet
```

O último trecho é o ponto: o dado percorre cliente → rede → filtro → controller
→ caso de uso → domínio → JPA → tabela → e volta, e o **único consumidor do
resultado no Wallet é o pet**.

---

## 3. Arquivos que importam

| Arquivo | Papel |
|---|---|
| `application/gamification/achievement/AchievementCatalog.java` | As 10 conquistas. **Autoridade** — é o que persiste. |
| `application/gamification/achievement/AchievementDefinition.java` | `record(code, xpReward, Predicate<AchievementContext>)`. |
| `application/gamification/achievement/AchievementContext.java` | Os fatos do portfólio que uma condição pode consultar. |
| `application/gamification/usecase/EvaluateAchievementsUseCaseImpl.java` | Monta o contexto, avalia, persiste. Contém a **2ª cópia** da tabela de rendimentos (§4.4). |
| `infrastructure/repository/gamification/AchievementRepositoryAdapter.java` | Único lugar que sabe que unlock é JPA. Trata corrida por constraint. |
| `infrastructure/config/SecurityConfig.java:120` | `/api/v1/achievements/**` → **WALLET apenas**. |
| `db/migration/V6__achievement_streak_schema.sql` | `achievement_unlocks` + `uq(user_id, achievement_code)`. |
| `db/migration-postgres/V20__schema_separation.sql:53` | Move a tabela para o schema `gamification`. |
| `lib/features/portfolio/domain/services/achievement_catalog.dart` | Cópia Dart: títulos, ícones e **preview** pré-save. Diverge (§4.3). |
| `lib/features/portfolio/presentation/widgets/achievement_celebration_overlay.dart` | Overlay animado. **Nunca montado** em nenhum dos dois apps. |
| `lib/features/portfolio/presentation/widgets/achievement_card_widget.dart` | Card de conquista. **Nunca usado** (só existe no Wallet, e tem teste). |
| `lib/features/investment/presentation/widgets/unlockable_rewards_card.dart` | A checklist do "+0 XP". Montado em `investment_configuration_screen.dart:650`. |

---

## 4. Regras de negócio — e o porquê

### 4.1 Toda conquista vale 0 XP, e isso é deliberado

O catálogo Java declara a razão em duas decisões nomeadas:

- **DECISION-014** — XP nunca pode recompensar lucro, patrimônio ou renda passiva.
- **DECISION-027** — XP nunca pode recompensar nem *atividade* de investimento;
  só comportamento de aprendizado/prática.

Como o pet e o nível são **compartilhados** entre os dois apps (fatia 04), XP
vindo de riqueza no Wallet apareceria como progresso no Academy. As conquistas
foram mantidas como marcos permanentes e tiveram o XP zerado.

<!-- A intenção é clara e está bem documentada. O problema é que ela está
     expressa como *valor literal* (0 em dez linhas), não como estrutura:
     nada no tipo, no construtor ou em teste impede que alguém escreva
     new AchievementDefinition("portfolio_50k", 200, ...). Já catalogado
     como P1 nas Demandas. -->

### 4.2 O `+0 XP` visível na tela

`UnlockableRewardsCard` monta a checklist assim:

```dart
final xp = AchievementCatalog.totalXpFor({'first_investment'});
final items = [
  'Emblema de Primeiro Investidor',
  '+$xp XP',
  ...
];
```

E o comentário acima explica a escolha:

> *"o +XP é puxado do `AchievementCatalog` real em vez de um número hardcoded,
> então nunca pode sair de sincronia com o que é de fato concedido"*

O mecanismo está **certo**. Ele não pode dessincronizar. E o número que ele
sincroniza é zero. A mesma coisa acontece no `AchievementCelebrationOverlay`,
que renderiza uma pílula dourada com `'+$totalXp XP'` — invisível hoje só
porque o overlay nunca é montado.

É o caso mais limpo do projeto de uma decisão correta (§4.1) tomada num lugar
sem propagar para o lugar que a exibe.

### 4.3 O catálogo Dart e o Java divergem — em `etf_collector`

O `achievement_catalog.dart` avisa, no próprio doc, que a sincronia é manual:

> *"Ids, XP rewards e condições aqui devem ser mantidos em sincronia com aquele
> catálogo Java à mão (mesmo risco de drift aceito do `LevelCalculator`)."*

A sincronia já quebrou:

| | Dart (`achievement_catalog.dart:115`) | Java (`EvaluateAchievementsUseCaseImpl:100`) |
|---|---|---|
| `etf_collector` | `holdings.where(type == FUNDS).length >= 3` | `distinctFundsTickerCount >= 3` |
| Conta | **lotes** | **nomes distintos** |

Três compras do mesmo fundo: o preview do onboarding acende o "Colecionador de
ETFs"; o backend, ao salvar, não desbloqueia. O usuário vê a conquista prometida
e depois ela não existe.

<!-- O Java é a definição correta ("colecionador" significa fundos distintos).
     O bug está no Dart, e é exatamente o drift que o doc do arquivo previu. -->

### 4.4 "Primeiro Dividendo" desbloqueia sem nenhum dividendo

```java
new AchievementDefinition("first_dividend", 0,
    c -> c.monthlyPassiveIncomeEstimate().compareTo(BigDecimal.ZERO) > 0),
```

`monthlyPassiveIncomeEstimate` vem da tabela de rendimentos **presumidos** — a
mesma da fatia 12 (ações 5%, renda fixa 11%, FIIs 8%, fundos 4%). Para qualquer
ativo que não seja cripto ou "outros", o valor é maior que zero **no instante da
primeira compra**.

Ou seja: `first_investment` e `first_dividend` desbloqueiam **juntos**, na mesma
avaliação, e o segundo se chama "Primeiro Dividendo" sem que nenhum dividendo
tenha sido pago.

A parte fina é onde a regra está escrita. O javadoc da tabela **em Dart** diz:

> *"Sempre exposto ao usuário como 'estimado', nunca apresentado como um
> pagamento confirmado."*

A cópia Java dessa tabela vive dentro de `EvaluateAchievementsUseCaseImpl` — é a
**segunda cópia**, sem esse javadoc, e é ela que decide o desbloqueio. A regra
foi documentada no arquivo que não a viola e omitida no que viola.

O mesmo vale para `dividend_hunter` ("Caçador de Dividendos", ≥ R$1.000/ano
estimados): com 5% presumidos, é um crachá de **R$20 mil em ações**, com nome de
dividendo.

### 4.5 Desbloqueio é permanente e idempotente

Duas garantias, em camadas diferentes:

- `uq_achievement_unlocks (user_id, achievement_code)` no banco.
- O adapter faz `exists` antes de inserir **e** captura
  `DataIntegrityViolationException`, tratando corrida como no-op:

```java
} catch (DataIntegrityViolationException e) {
    // A concurrent duplicate request already unlocked this achievement between our
    // exists-check and this insert — the unique constraint caught it. Treat it as
    // the idempotent no-op it actually is, not a 500.
}
```

E o cliente monta a lista sempre a partir do estado persistido, nunca
re-derivando ao vivo — comentado no Dart como *"para que uma queda posterior do
patrimônio não possa esconder uma conquista já conquistada"*. Correto: quem
atingiu R$50 mil e depois caiu para R$40 mil mantém o marco.

---

## 5. O Academy chama um endpoint que não pode chamar

`PortfolioController._evaluateGamification()` do **Academy** roda a cada
`loadAll()`, deliberadamente fora do try/catch do portfólio, com este comentário:

```dart
// Deliberately outside the try/catch above and always run: XP/
// achievements/missions are gamification.* endpoints, unrestricted by
// app_context (see backend SecurityConfig), so they must keep working
// even when the real-portfolio fetch above fails ...
```

O comentário está **errado**. `SecurityConfig.java:120`:

```java
.requestMatchers("/api/v1/achievements/**").hasAuthority(AppContextEnum.WALLET.authority())
```

Consequência, a cada abertura do painel do Academy:

1. `GET /api/v1/achievements` → **403**.
2. O `catch (_)` engole e cai no cache local de desbloqueios.
3. O cache local de um usuário Academy está **sempre vazio** — ele nunca teve
   uma resposta bem-sucedida para cachear.

O Academy carrega o catálogo Dart inteiro, o overlay, o repositório, o
datasource e o caminho de celebração por snack — tudo alcançável, tudo morto. A
tela de Perfil, que mostra "Indisponível", é a única parte do Academy que está
contando a verdade.

<!-- A separação em si está certa: conquistas são baseadas em patrimônio real e
     não pertencem ao Academy. O que está errado é o cliente chamar assim mesmo
     e o comentário afirmar o contrário do que o servidor faz. -->

Detalhe que vale registrar: no Academy o caminho de celebração é
propositalmente **um snack simples**, nunca o overlay:

> *"marcos de resultado financeiro são reconhecidos com um snack simples e
> dispensável, nunca o tratamento de momento-de-recompensa do
> `AchievementCelebrationOverlay`: o guardrail do design system é explícito que
> valorização, dividendos, aportes ou trades nunca podem disparar uma
> celebração — só progresso educacional pode."*

Guardrail bem escrito, aplicado no app onde o código nunca executa, e ausente do
app onde executa.

---

## 6. Dados persistidos

```
gamification.achievement_unlocks
  id             bigint identity
  user_id        bigint  → jf_users(user_id)
  achievement_code varchar(64)
  xp_awarded     integer
  unlocked_at    timestamp
  unique (user_id, achievement_code)
```

Definições (id, XP, condição) **não** são persistidas — vivem em código, pela
mesma razão que o conteúdo das aulas vive no `AcademyCatalog` do Flutter: são
lógica executável, não dado.

`xp_awarded` é gravado **por linha**, no momento do desbloqueio. Isso é a coisa
mais importante desta seção: o valor é congelado no passado, não lido do
catálogo atual. Ver drill 4.

---

## 7. Modos de falha

| Falha | O que acontece | Visível? |
|---|---|---|
| Sessão Academy chama o endpoint | 403, engolido, cache vazio | **Não** |
| Portfólio real falha ao carregar | Avaliação roda mesmo assim (por design) | Não |
| Duas requisições simultâneas desbloqueiam | Constraint pega, tratada como no-op | Não |
| Conquista desbloqueia no Wallet | Uma fala do pet, e nada mais | **Quase não** |
| Preview do onboarding diverge do backend | Conquista prometida não aparece | Sim, sem explicação |
| Cotação indisponível (fatia 10) | `currentValue` cai → `portfolio_10k` não desbloqueia hoje, desbloqueia amanhã | Não |

O último merece atenção: as condições de patrimônio dependem de
`GetPortfolioSummaryUseCase`, que depende da brapi. Uma falha de cotação não
apenas mostra ganho zero (fatia 10) — ela **adia silenciosamente** um
desbloqueio de marco.

---

## 8. Drills

<details>
<summary><b>Drill 1 —</b> Um usuário do Wallet atinge R$50 mil. O que ele vê?</summary>

Uma fala do pet, se estiver com o pet na tela naquele momento.

`AchievementCelebrationOverlay` existe, está testado, e não é montado em
nenhuma tela. `AchievementCardWidget` existe, está testado, e não é importado
por ninguém. O getter `PortfolioController.achievements` (que resolve o catálogo
para exibição) não é lido por nenhuma tela. `newlyUnlocked` é preenchido e nunca
consumido no Wallet.

O único consumidor é `PetCompanionController._onAppEvent`, via
`AchievementUnlockedEvent` → `PetMessageCatalog.achievementUnlocked(title)`.

Fechou o app antes da fala? O marco fica só no banco.
</details>

<details>
<summary><b>Drill 2 —</b> Por que o Academy mostra "Indisponível" e o Wallet não mostra nada?</summary>

Porque o Academy foi explicitamente ajustado na separação (`profile_screen.dart`
com `UnavailableBadge()`) e o Wallet não recebeu a tela correspondente.

A string existe nos dois apps (`profileAchievementsLabel: "Conquistas"`,
`profileAchievementsComingSoonBody`), mas só o `profile_screen.dart` do Academy
a renderiza.

A ironia é exata: o app que **não pode** desbloquear conquistas explica isso ao
usuário; o app que desbloqueia de verdade não menciona que elas existem.
</details>

<details>
<summary><b>Drill 3 —</b> O toggle "Alertas de conquistas" em Configurações controla o quê?</summary>

Nada.

`settings_screen.dart` grava `settings_achievement_alerts` no SharedPreferences
e lê de volta para desenhar o switch. Nenhum outro arquivo, em nenhum dos dois
apps, lê essa chave. Não há caminho de notificação de conquista para ela ligar
ou desligar.

Está nos dois apps, idêntico.
</details>

<details>
<summary><b>Drill 4 —</b> Todas as conquistas valem 0 XP hoje. O XP de conquistas de um usuário antigo é necessariamente 0?</summary>

**Não sabemos pelo código — e essa é a resposta.**

`xp_awarded` é gravado por linha no instante do desbloqueio
(`entity.setXpAwarded(definition.xpReward())`), e `totalXpFor` soma o que está
gravado:

```java
@Query("select coalesce(sum(a.xpAwarded), 0) from AchievementUnlockJpaEntity a where a.userId = :userId")
```

Os comentários DECISION-014/027 dizem *"kept as ... zero-XP"*, o que implica que
antes não era zero. Zerar o catálogo **não reescreve linhas já gravadas**, e
`V6` não tem backfill — nenhuma migração faz `update achievement_unlocks set
xp_awarded = 0`.

Se algum usuário desbloqueou conquistas antes daquela mudança, o XP dessas
linhas ainda soma no `TotalXpCalculator` (fatia 04) e ainda alimenta o nível
**compartilhado** com o Academy — exatamente o que a DECISION-014 proíbe.

Isso é verificável com uma consulta em produção:

```sql
select count(*), sum(xp_awarded)
from gamification.achievement_unlocks
where xp_awarded <> 0;
```

Se o resultado não for `0`, a decisão está violada em dados vivos e precisa de
uma migração corretiva, não de uma mudança de código.
</details>

<details>
<summary><b>Drill 5 —</b> A brapi está fora do ar. Um usuário com R$60 mil abre o app pela primeira vez em meses. Ele desbloqueia `portfolio_50k`?</summary>

Não hoje.

`AchievementContext.currentValue` vem de `GetPortfolioSummaryUseCase`, que usa
as cotações da brapi. Sem cotação, o valor atual cai (fatia 10) e a condição
`currentValue >= 50_000` não passa.

O desbloqueio não se perde — a avaliação roda de novo a cada abertura, e ele
desbloqueia quando as cotações voltarem. Mas o `unlocked_at` gravado será a data
da recuperação, não a data em que ele de fato atingiu o patamar.

O mesmo vale para `positive_return` e `first_dividend`.
</details>

<details>
<summary><b>Drill 6 —</b> Quantas cópias da tabela de rendimentos presumidos existem?</summary>

Duas, em linguagens diferentes:

- `investment_type_display.dart` → `assumedAnnualYield` (com o javadoc da regra).
- `EvaluateAchievementsUseCaseImpl.java:58` → `ASSUMED_ANNUAL_YIELD` (sem).

Os valores estão iguais hoje (5% / 11% / 8% / 4% / 0 / 0). Nenhum teste compara
as duas, e a cópia Java está enterrada dentro de um caso de uso de gamificação —
um lugar onde ninguém procuraria por regra de renda passiva.

É a mesma família de duplicação Dart/Java do `LevelCalculator` (fatia 04) e do
catálogo de conquistas (§4.3), que já quebrou.
</details>

---

## 9. Se você fosse mudar algo aqui

- **Dar uma tela às conquistas no Wallet** → o overlay, o card e o getter já
  existem e estão testados. É montar, não construir. Ver drill 1.
- **Parar de chamar o endpoint no Academy** → e corrigir o comentário que afirma
  o contrário do `SecurityConfig`. Ver §5.
- **Tirar o `+0 XP` da checklist do onboarding** → ou remover o item, ou trocar
  por algo que o usuário de fato ganha. Ver §4.2.
- **Alinhar `etf_collector`** → o Dart deve contar nomes distintos, como o Java.
  Ver §4.3.
- **Renomear `first_dividend` / `dividend_hunter`** → ou condicioná-los ao Radar
  de Proventos (dados confirmados) em vez da estimativa. Ver §4.4.
- **Rodar a consulta do drill 4 em produção** → antes de qualquer outra coisa
  desta lista.
- **Transformar o 0 XP em estrutura** → já registrado como P1 nas Demandas.
