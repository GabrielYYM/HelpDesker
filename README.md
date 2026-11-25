# HelpDesker

Sistema completo de Help Desk desenvolvido com **Spring Boot**, **React** e **MongoDB Atlas**.

---

## Escopo do Sistema

### Autenticação e Cadastro
- **Login, logout e cadastro** de usuários.
- **Senhas criptografadas** com **Argon2**.
- **Controle de acesso baseado em papéis (RBAC)**: `ADM`, `USER` e `IT`.

### Controle de Chamados
- **Ciclo completo**: Abertura, edição, acompanhamento e cancelamento.
- **Interações**: Comentários e histórico detalhado de alterações e resolução.
- **Metadados**: Protocolo próprio gerado pelo sistema, prioridade, tipo, setor, cliente e técnico responsável.
- **Estados do chamado**: `ABERTO`, `EM_ANDAMENTO`, `RESOLVIDO` e `CANCELADO`.

### Dashboard do Gerente
- **Indicadores gerais** e cartões de métricas de chamados.
- **Gráficos** de distribuição e volume do último mês.
- Listagem rápida de chamados recentes e métricas de resolução.
- **Tempo Médio de Atendimento (TMA)** e análise de carga de trabalho por técnico.

### Painel do Gerente
- Gestão e cadastro de usuários e técnicos. O sistema gera uma senha temporária e exige a troca no primeiro login.
- Visualização e controle da fila geral de atendimento.

### Persistência e Sessões
- Banco de dados principal: **MongoDB Atlas**.
- **Sessões HTTP persistidas no MongoDB** com expiração configurada para **30 minutos**.

### Segurança
- Autorização refinada por perfil em rotas e métodos (`@PreAuthorize`).
- Proteção contra **CSRF**.
- Páginas de erro personalizadas para:
  - **403**: Acesso Negado
  - **404**: Página Não Encontrada
  - **500**: Erro Interno do Servidor

### Testes e Deploy
- Testes automatizados unitários e de integração com **JUnit 5** e **Mockito**.
- Empacotamento com **Maven**.
- Containerização e execução via **Docker** no **Render**.
- Persistência e alta disponibilidade via **MongoDB Atlas**.

---

## Roadmap de Versões (SemVer)

O versionamento segue rigorosamente as diretrizes do Semantic Versioning 2.0.0 (SemVer)

| Versão (SemVer) | Tag Git | Módulo / Escopo da Entrega | Status |
| :--- | :--- | :--- | :---: |
| **0.1.0** | `v0.1.0` | **Setup Base**: Configuração Spring Boot + React, conexão MongoDB Atlas| FINALIZADA |
| **0.2.0** | `v0.2.0` | **Segurança & Autenticação Core**: Criptografia Argon2, perfis, endpoints de Login, Logout e Cadastro com proteção CSRF | Planejado |
| **0.3.0** | `v0.3.0` | **Autorização & Erros da API**: Proteção de endpoints por perfil e tratamento de erros no backend | Planejado |
| **0.4.0** | `v0.4.0` | **Painel do Gerente (Gestão & Fila)**: Integração da sessão no React, proteção de rotas e páginas de erro 403, 404 e 500. Cadastro e edição de usuários e técnicos sem senha inicial e visualização da fila geral de atendimento | Planejado |
| **0.5.0** | `v0.5.0` | **Controle de Chamados**: Protocolo próprio, abertura, edição, cancelamento, atendimento com técnico responsável, comentários e histórico de auditoria | Implementado na branch de desenvolvimento |
| **0.6.0** | `v0.6.0` | **Dashboard do Gerente & Métricas**: Indicadores gerais, gráficos do último mês, tempo médio de atendimento e carga por técnico | Implementado na branch de desenvolvimento |
| **0.7.0** | `v0.7.0` | **Testes Automatizados**: Suíte de testes unitários e de integração com JUnit 5 e Mockito | Implementado na branch de desenvolvimento |
| **1.0.0-beta.1** | `v1.0.0-beta.1` | **Homologação & Dockerização**: Empacotamento Maven, `Dockerfile`, `docker-compose` local e carga de dados de demonstração | Planejado |
| **1.0.0** | `v1.0.0` | **Release Oficial de Produção**: Deploy final (Docker) conectado ao MongoDB Atlas com frontend integrado | Planejado |
| **1.0.2** | `v1.0.1` | **Polimento**: Ajustes visuais finos, dados demonstrativos e documentação completa | Planejado |

---

## Fluxo de chamados

- **Solicitante** acompanha os próprios chamados e pode editar ou cancelar enquanto o estado é `ABERTO`.
- **Equipe de atendimento** (`IT` e `ADM`) acessa a fila. Ao assumir um chamado aberto, o atendente se torna o responsável e o chamado passa para `EM_ANDAMENTO`.
- **Responsável** registra atualizações e pode marcar o chamado como `RESOLVIDO` ou `CANCELADO`. O perfil `ADM` também pode cancelar chamados abertos ou em andamento.
- **Auditoria** registra o autor, a data, o estado, os campos alterados e os comentários. Início, resolução e cancelamento exigem uma nota. O solicitante, o responsável e administradores podem comentar após o encerramento.

## Métricas do dashboard

- O volume diário usa os últimos 30 dias corridos, incluindo o dia atual, no fuso de São Paulo. Aberturas e resoluções são contabilizadas na data em que ocorreram.
- O tempo médio de atendimento (TMA) mede o intervalo entre assumir e resolver, para chamados resolvidos no período. Comentários posteriores não alteram esse tempo.
- A carga de cada técnico mostra os atendimentos em andamento no momento, as resoluções no período e o TMA individual. A distribuição por estado e o total consideram todos os chamados.

## Testes automatizados

Com Java 21, execute `bash ./mvnw --batch-mode test` na pasta `backend`. No Windows, use `.\mvnw.cmd -B test`.

Os testes HTTP executam o Spring Boot em uma porta aleatória e substituem a persistência de usuários e sessões por mocks. A suíte roda sem MongoDB ativo e sem credenciais do Atlas. O GitHub Actions executa os testes em pushes e pull requests que alterem o backend ou o workflow.

## Deploy no Render

O arquivo `render.yaml` prepara dois serviços no mesmo Blueprint:

- `helpdesker-api`: API Spring Boot em Docker, com health check em `/api/auth/csrf`.
- `helpdesker-web`: frontend Vite estático, com fallback para as rotas do React.

No Render, crie um novo **Blueprint** a partir deste repositório e selecione a branch que contém o `render.yaml`. O Blueprint cria os dois serviços no plano `free`; altere o plano no painel do Render se precisar evitar a suspensão por inatividade ou aumentar os recursos da API.

A única variável solicitada na criação é `MONGODB_URI`, que deve conter a URI de produção do MongoDB Atlas. O Render gera o segredo JWT, usa a porta fornecida pela plataforma e conecta automaticamente a URL pública da API ao frontend e a URL do frontend ao CORS da API. Não inclua segredos em arquivos `.env` enviados ao Git.

Para domínio próprio no frontend, inclua a origem HTTPS correspondente em `CORS_ALLOWED_ORIGINS`. A configuração padrão cobre as URLs `onrender.com` criadas pelo Blueprint. Os cookies de sessão e CSRF usam `Secure` e `SameSite=None` em produção para que a autenticação funcione quando frontend e API estiverem em origens distintas.
