# Petrimonium-Backend — guia para Codex/Claude

Complementa [`/AGENTS.md`](../AGENTS.md) (leia-o primeiro) com o que só faz
sentido aqui: Spring Boot 3 / Java 21 / Postgres, arquitetura hexagonal por
pacote (um único módulo Maven — ver `docs/BACKEND_MODULE_PLAN.md` §0 sobre
por que não é multi-módulo).

## Camadas e a regra de dependência

```
core/domain        entidades puras — sem Spring, sem JPA
      ↑
application/<feature>/{usecase, port, dto, service, exception}
      ↑
infrastructure/{controller, repository, entity, external, config, security}/<feature>
```

A dependência aponta sempre para dentro: `application` nunca importa
`infrastructure`; `core/domain` nunca importa `application` nem
`infrastructure`. Isso é conferido por ArchUnit
(`archunit-junit5`), não só seguido por convenção.

`presentation/` existe hoje só para `auth` (request/response DTOs de
autenticação) — não é um padrão geral do repositório ainda. Não crie
`presentation/<feature>/` para uma feature nova só por simetria; use
`application/<feature>/dto/` como o resto do código faz, a menos que o
usuário peça para estender o padrão de `auth` para outra feature.

### Fronteiras entre bounded contexts

Cada fronteira de contexto (`simulatedportfolio` vs `investment`,
`health` vs `wallet`/`academy`) tem seu próprio teste ArchUnit em
`src/test/java/com/jf/PetApp/architecture/` — um arquivo por fronteira
(`SimulatedPortfolioBoundaryTest`, `HealthBoundaryTest`,
`HealthTransactionBoundaryTest`), não um arquivo monolítico. Ao criar ou
tocar uma fronteira nova:

1. Escreva (ou estenda) o teste ArchUnit correspondente antes de considerar
   a fronteira pronta.
2. Se precisar de uma exceção deliberada (ex.: as duas simulações
   dependerem de um port de cotação de mercado somente-leitura), documente-a
   como `SimulatedPortfolioBoundaryTest` documenta
   `MARKET_DATA_PORT_EXCEPTION`: nomeada, estreita, com javadoc explicando o
   porquê e quando revisitar — nunca um `.and(not(...))` sem comentário.

## Nomenclatura — já é o padrão em todo o código, não uma sugestão

| Papel | Convenção | Exemplo real |
|---|---|---|
| Caso de uso (contrato) | `XyzUseCase` em `application/<feature>/usecase/` | `GetMentorReplyUseCase` |
| Caso de uso (implementação) | `XyzUseCaseImpl` no mesmo pacote | `GetMentorReplyUseCaseImpl` |
| Porta (contrato) | `XyzPort` em `application/<feature>/port/` | `MentorConversationRepositoryPort` |
| Adaptador (implementação) | `XyzAdapter` em `infrastructure/repository/<feature>/`, ou `XyzClient` para integração externa | `MentorConversationRepositoryAdapter`, `BrapiInvestmentApiClient` |
| DTO | `XyzDTO` ou `XyzRequest`/`XyzResponse` em `application/<feature>/dto/` | `MentorChatRequest` |

Nunca exponha a entidade JPA (`infrastructure/entity/`) diretamente num
controller — sempre através de um DTO da camada `application`.

## Lombok

Usado deliberadamente em **`core/domain` e `infrastructure`**, não só em
uma camada — decisão explícita do usuário priorizando consistência entre
camadas sobre um diff menor. Não está em uso em `application/` hoje; não
introduza lá sem alinhar antes, já que seria uma mudança de convenção, não
uma aplicação de uma já existente.

## TDD e o gate de mutação — não é só cobertura de linha

- `./mvnw test` roda a suíte normal (JUnit 5 + Mockito, `@Mock`/
  `@InjectMocks`). Toda classe de teste é `XyzUseCaseImplTest` (ou
  equivalente), sufixo sempre `Test`, nunca `Tests`, no pacote espelhado
  sob `src/test/java`.
- `./mvnw org.pitest:pitest-maven:mutationCoverage` roda mutation testing,
  escopado a `application.*` e `core.domain.*` (infraestrutura/DTO/config
  são excluídos de propósito: são "data-shaped", não lógica de decisão —
  mutá-los produz ruído de mutante equivalente, não sinal). Gate atual:
  `mutationThreshold=70`, `coverageThreshold=80` — a baseline medida na
  introdução do plugin foi 78%/88%; os limiares ficam abaixo de propósito
  para o gate ser real (não trivialmente satisfeito), não para ser
  confortável. Isso é bloqueante em CI (`backend-ci.yml`).
- Ao adicionar lógica de decisão nova em `application` ou `core/domain`:
  escreva o teste que falha primeiro sempre que a lógica não for trivial;
  depois de verde, rode a suíte completa (não assuma "compilou" = correto —
  `docs/BACKEND_MODULE_PLAN.md` documenta casos reais em que só um boot
  real pegou a regressão); se a mudança for não trivial, rode o PIT
  localmente antes de declarar a tarefa concluída.

## Migrações Flyway

Nem toda migração é portável entre os dois profiles reais deste repositório
— `dev` roda contra H2, `prod` contra Postgres, e já houve um caso real de
sintaxe Postgres-only (`ALTER TABLE ... SET SCHEMA`) que quebrou o H2
silenciosamente até um boot real expor o erro. Para qualquer migração nova:
suba o `dev` profile com `spring-boot:run` e observe o log real do
Flyway/Hibernate — não confie no exit code sozinho.
