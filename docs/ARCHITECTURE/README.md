# Atlas Técnico — Petrimonium

Este diretório é a **referência do estado atual** do ecossistema Petrimonium.
Não é changelog. Não registra o que mudou nem quando — para isso existem
`docs/ECOSYSTEM.md`, `docs/BACKEND_MODULE_PLAN.md` e o histórico do git.

Aqui só existe uma pergunta: **como isso funciona hoje?**

## Para quem isto foi escrito

Para o dono do produto que precisa ser o desenvolvedor principal dele.
O objetivo não é "ter documentação" — é conseguir, sem ajuda:

1. Ler uma funcionalidade e saber por quais arquivos o dado passa.
2. Abrir um bug em produção e saber em qual camada olhar primeiro.
3. Ler um PR (humano ou gerado por IA) e saber se ele quebra alguma regra do sistema.

## A regra que faz isto funcionar

> **Nada entra na `main` sem que você consiga traçar a fatia inteira de cabeça.**

O Atlas é a ferramenta de estudo, não o produto final. Se um documento aqui
está desatualizado, ele é pior que inexistente — atualizar a fatia faz parte
do PR que muda o código dela.

## Como está organizado

- [`00-visao-geral.md`](00-visao-geral.md) — o mapa: os 3 repositórios, o que é
  compartilhado, o que é isolado, inventário completo de endpoints, schemas e
  módulos. **Leia isto primeiro, uma vez.**
- [`fatias/`](fatias/) — uma funcionalidade por arquivo, sempre ponta a ponta.

## O formato de uma fatia (e por que ele é assim)

Documentação organizada por camada ("aqui ficam os repositories") não ensina
nada, porque não é assim que você depura. Quando algo quebra, você segue o
**caminho do dado**. Então toda fatia segue esse caminho:

```
Tela → Widget/Controller Flutter → Repository → DataSource HTTP
     → [rede] →
Filtro de segurança → Controller Spring → Use Case → Domínio → Repositório JPA → Tabela
```

E toda fatia tem, obrigatoriamente, as mesmas sete seções:

| Seção | Responde a pergunta |
|---|---|
| **1. O que o usuário vê** | Qual é a funcionalidade, em linguagem de produto |
| **2. Caminho do dado** | Diagrama de sequência real, com nomes de arquivos |
| **3. Arquivos que importam** | A lista curta — não todos, só os que decidem algo |
| **4. Regras de negócio** | O que o código garante e *por quê* (a decisão por trás) |
| **5. Dados persistidos** | Tabelas, colunas, migrations |
| **6. Modos de falha** | O que acontece quando dá errado — a parte que ninguém documenta |
| **7. Drills** | Perguntas que só se respondem lendo o código, com gabarito |

A seção 7 é a que transforma leitura em domínio. Leia a fatia, feche o
documento, responda os drills de cabeça, e só depois confira.

## O que fazer com o que você encontra

Escrever uma fatia sempre revela coisas: bugs, débito, decisões que ninguém
tomou. O Atlas **descreve** — ele não é o lugar de rastrear a correção.

Todo achado vai para o banco **Demandas — Petrimonium** no Notion, que já tem a
taxonomia certa (`Tipo`, `Origem`, `Prioridade`, `Produto`, `Evidencia no
codigo`). A fatia registra o fato e o porquê; a demanda registra o que fazer.
Cada uma aponta para a outra.

Um bug reproduzido em runtime, e não só por leitura de código, também merece
entrada na página **Correção de Bugs — Petrimonium**, que exige reprodução.

## Índice de fatias

Legenda: ✅ escrita · ⬜ pendente

### Núcleo compartilhado (Wallet + Academy)

| # | Fatia | Status |
|---|---|---|
| 01 | [Autenticação e `app_context`](fatias/01-auth-e-app-context.md) | ✅ |
| 02 | [Onboarding e `InvestorProfile`](fatias/02-onboarding-investorprofile.md) | ✅ |
| 03 | [Pet / companheiro (`/api/pets`)](fatias/03-pet-companheiro.md) | ✅ |
| 04 | [Gamificação: XP, nível e streak](fatias/04-gamificacao-xp-streak.md) | ✅ |
| 05 | [Mentor: chat, prompt por contexto e conversas](fatias/05-mentor.md) | ✅ |
| 06 | Configurações e idioma (tradução) | ⬜ |
| 07 | [Infra transversal: filtros, rate limit, erros e config](fatias/07-infra-transversal.md) | ✅ |
| 08 | [Flyway: migrations, schemas e os dois ambientes](fatias/08-flyway-schemas.md) | ✅ |

### Domínio Wallet (dinheiro real)

| # | Fatia | Status |
|---|---|---|
| 09 | [Carteira real: cadastro, posições e cálculo](fatias/09-carteira-real.md) | ✅ |
| 10 | Cotações e busca de ativos (brapi.dev) | ⬜ |
| 11 | Resumo, alocação e histórico da carteira | ⬜ |
| 12 | Proventos e detalhes do ativo | ⬜ |
| 13 | Conquistas (`/api/v1/achievements`) | ⬜ |
| 14 | Sync B3 — arquitetura preparatória, adapter desligado | ⬜ |
| 15 | Shell de navegação do app Wallet | ⬜ |
| 16 | Onboarding do Wallet | ⬜ |

### Domínio Academy (dinheiro fictício)

| # | Fatia | Status |
|---|---|---|
| 17 | Catálogo Academy: domínios, escolas, módulos, aulas | ⬜ |
| 18 | Progresso de aprendizagem e conclusão de aula | ⬜ |
| 19 | Laboratório financeiro (simuladores) | ⬜ |
| 20 | Missões (`/api/v1/missions`) | ⬜ |
| 21 | Carteira simulada: portfólio, ordens e reset | ⬜ |
| 22 | Shell de navegação do app Academy | ⬜ |
| 23 | Onboarding do Academy (10 telas) | ⬜ |

### Transversal aos apps

| # | Fatia | Status |
|---|---|---|
| 24 | Design system, tema e widgets compartilhados | ⬜ |
| 25 | Estratégia de testes: o que é testado e como rodar | ⬜ |
