# Fatia 14 — Sync B3 (adapter permanentemente desligado)

> Verificado em 2026-09-03 lendo o código. Toda linha aqui é rastreável a um arquivo.

Esta fatia é diferente das outras. Não documenta uma funcionalidade que o usuário
usa — documenta uma que **não existe de propósito**, e que foi construída como
costura arquitetural para o dia em que existir.

É também o melhor exemplo do projeto de "não implementado" feito direito. Vale
ler antes de escrever o próximo stub em qualquer lugar do sistema.

---

## 1. O que o usuário vê

Nada. Nenhuma tela dos dois apps chama este endpoint — zero referências a
`/api/investments/sync` em todo o Dart dos dois repositórios.

A carteira real continua sendo **inteiramente declarada pelo usuário**, via
`POST /api/investments/configure` (fatia 09). Não existe, e nunca existiu, uma
capacidade de "importar da corretora" para consolidar.

O onboarding do Wallet diz isso com todas as letras:

> *"A importação automática (B3, corretoras, CSV) ainda está sendo construída.
> Por enquanto, você pode adicionar seus ativos manualmente ou pular esta etapa."*

Honesto e correto.

### Mas o nome "B3" aparece em outro lugar, e ali não é honesto

Ler esta fatia levou a dois lugares onde a interface **credita a B3** por dados
que não vêm da B3:

```dart
// overview_screen.dart:159 e 184
final now = TimeOfDay.now();
final time = '${now.hour...}:${now.minute...}';
...
LayerChip(layer: DataLayer.data, label: 'DADO · B3, hoje $time'),
```

Acima do valor do patrimônio, no Home do Wallet, há um selo de proveniência que
diz **"DADO · B3, hoje 14:32"**. Dois problemas:

1. **A fonte não é a B3.** É a `brapi.dev`, uma API pública agregadora
   (`BrapiInvestmentApiClient`, fatia 10). Não existe nenhuma integração com a
   B3 neste projeto — é exatamente o que esta fatia inteira documenta.
2. **O horário é o relógio do aparelho no instante em que o widget desenha.**
   `TimeOfDay.now()` não tem relação alguma com quando a cotação foi buscada. Um
   scroll, uma troca de tema, um `setState` — o horário avança sem nenhuma
   chamada de rede.

E, cruzando com a fatia 10: se a cotação falhou, o valor exibido é o preço de
compra — e o selo continua dizendo "B3, hoje 14:33".

O mesmo selo existe no chat do Mentor (`chat_bubble.dart:180`), ali usando o
timestamp real da mensagem — melhor, mas ainda creditando a B3.

<!-- O componente é bem desenhado: LayerChip separa DADO / CÁLCULO /
     INTERPRETAÇÃO e nunca depende só de cor. A estrutura de proveniência está
     certa; o conteúdo que ela declara é que está errado. Catalogado. -->

---

## 2. Caminho do dado

```
POST /api/investments/sync
  [SecurityConfig: /api/investments/** → APP_CONTEXT_WALLET]
  └─ InvestmentController.syncRealPortfolio()      ← body opcional
       └─ SyncRealPortfolioUseCaseImpl.execute(email, ref, idempotencyKey)
            ├─ key = idempotencyKey ?: UUID.randomUUID()      ← §4.3
            ├─ findBy(email, provider, key) → já existe? devolve o mesmo resultado
            ├─ syncPort.isEnabled()?
            │    ├─ false → grava DISABLED  ← sempre, em todo ambiente hoje
            │    └─ true  → fetchPositions() → COMPLETED | FAILED
            └─ real_portfolio.real_portfolio_sync_log
```

A resposta é **sempre 200**. `DISABLED` não é erro — é o resultado normal e
esperado.

---

## 3. Arquivos que importam

