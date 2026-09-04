# Petrimonium Backend

O repositório central do ecossistema Petrimonium: um único Spring Boot /
PostgreSQL servindo os três apps Flutter — **Health** (fluxo de caixa real),
**Wallet** (patrimônio real) e **Academy** (educação e dinheiro fictício).

O backend não é só a camada de dados: é onde a fronteira entre os produtos é
**executada**. A separação entre eles vive na claim `app_context` do JWT e no
`SecurityConfig`, não na navegação dos apps.

## Por onde começar

| Documento | O que responde |
|---|---|
| [`docs/INTEGRATION.md`](docs/INTEGRATION.md) | **O contrato de integração do ecossistema** — por que são três produtos, o que é compartilhado (identidade, Pet, XP, Mentor), o que nunca atravessa a fronteira, e as lacunas conhecidas. É o documento canônico; os três apps apontam para ele. |
| [`docs/ARCHITECTURE/`](docs/ARCHITECTURE/) | O Atlas Técnico — a visão geral da máquina e as fatias ponta a ponta (tela → tabela) |
| [`docs/BACKEND_MODULE_PLAN.md`](docs/BACKEND_MODULE_PLAN.md) | O plano de módulos/schemas por contexto e o que já foi executado |
| [`docs/ECOSYSTEM.md`](docs/ECOSYSTEM.md) | Histórico: como este backend chegou ao estado atual |
