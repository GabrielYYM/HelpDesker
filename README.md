# HelpDesker

Sistema completo de Help Desk desenvolvido com **Spring Boot**, **React** e **MongoDB Atlas**.

---

## Escopo do Sistema

### Autenticação e Cadastro
- **Login, logout e cadastro** de usuários.
- **Senhas criptografadas** com **Argon2**.
- **Controle de acesso baseado em papéis (RBAC)**: `USUARIO`, `GERENTE` e `ADMINISTRADOR`.

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
- Gestão e cadastro de usuários e técnicos (informações cadastrais sem exigência de senha inicial).
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
| **0.3.0** | `v0.3.0` | **Páginas de Erro & Proteção de Rotas**: Autorização por perfil (rotas e métodos) e páginas customizadas para erro 403, 404 e 500 | Planejado |
| **0.4.0** | `v0.4.0` | **Painel do Gerente (Gestão & Fila)**: Cadastro/edição de usuários e técnicos sem senha e visualização da fila geral de atendimento | Planejado |
| **0.5.0** | `v0.5.0` | **Controle de Chamados (Core)**: Abertura, geração de protocolo próprio, edição, cancelamento e máquina de estados (`ABERTO`, `EM_ANDAMENTO`, `RESOLVIDO`, `CANCELADO`) | Planejado |
| **0.6.0** | `v0.6.0` | **Interações nos Chamados**: Comentários, histórico de auditoria e fluxo de resolução com técnico responsável | Planejado |
| **0.7.0** | `v0.7.0` | **Dashboard do Gerente & Métricas**: Indicadores gerais, gráficos do último mês, tempo médio de atendimento e carga por técnico | Planejado |
| **0.8.0** | `v0.8.0` | **Testes Automatizados**: Suíte de testes unitários e de integração com JUnit 5 e Mockito | Planejado |
| **1.0.0-beta.1** | `v1.0.0-beta.1` | **Homologação & Dockerização**: Empacotamento Maven, `Dockerfile`, `docker-compose` local e carga de dados de demonstração | Planejado |
| **1.0.0** | `v1.0.0` | **Release Oficial de Produção**: Deploy final no Render (Docker) conectado ao MongoDB Atlas com frontend integrado | Planejado |
| **1.0.1** | `v1.0.1` | **Hotfix**: Correções emergenciais de variáveis de ambiente, CORS de produção ou build Docker | Planejado |
| **1.0.2** | `v1.0.2` | **Polimento**: Ajustes visuais finos, dados demonstrativos e documentação para apresentação | Planejado |

---
