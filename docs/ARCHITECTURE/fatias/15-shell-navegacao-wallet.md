# Fatia 15 — Shell de navegação do Wallet

> Verificado em 2026-09-03 lendo o código. Toda linha aqui é rastreável a um arquivo.

Esta fatia é a moldura: o que decide qual tela o app abre no arranque, quantas
abas existem, e o que acontece quando o usuário troca de contexto. É curta em
arquivos e densa em decisões — e é onde mora o único caminho do produto que
**desloga o usuário sozinho**.

---

## 1. O que o usuário vê

Abre o app e vê um splash com uma raposa e a frase *"Inicializando Módulo de
Comandante..."*. Depois, dependendo do estado: login, boas-vindas do Mentor,
configuração rápida, ou o painel.

No painel: uma AppBar com o pet, o nome do produto, sino de proventos,
engrenagem e sair; e uma barra inferior com **duas ou três abas** — Início,
Proventos (às vezes) e Mentor.

---

## 2. Caminho da decisão de arranque

```
main()
  ├─ ApiConstants.assertConfiguredForRelease()   ← falha alto se faltar URL
  ├─ SentryFlutter.init(dsn: --dart-define)
  └─ appRunner:
       ├─ Translator.load()
       ├─ ThemeController.load()
       ├─ onboardingStateRepository.incrementSessionCount()
       └─ runApp(MyApp)
             └─ ValueListenableBuilder<idioma>
                  └─ ValueListenableBuilder<tema>
                       └─ MaterialApp(home: FutureBuilder(
                              future: StartRouteResolver().resolve()   ← §4.3
                          ))

StartRouteResolver.resolve()
  ├─ isLoggedIn()?           não → login
  ├─ _ensureDefaultPet()     ← GET /api/pets/status  (rede!)
  ├─ hasSeenMentorWelcome()? não → mentorWelcome
  ├─ hasCompletedQuickSetup()? não → quickSetup
  ├─ tudo resolvido          → home
  └─ catch (_) → logout() → login          ← §4.2
```

---

## 3. Arquivos que importam

| Arquivo | Papel |
|---|---|
| `main.dart` | Bootstrap, Sentry, os dois `ValueListenableBuilder`, o `FutureBuilder` de rota, o splash. |
| `core/navigation/start_route_resolver.dart` | As regras de arranque. **O único lugar que desloga sem o usuário pedir.** |
| `features/dashboard/presentation/screens/dashboard_screen.dart` | O shell: AppBar, `IndexedStack`, barra inferior, pet persistente. |
| `features/dashboard/presentation/services/dashboard_tab_router.dart` | Mapeamento puro aba → voz do pet. Extraído para ser testável. |
| `features/investment/data/models/investment_type_enum.dart` | `paysDividends` — decide se a aba Proventos existe. |

O `dashboard` não tem camada de domínio própria. A pequena regra de negócio que
existe (qual voz do companheiro cada aba recebe) foi puxada para o
`DashboardTabRouter` justamente para não ficar embutida na tela.

---

## 4. Regras de negócio — e o porquê

### 4.1 A barra inferior tem 2 ou 3 abas, decidido em tempo de execução

```dart
List<int> get _visibleTabIndices => [
  DashboardTabRouter.homeTab,
  if (_portfolioController.hasDividendPayingHoldings)
    DashboardTabRouter.passiveIncomeTab,
  DashboardTabRouter.mentorTab,
];
```

E `hasDividendPayingHoldings` é `holdings.any((h) => h.type.paysDividends)`, com:

```dart
STOCKS => true,  REAL_ESTATE => true,  FUNDS => true,
FIXED_INCOME => false,  CRYPTO => false,  OTHERS => false,
```

A tradução do índice está feita com cuidado e documentada — `_selectedIndex` é
um **id lógico** (as constantes do router), não uma posição na lista visível:

```dart
final currentPosition = visible.indexOf(_selectedIndex);
...
currentIndex: currentPosition == -1 ? 0 : currentPosition,
onTap: (position) => _onTabSelected(visible[position]),
```

E se a aba some no meio da sessão (o usuário vendeu as ações), `_onPortfolioChanged`
devolve o usuário para o Início em vez de deixá-lo numa aba sem item de nav.