| Arquivo | Papel |
|---|---|
| `application/investment/port/RealPortfolioSyncPort.java` | A porta. `isEnabled()` + `providerName()` + `fetchPositions()`. |
| `application/investment/dto/ExternalPositionDTO.java` | Formato interno provider-agnóstico: ticker, quantidade, preço médio, data. |
| `infrastructure/external/B3RealPortfolioSyncAdapter.java` | A única implementação — e permanentemente desligada. |
| `application/investment/usecase/SyncRealPortfolioUseCaseImpl.java` | Orquestra a tentativa e **sempre** audita o resultado. |
| `infrastructure/repository/investment/RealPortfolioSyncLogRepositoryAdapter.java` | Grava a linha de auditoria. |
| `db/migration/V25__real_portfolio_sync_log.sql` | A tabela. |
| `db/migration-postgres/V26__...sql` | Move para o schema `real_portfolio`. |
| `docs/BACKEND_MODULE_PLAN.md` §13 | A decisão registrada por escrito. |

---

## 4. Regras de negócio — e o porquê

### 4.1 O adapter exige duas condições, e nenhuma está satisfeita

```java
@Override
public boolean isEnabled() {
    return syncEnabled && token != null && !token.isBlank();
}
```

`app.b3-sync.enabled=true` **e** `api.b3.token` não vazio. Nenhum dos dois está
definido em ambiente algum.

E se alguém chamar mesmo assim:

```java
public List<ExternalPositionDTO> fetchPositions(String externalAccountReference) {
    if (!isEnabled()) {
        throw new IllegalStateException(
                "B3RealPortfolioSyncAdapter is disabled — no legitimate B3 integration is configured. ...");
    }
    throw new UnsupportedOperationException("B3 integration is not implemented");
}
```

**Duas exceções, nenhum retorno falso.** Compare com o que a fatia 10 encontrou
no `BrapiInvestmentApiClient`, que servia uma cotação fabricada de R$ 50,00
quando o token faltava. O mesmo problema, resolvido de dois jeitos opostos, no
mesmo projeto — e este é o jeito certo.

<!-- A diferença não é sorte: o javadoc da porta diz explicitamente
     "Implementations must throw rather than fabricate a result". A regra foi
     escrita na interface, não deixada ao critério de quem implementa. -->

### 4.2 A suíte de testes fixa a condição *falsa*, não a verdadeira

O `B3RealPortfolioSyncAdapterTest` testa `isEnabled()` em **toda combinação
parcial** de configuração: só a flag, só o token, nenhum dos dois. Todas
resultam em `false`.

É um teste que não protege uma funcionalidade — protege uma *ausência*. Um typo
futuro em `application.properties` não pode fazer o adapter reportar "ligado"
com nada real por trás.

### 4.3 A idempotência é opt-in, e o fallback escreve sempre

```java
String key = idempotencyKey != null && !idempotencyKey.isBlank()
        ? idempotencyKey.trim()
        : UUID.randomUUID().toString();
```

A tabela tem `unique (user_email, provider, idempotency_key)`. Se o cliente
manda uma chave, repetir a requisição devolve o resultado já gravado sem
re-executar — correto, e o mesmo formato de `xp_events` e `simulated_orders`.

Se o cliente **não** manda, o servidor gera um UUID aleatório. Um UUID aleatório
nunca colide com nada. Consequência: **cada chamada sem chave insere uma linha
nova**, para sempre.

E `POST /api/investments/sync` **não está no rate limiter** (§5). Ver drill 3.

### 4.4 A linha de auditoria usa e-mail, não id de usuário

```java
@Column(name = "user_email", nullable = false, length = 255)
private String userEmail;
```

Toda outra tabela do sistema referencia `user_id bigint` com FK para
`identity.jf_users` — `achievement_unlocks`, `xp_events`, `activity_log`,
`jf_pets`. Esta usa uma string de e-mail, **sem FK**.

Duas consequências:

- Não há `on delete cascade`. Apagar um usuário (LGPD) deixa as linhas de sync
  para trás, com o e-mail dele dentro.
- Se um dia existir troca de e-mail (hoje não existe — já registrado como
  demanda P0), o histórico de sync do usuário se parte em dois.

