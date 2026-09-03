# Fatia 12 — Proventos e detalhes do ativo

> Verificado em 2026-09-02 lendo o código. Toda linha aqui é rastreável a um arquivo.

Esta fatia mostra ao usuário **dois números diferentes sobre proventos**, com
significados incompatíveis. Entender qual é qual é o ponto principal aqui — e é
também o melhor exemplo, no projeto inteiro, de como sinalizar uma estimativa
corretamente.

---

## 1. O que o usuário vê

A aba **Proventos** (existe no Wallet; foi removida do Academy na Etapa 7)
mostra:

- Uma **estimativa** de renda passiva mensal e anual, por categoria de ativo.
- O **Radar de Proventos**: pagamentos anunciados e recebidos, por ativo.
- Uma barra de evolução de proventos nos últimos 12 meses.

E na tela de detalhes de um ativo: fundamentos, setor, logo, histórico de
proventos e a posição do próprio usuário naquele papel.

---

## 2. Os dois números, lado a lado

| | Estimativa de renda passiva | Radar de Proventos |
|---|---|---|
| Onde é calculado | **Cliente** (`PassiveIncomeEstimator`) | **Backend** (`GetDividendRadarUseCase`) |
| De onde vem | Rendimento **presumido** por categoria | Pagamentos **confirmados** pelo provedor |
| Escala por | Valor atual da alocação | Quantidade real que o usuário detinha |
| Ativo sem dados | Contribui com a presunção da categoria | Contribui com **nada** |
| Rotulado na interface | **Sim** | É factual, não precisa |

<!-- São dois modelos diferentes respondendo à mesma pergunta do usuário
     ("quanto eu recebo?"). A separação é deliberada e está documentada no
     javadoc do use case, mas nada na tela explica por que os dois números
     divergem — e eles divergem por construção. -->

---

## 3. Arquivos que importam

| Arquivo | Papel |
|---|---|
| `application/investment/usecase/GetDividendRadarUseCaseImpl.java` | Proventos confirmados, escalados por posse real |
| `application/investment/usecase/GetAssetDetailsUseCaseImpl.java` | Orquestra cache → provedor → posição |
| `application/investment/service/AssetDetailsResponseMapper.java` | Payload cru → DTO |
| `features/portfolio/domain/services/passive_income_estimator.dart` | A estimativa |
| `features/portfolio/domain/entities/investment_type_display.dart:62` | **Os rendimentos presumidos** |
| `features/portfolio/presentation/widgets/passive_income_card.dart:69` | **O rótulo de estimativa** |

---

## 4. Regras de negócio (e o porquê de cada uma)

### 4.1 O Radar nunca fabrica um pagamento que o usuário não recebeu

Esta é a parte mais cuidadosa da fatia, e vale ler o código:

```java
double heldAsOfDataCom = quantityHeldAsOf(tickerLots, dividend.dataCom(), currentQuantity);
if (heldAsOfDataCom <= 0) continue;
```

Para um provento **já pago**, o use case soma apenas os lotes comprados **em ou
antes da data-com** — a última data em que um detentor qualificava para aquele
pagamento. Quem comprou depois não recebeu, e por isso não entra.

O comentário diz por quê: escalar pela quantidade de hoje *"fabricaria um
pagamento que ele nunca recebeu"*.

Para proventos **anunciados e ainda não pagos**, escala pela posição atual — a
melhor estimativa do que ele deterá na data do pagamento.

<!-- O fallback quando o provedor não informa data-com é a quantidade atual, e
     o próprio comentário chama isso de "esforço honesto, não fabricação". É
     uma linha tênue, mas defensável: sem data-com não há como filtrar, e omitir
     o pagamento inteiro seria igualmente incorreto. -->

### 4.2 A estimativa usa rendimentos presumidos, fixos por categoria

```dart
InvestmentTypeEnum.STOCKS       => 0.05,   // 5% a.a.
InvestmentTypeEnum.FIXED_INCOME => 0.11,   // 11% a.a.
InvestmentTypeEnum.REAL_ESTATE  => 0.08,   // 8% a.a.
InvestmentTypeEnum.FUNDS        => 0.04,   // 4% a.a.
InvestmentTypeEnum.CRYPTO       => 0.0,
InvestmentTypeEnum.OTHERS       => 0.0,
```