<!-- Isso está bem feito. É o tipo de detalhe que quase sempre vira um
     RangeError em produção, e aqui foi resolvido explicitamente e comentado. -->

**O que não fecha:** um usuário só de Tesouro Direto **nunca vê a aba
Proventos**. Cruzando com a fatia 12: `FIXED_INCOME` tem o **maior** rendimento
presumido de todos (`assumedAnnualYield = 0.11`). O produto calcula 11% ao ano
para exatamente a classe de ativo cujo dono não tem onde ver isso — e a
Carteira, que ele vê, mostra 0%.

### 4.2 Falha de rede no arranque **desloga o usuário**

```dart
try {
  await _ensureDefaultPet();          // GET /api/pets/status
  ...
} catch (_) {
  // Any failure while resolving pet/onboarding state is treated as "not
  // safely resumable" — log the user out rather than risk stranding
  // them on a screen that assumes state that couldn't be loaded.
  await _authRepository.logout();
  return StartRoute.login;
}
```

`getPetStatus()` é `GET /api/pets/status` e **lança** em qualquer não-200 — e o
`ApiClient` lança `TimeoutException` após 15s sem resposta.

Ou seja: abrir o app no metrô, sem sinal, custa a sessão.

O comentário justifica a decisão pelo caso do **token velho** (o usuário não
existe mais no servidor — trivial de reproduzir em dev, porque o H2 é em memória
e reseta a cada restart). Esse caso é real e a decisão faz sentido para ele. Mas
`catch (_)` não distingue *"seu usuário não existe mais"* de *"o wi-fi caiu"*.

E isso contradiz uma decisão explícita da fatia 01, no `ApiClient`:

| Situação | `ApiClient` (fatia 01) | `StartRouteResolver` (aqui) |
|---|---|---|
| Refresh falha com não-200 | Limpa tokens, vai para login | — |
| Refresh falha por **erro de rede** | **Devolve o 401 original, preserva a sessão** | — |
| `/api/pets/status` falha por não-200 | — | Logout |
| `/api/pets/status` falha por **erro de rede** | — | **Logout** |

A terceira saída do `ApiClient` — *"wi-fi ruim não custa a sessão do usuário"* —
existe justamente porque erro de rede não é a mesma afirmação que sessão
inválida. Aqui, no arranque, ela não existe.

<!-- Isso não é descuido: há um teste chamado literalmente
     'network down' que fixa esse comportamento. É uma decisão tomada, pinada
     por teste, que contradiz uma decisão tomada em outro arquivo. O Atlas
     registra a contradição; qual das duas vence é chamada de produto. -->

### 4.3 O `future` da rota é recriado a cada rebuild

```dart
home: FutureBuilder<StartRoute>(
  future: StartRouteResolver().resolve(),
  ...
)
```

Esse `FutureBuilder` está dentro de **dois** `ValueListenableBuilder` — idioma e
tema. A expressão `StartRouteResolver().resolve()` é avaliada a cada `build`.

Trocar o tema ou o idioma nas Configurações portanto:

1. Constrói um `StartRouteResolver` novo.
2. Dispara **outro `GET /api/pets/status`**.
3. Volta o `FutureBuilder` para `waiting` (splash na raiz da pilha).
4. E, se essa chamada falhar, aplica §4.2 — **o usuário é deslogado por ter
   trocado o tema**.

O projeto já percebeu metade disso: o comentário do `DashboardScreen` diz que
ele escuta o idioma por conta própria *"em vez de depender só do rebuild do
`MyApp` no topo (que reseta a resolução de rota do `FutureBuilder` e faria o
splash piscar a cada troca de idioma)"*. A tela foi blindada; a causa continua.

A correção padrão é resolver o future **uma vez** (`initState`, guardado num
campo) em vez de dentro do `build`.

### 4.4 O pet do Wallet é provisionado em silêncio

```dart
const String kDefaultWalletPetName = 'Nino';
...
if (!hasPet) await _petRepository.configurePet(PetSpecieEnum.DOG);
if (!hasName) await _mascotRepository.saveName(kDefaultWalletPetName);
```

Um cadastro que começa pelo Wallet não vê seletor de pet. O raciocínio está
escrito: o pet é um companheiro **compartilhado** entre os apps, não algo que o
Wallet esteja pedindo para o usuário configurar. Quem vem da Academy já tem um.