### 4.5 Reconciliação foi deliberadamente não construída

Mesmo no caminho de sucesso, as posições buscadas **não** são gravadas em
`jf_investments`. O javadoc do caso de uso explica:

> *"mesclar posições externas com os lotes que o usuário digitou à mão
> (substituir? mesclar? sinalizar conflitos?) é uma decisão de produto real que
> precisa de um contrato de provedor concreto para ser desenhada, não algo para
> chutar enquanto a porta está permanentemente desligada."*

Um fetch bem-sucedido registra apenas a contagem de posições.

Isso é o oposto do que a fatia 09 encontrou em `/configure`, onde a decisão
"substituir tudo" **foi** tomada por omissão e apagava carteiras. Aqui a
ausência de decisão está declarada como ausência, em vez de virar um
comportamento destrutivo por padrão.

---

## 5. Dados persistidos

```
real_portfolio.real_portfolio_sync_log
  id               bigint identity
  user_email       varchar(255) not null     ← sem FK (§4.4)
  provider         varchar(50)  not null
  idempotency_key  varchar(100) not null
  status           varchar(20)  not null     ← check in ('DISABLED','COMPLETED','FAILED')
  started_at       timestamp    not null
  finished_at      timestamp    not null
  message          varchar(500)
  unique (user_email, provider, idempotency_key)
  index  (user_email)
```

`finished_at` é `Instant.now()` no momento do `save`. Para um resultado
`DISABLED`, `started_at` e `finished_at` ficam a microssegundos de distância —
honesto, mas a coluna só passa a medir algo real quando o adapter existir.

O rate limiter (`RateLimitingFilter.RULES`) cobre `/auth/login`,
`/auth/register`, `/auth/forgot-password`, `/api/v1/learning/progress`,
`/api/v1/achievements`, `/api/v1/missions`, `/api/v1/gamification/summary` e
`/api/v1/learning/lessons/{id}/complete`. **`/api/investments/sync` não está na
lista.**

---

## 6. Modos de falha

| Falha | O que acontece | Visível? |
|---|---|---|
| Chamada normal, hoje | `200` + `DISABLED` + uma linha nova no log | Não — ninguém chama |
| Mesma chave de idempotência repetida | Devolve o resultado já gravado | Não |
| Sem chave de idempotência | **Uma linha nova a cada chamada** | Não |
| Sessão Academy chama | `403` no `SecurityConfig`, antes do controller | Não |
| Provedor indisponível (futuro) | `FAILED`, `200` ao cliente, nunca 500 | Sim, no `message` |
| Mensagem de erro > 487 caracteres (futuro) | **O insert falha e a auditoria se perde** | Ver drill 4 |

---

## 7. Drills

<details>
<summary><b>Drill 1 —</b> Qual é a diferença de filosofia entre este adapter e o `BrapiInvestmentApiClient` da fatia 10?</summary>

Os dois lidam com "a credencial não está configurada". As respostas eram opostas.

- **B3**: `isEnabled()` devolve `false`, o chamador nem tenta, e chamar mesmo
  assim lança exceção. Nenhum dado é inventado.
- **brapi (antes da correção)**: devolvia uma cotação placeholder de R$ 50,00,
  que entrava direto no cálculo da carteira real do usuário.

A diferença não é sorte. O javadoc de `RealPortfolioSyncPort` **manda**:
*"Implementations must throw rather than fabricate a result"*. A regra ficou
escrita na interface, onde qualquer implementação futura a encontra.

O `InvestmentApiPort` da brapi não tem essa frase.
</details>

<details>
<summary><b>Drill 2 —</b> Por que existe uma linha de auditoria para uma tentativa que nunca sai do lugar?</summary>

Porque a garantia é *"toda tentativa é auditada"*, não *"toda tentativa
bem-sucedida é auditada"*.

Se um dia alguém perguntar "quantos usuários tentaram importar da corretora
antes de existir integração?", a resposta está no banco. Se `DISABLED` não fosse
gravado, o produto teria zero visibilidade sobre demanda por uma funcionalidade
que ele não tem.