São constantes no código, iguais para todo usuário e todo ativo da categoria.
Um FII que paga 12% e um que paga 4% contribuem igual.

### 4.3 A estimativa **é** rotulada — e é o modelo a seguir

O card exibe, literalmente:

> *"Estimativa baseada no rendimento médio histórico de cada categoria de ativo
> — não representa pagamentos confirmados."*

E o javadoc do `assumedAnnualYield` reforça: *"sempre exibido ao usuário como
'estimado', nunca apresentado como pagamento confirmado"*.

<!-- Comparação que vale fazer: este é exatamente o problema do gráfico
     "Evolução Patrimonial" (fatia 11, regra 4.4) — um número derivado de um
     modelo, não de dados reais. Aqui está resolvido com uma frase; lá não está
     resolvido de jeito nenhum.

     Ou seja: a omissão no gráfico não é uma filosofia do projeto, é um
     esquecimento. O padrão correto já existe e está a poucos arquivos de
     distância. -->

### 4.4 Renda fixa: 11% de renda passiva presumida, 0% de valorização

Cruzando esta fatia com a 11: um Tesouro Selic contribui com **11% ao ano** para
a estimativa de renda passiva, e ao mesmo tempo aparece na carteira valendo
exatamente o preço de compra, com ganho zero **para sempre** — porque
`fetchCurrentPrice` devolve o preço de compra para `FIXED_INCOME`.

Cada modelo é coerente consigo mesmo. Juntos, dizem ao usuário que o mesmo ativo
rende 11% e não rende nada.

### 4.5 Histórico limitado a 12 entradas por ticker

`MAX_HISTORY_PER_TICKER = 12`, aplicado depois da ordenação por data
decrescente. O comentário explica: limita o payload para tickers com décadas de
histórico (PETR4).

O corte é **por ticker**, não global — então uma carteira com 20 ativos pode
devolver até 240 entradas.

### 4.6 Detalhes do ativo têm uma cadeia de degradação explícita

```
cache (5 min) → getEnrichedQuote → getQuote → mapper.unavailable(...)
```

Cada nível é mais pobre que o anterior, e o último **é explícito sobre a
indisponibilidade** em vez de devolver zeros. A resposta ainda carrega um campo
`status` (`"CACHED"` etc.).

Mesmo servindo do cache, a **posição do usuário é recalculada na hora** — o dado
de mercado pode ser de até 5 minutos atrás, a posição nunca é velha.

<!-- Terceiro exemplo, na mesma fatia, de sinalização honesta feita direito:
     `unavailable` existe, `status` existe. Reforça que a lacuna da fatia 11 é
     pontual. -->

### 4.7 O Radar continua em `Double`, deliberadamente

```java
double currentQuantity = tickerLots.stream()
        .map(Investment::quantity)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .doubleValue();
```

Converte de `BigDecimal` para `double` de propósito, com comentário apontando
para `BACKEND_MODULE_PLAN.md §12`: a cadeia de proventos ficou fora da migração
de precisão da V24, junto com os ~40 campos de fundamentos do asset-details.

<!-- É débito registrado, não descuido. Mas note a assimetria: a posição do
     usuário é BigDecimal porque "isto é dinheiro real", e o provento que ele
     recebe desse mesmo ativo é double. -->

---

## 5. Dados persistidos

**Nenhum.** Nem proventos, nem fundamentos, nem detalhes de ativo.

Tudo é buscado no provedor a cada requisição (com o cache de 5 minutos do
asset-details como única memória, em processo). Consequência: **não existe
histórico próprio de proventos recebidos** — se a brapi deixar de reportar um
pagamento antigo, ele desaparece da tela do usuário.

---

## 6. Modos de falha

| Situação | O que acontece | Onde |
|---|---|---|
| Provedor sem dados para um ticker | Contribui com **nada** ao Radar | regra 4.1 |
| Provento sem data-com informada | Escala pela quantidade atual | regra 4.1 |
| Usuário comprou depois da data-com | Pagamento **não** aparece no histórico dele | regra 4.1 |
| Enriched indisponível | Cai para `getQuote` | regra 4.6 |
| Ambos indisponíveis | `mapper.unavailable(...)`, explícito | regra 4.6 |
| Detalhes servidos do cache | Dado de mercado até 5 min velho; **posição sempre fresca** | regra 4.6 |
| Carteira só com cripto | Estimativa de renda passiva = **zero** | regra 4.2 |
| Ticker com décadas de histórico | Truncado em 12 entradas | regra 4.5 |