O cuidado chega ao detalhe de escolher um nome que **não pareça sintético** —
"Nino" é uma das sugestões que o `PetNameField` da Academy oferece.

Duas consequências que já estão catalogadas em outras fatias:

- O nome fica **só no `SharedPreferences`** (`MascotRepositoryImpl.loadProfile`
  lê de prefs). O backend nunca sabe que o pet se chama Nino, e o Mentor o chama
  de "DOG Companion" (fatia 03).
- As telas antigas de nomear pet / meta financeira / tutorial continuam no
  repositório, inalcançáveis a partir do arranque. Está declarado como
  "deferred cleanup", não como esquecimento.

### 4.5 O splash mostra uma raposa que não é o pet de ninguém

```dart
Image.asset('assets/images/generated_fox.png', height: 120, ...)
...
Text('Inicializando Módulo de Comandante...', ...)
```

Existem assets para as sete espécies (`generated_dog.png`, `generated_cat.png`,
`generated_owl.png`, …). O splash usa a raposa, fixa. O pet padrão do Wallet é
**DOG**.

E a frase está em português cru, fora do `Translator` — o único texto de
interface do app que não passa por ele. `Translator.load()` já rodou quando o
splash desenha, então não é limitação técnica.

---

## 5. Dados persistidos

Nada é gravado por esta fatia. Ela **lê**:

| Chave / rota | Onde | Quem escreve |
|---|---|---|
| tokens | `flutter_secure_storage` | `AuthRepository` (fatia 01) |
| `GET /api/pets/status` | rede | — |
| perfil do pet (nome, estágio) | `SharedPreferences` | `MascotRepositoryImpl` |
| `hasSeenMentorWelcome` | `SharedPreferences` | `OnboardingStateRepository` |
| `hasCompletedQuickSetup` | `SharedPreferences` | `OnboardingStateRepository` |
| contador de sessões | `SharedPreferences` | `main()` |

Tudo de onboarding é **local por dispositivo** — já registrado como demanda P0
("preferências não sincronizadas via identidade compartilhada"). Trocar de
aparelho refaz o mini-onboarding do Wallet.

---

## 6. Modos de falha

| Falha | O que acontece | Visível? |
|---|---|---|
| Sem rede no arranque | 15s de splash → **logout** | Sim, e mal explicado |
| Token válido de usuário deletado | Logout — o caso que a regra existe para tratar | Sim |
| Troca de tema/idioma | Nova chamada de rede + splash na raiz; falha → logout | Parcialmente |
| `SessionExpiredEvent` do `ApiClient` | `pushAndRemoveUntil` para login, limpando a pilha | Sim |
| Usuário vende todas as ações com a aba Proventos aberta | Volta para o Início automaticamente | Sim, e bem |
| `generated_fox.png` não carrega | `errorBuilder` → ícone de patinha | Sim |
| Release sem `API_BASE_URL` | `StateError` no boot, antes de qualquer tela | Sim |

---

## 7. Drills

<details>
<summary><b>Drill 1 —</b> Um usuário abre o app no metrô, sem sinal. O que acontece com a sessão dele?</summary>

**Ele é deslogado.**

`resolve()` chama `_ensureDefaultPet()` → `getPetStatus()` → `GET /api/pets/status`.
Sem rede, o `ApiClient` lança `TimeoutException` após 15s. O `catch (_)` do
resolver chama `logout()` e devolve `StartRoute.login`.

Quinze segundos de splash, e a tela de login com os campos vazios.

E o `ApiClient` — no mesmo app — faz o oposto de propósito quando o refresh
falha por rede: devolve o 401 original e **preserva** a sessão, porque erro de
rede não é a mesma afirmação que sessão inválida.

Os dois comportamentos estão testados. O do resolver tem um teste chamado
literalmente `'network down'`.
</details>

<details>
<summary><b>Drill 2 —</b> O usuário está em Configurações e troca de Escuro para Claro. Quantas chamadas de rede isso dispara?</summary>

**Uma** — `GET /api/pets/status`.

`ThemeController.themeModeNotifier` dispara → `ValueListenableBuilder` reconstrói
→ o `MaterialApp` é reconstruído → a expressão `StartRouteResolver().resolve()`
no `future:` do `FutureBuilder` é **reavaliada**.