É o mesmo princípio do `activity_log` da fatia 04: ledger, não contador mutável.
</details>

<details>
<summary><b>Drill 3 —</b> Um cliente chama <code>POST /api/investments/sync</code> em loop, sem corpo. O que acontece com o banco?</summary>

**Uma linha nova por chamada, sem limite.**

Três fatos se compõem:

1. Sem `idempotencyKey`, o servidor gera `UUID.randomUUID()` (§4.3).
2. Um UUID aleatório nunca colide com a constraint `unique`.
3. `/api/investments/sync` **não está no `RateLimitingFilter`**.

Cada linha carrega e-mail, provider, chave, status, dois timestamps e uma
mensagem. Nenhuma expira, nenhuma é limpa.

Compare com `/api/v1/achievements`, que também escreve e **está** limitado — e
cujas escritas são idempotentes por constraint real, não por chave aleatória.

O que salva hoje é só o fato de nenhum cliente chamar a rota. Isso é
circunstância, não proteção.
</details>

<details>
<summary><b>Drill 4 —</b> A integração B3 existe, o provedor devolve um erro longo. O que acontece?</summary>

O caminho de falha pode **ele mesmo falhar**.

```java
} catch (Exception e) {
    RealPortfolioSyncLog logged = syncLogRepository.save(
            email, provider, key, RealPortfolioSyncStatus.FAILED, startedAt,
            "Sync failed: " + e.getMessage());
```

`message` é `varchar(500)`, e nada trunca a string em lugar nenhum. Um
`e.getMessage()` com mais de 487 caracteres — perfeitamente comum em erro de
cliente HTTP, que costuma embutir URL e corpo da resposta — faz o `insert`
estourar. A exceção do `save` não está dentro de nenhum catch, então ela sobe:
**500 ao cliente, e nenhuma linha de auditoria**.

A garantia de "toda tentativa é auditada" quebra exatamente no caso em que ela
mais importa. E o mesmo `e.getMessage()` volta ao cliente dentro do
`RealPortfolioSyncResultDTO` — se ele contiver a URL do provedor ou fragmento de
credencial, vaza.

Duas correções, ambas de uma linha: truncar a mensagem antes de gravar, e não
devolver a mensagem crua do provedor ao cliente.
</details>

<details>
<summary><b>Drill 5 —</b> O que precisa mudar no dia em que a integração B3 for contratada?</summary>

Em teoria, **um arquivo**: `B3RealPortfolioSyncAdapter`. Cliente HTTP e mapeamento
para `ExternalPositionDTO` entram ali; nem o caso de uso nem o controller nem o
`SecurityConfig` mudam.

Na prática, mais três coisas que a costura não cobre:

1. **A reconciliação** (§4.5) — substituir, mesclar ou sinalizar conflito com os
   lotes manuais. É a decisão de produto que foi adiada, não resolvida.
2. **O rate limit** (drill 3) — a rota passa a fazer chamada externa por
   requisição, não só um insert.
3. **A truncagem da mensagem** (drill 4) — o caminho de falha vira alcançável.

A costura entrega o que promete: o *lugar* está pronto. O que ela não entrega, e
não pretende entregar, é a decisão.
</details>

---

## 8. Se você fosse mudar algo aqui

- **Adicionar `/api/investments/sync` ao `RateLimitingFilter`** → uma linha, e é
  o único endpoint do projeto que escreve sem limite. Ver drill 3.
- **Truncar `message` antes de gravar** → uma linha, e conserta um caminho de
  falha que hoje falha. Ver drill 4.
- **Migrar `user_email` para `user_id` com FK** → alinha com todas as outras
  tabelas e resolve o rastro LGPD. Ver §4.4.
- **Copiar a frase do javadoc de `RealPortfolioSyncPort` para `InvestmentApiPort`**
  → *"must throw rather than fabricate a result"*. A brapi já foi corrigida; a
  regra ainda não está escrita onde impede a recaída.