---

## 7. Drills

<details>
<summary><b>Drill 1 —</b> O usuário comprou PETR4 na semana passada. A Petrobras pagou dividendos no mês passado. Ele vê esse pagamento?</summary>

**Não** — e é isso que o código se esforça para garantir.

Para proventos já pagos, o use case soma apenas os lotes comprados **em ou antes
da data-com**. Como o lote dele é posterior, `heldAsOfDataCom` é zero e a entrada
é descartada.

Escalar pela quantidade de hoje seria mais simples e mostraria um número maior —
e seria um pagamento que ele nunca recebeu. O comentário no código usa
exatamente essa palavra: *"fabricaria"*.
</details>

<details>
<summary><b>Drill 2 —</b> A estimativa de renda passiva diz R$ 400/mês e o Radar mostra R$ 90 recebidos. Qual está errado?</summary>

Nenhum. **São dois modelos diferentes respondendo à mesma pergunta.**

A estimativa multiplica o valor atual de cada categoria por um rendimento
presumido fixo (5% ações, 11% renda fixa, 8% FIIs, 4% fundos). O Radar soma
apenas pagamentos que o provedor confirmou e que o usuário de fato tinha direito
a receber.

Divergir é o comportamento esperado. O que **não** existe é qualquer explicação
na tela de por que os dois números diferem.
</details>

<details>
<summary><b>Drill 3 —</b> Por que a estimativa de renda passiva tem um aviso e o gráfico "Evolução Patrimonial" não?</summary>

Não há razão técnica — e é essa a conclusão útil.

Os dois são números derivados de modelo, não de dados reais. O
`PassiveIncomeEstimator` resolve isso com uma frase no card, e o javadoc do
`assumedAnnualYield` até declara a regra: *"sempre exibido como estimado, nunca
como pagamento confirmado"*.

O gráfico da fatia 11 tem exatamente o mesmo problema e nenhuma frase.
**O padrão correto já existe no projeto, a poucos arquivos de distância.** Isso
torna a lacuna um esquecimento, não uma escolha — e a correção, uma cópia.
</details>

<details>
<summary><b>Drill 4 —</b> Um usuário só de Tesouro Direto abre a Carteira e depois a aba Proventos. O que os dois números dizem?</summary>

Coisas contraditórias.

Na **Carteira**: valor atual igual ao investido, ganho zero — `fetchCurrentPrice`
devolve o preço de compra para `FIXED_INCOME` (fatia 11, regra 4.2).

Em **Proventos**: estimativa de **11% ao ano**, porque é o rendimento presumido
da categoria.

O mesmo ativo, na mesma sessão, rendendo 11% numa tela e nada na outra. Cada
modelo é coerente consigo mesmo; a incoerência aparece só quando o usuário olha
as duas.
</details>

<details>
<summary><b>Drill 5 —</b> A brapi para de reportar um provento pago em 2023. O que acontece com o histórico do usuário?</summary>

Ele **desaparece** da tela.

Nada de proventos é persistido — nem pagamentos, nem fundamentos. Tudo é buscado
no provedor a cada requisição, com apenas 5 minutos de cache em memória para os
detalhes de ativo.

Não existe histórico próprio: o que o usuário vê é sempre o que o provedor
responde agora. É o mesmo padrão da fatia 10, e tem a mesma consequência — o
passado é reconstruído, não guardado.
</details>

---

## 8. Se você fosse mudar algo aqui

- **Explicar a diferença entre os dois números** → uma linha na aba Proventos.
  Ver drill 2.
- **Copiar o rótulo de estimativa para o gráfico da fatia 11** → o texto já
  existe e está pronto. Ver drill 3.
- **Resolver a contradição da renda fixa** → ou o rendimento presumido some, ou
  a valorização aparece. Manter os dois é o pior caso. Ver drill 4.
- **Persistir proventos recebidos** → daria histórico próprio e independente do
  provedor. Ver drill 5.
- **Migrar o Radar para `BigDecimal`** → débito já registrado no
  `BACKEND_MODULE_PLAN.md` §12.