O `FutureBuilder` volta para `waiting`, então a raiz da pilha vira o splash. O
usuário não vê (está numa rota empilhada), mas ela está lá — e quando ele voltar,
vai encontrar o que aquela nova resolução decidiu.

Se a chamada falhar, ele volta de Configurações **para a tela de login**.
</details>

<details>
<summary><b>Drill 3 —</b> Um usuário tem só Tesouro Direto. Quantas abas ele vê, e por quê?</summary>

**Duas**: Início e Mentor.

`FIXED_INCOME.paysDividends` é `false`, então `hasDividendPayingHoldings` é
falso e a aba Proventos não entra em `_visibleTabIndices`.

A parte que incomoda vem da fatia 12: `FIXED_INCOME.assumedAnnualYield` é
**0.11** — o maior de todos os tipos. O app calcula 11% ao ano de renda passiva
presumida para esse usuário e **não tem onde mostrar**, enquanto a Carteira, que
ele vê, mostra valorização zero (renda fixa nunca é cotada).

Ele só sabe que rende 11% se comprar uma ação.
</details>

<details>
<summary><b>Drill 4 —</b> O `_selectedIndex` é 1 e a aba Proventos some. O que impede um `RangeError`?</summary>

Duas coisas, ambas explícitas.

1. `_onPortfolioChanged` detecta que `_selectedIndex` não está mais em
   `_visibleTabIndices` e volta para `homeTab`.
2. Mesmo se não voltasse, `_buildBottomNav` traduz id lógico → posição:
   `visible.indexOf(_selectedIndex)`, com `currentPosition == -1 ? 0 : ...`.

O `IndexedStack` continua com os três filhos sempre montados e indexado pelo id
lógico — que é o que preserva o estado de cada aba ao trocar.

É um trecho pequeno onde três coisas poderiam desalinhar (id lógico, posição na
nav, índice do stack) e nenhuma desalinha.
</details>

<details>
<summary><b>Drill 5 —</b> Por que o `MyApp` usa um `GlobalKey<NavigatorState>` em vez de `Navigator.of(context)`?</summary>

Porque `SessionExpiredEvent` nasce no `ApiClient`, dentro de `core/`, que não
tem `BuildContext` nenhum — e pode chegar a qualquer momento da vida do app, não
só a partir de um widget montado.

O comentário deixa claro que é a **única** exceção: toda outra navegação do app
continua usando `Navigator.of(context)` local.

O handler faz `pushAndRemoveUntil(..., (route) => false)` — limpa a pilha
inteira. Um lesson screen aberto, o Dashboard, o que estiver na tela: tudo sai.
Os tokens já foram apagados pelo `ApiClient` antes do evento.
</details>

<details>
<summary><b>Drill 6 —</b> Quantas telas o mini-onboarding do Wallet tem, e o que aconteceu com as outras?</summary>

**Duas**: boas-vindas do Mentor e configuração rápida (mercado/moeda).

As telas de nomear o pet, definir meta financeira, tutorial e escolha de
portfólio continuam no repositório, **inalcançáveis a partir do arranque** — o
mesmo padrão de "limpeza adiada" do resto da separação Wallet/Academy, até
decidir se alguma delas reaparece em outro lugar (por exemplo "mudar meta" em
Perfil).

O pet não é perguntado porque ele é compartilhado: quem vem da Academy já tem
um, e um cadastro Wallet-first recebe um DOG chamado "Nino" em silêncio.
</details>

---

## 8. Se você fosse mudar algo aqui

- **Distinguir erro de rede de sessão inválida no `StartRouteResolver`** →
  copiar a terceira saída do `ApiClient`. É o único caminho do produto que
  desloga sozinho. Ver drill 1.
- **Resolver a rota uma vez, no `initState`** → em vez de dentro do `build`.
  Elimina a chamada extra e o risco de logout por troca de tema. Ver drill 2.
- **Splash com o pet do usuário** → os sete assets já existem, e o nome já é
  escolhido com cuidado para não parecer sintético. Ver §4.5.
- **Passar a frase do splash pelo `Translator`** → é o único texto de interface
  que não passa.
