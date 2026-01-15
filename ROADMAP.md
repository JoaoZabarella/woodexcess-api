# 🗺️ WoodExcess API - Product Roadmap

> **Última atualização:** 15/01/2026  
> **Versão atual:** 1.0.0 (MVP com Favoritos)  
> **Próxima milestone:** 1.1.0 (Transações + Avaliações)

---

## 📊 Status Geral do Projeto

| Categoria | Implementado | Em Progresso | Planejado |
|-----------|--------------|--------------|-----------|
| **Core Features** | 9/11 | 0 | 2 |
| **Monetização** | 0/3 | 0 | 3 |
| **UX/Engajamento** | 2/5 | 0 | 3 |
| **Admin/Moderação** | 1/6 | 0 | 5 |
| **Infraestrutura** | 3/8 | 0 | 5 |

**Progresso Total:** 62% das funcionalidades críticas implementadas

---

## ✅ Funcionalidades Implementadas

### **Core Business Logic**
- [x] Autenticação JWT (access + refresh tokens)
- [x] Gestão de usuários (CRUD, soft delete)
- [x] Gestão de endereços (integração ViaCEP)
- [x] Material Listings (CRUD completo)
- [x] Upload de imagens (AWS S3 + reordenação)
- [x] Sistema de chat em tempo real (WebSocket/STOMP)
- [x] Sistema de mensagens (histórico, não lidas)
- [x] Sistema de favoritos/watchlist
- [x] Sistema de ofertas/negociação

### **Segurança & Performance**
- [x] Rate limiting (login, mensagens, WebSocket)
- [x] Soft delete para auditoria
- [x] Otimização N+1 queries (JOIN FETCH, batch queries)
- [x] Paginação em todos os endpoints críticos

### **Qualidade & DevOps**
- [x] Testes (85%+ cobertura)
- [x] CI/CD (GitHub Actions)
- [x] Documentação Swagger/OpenAPI
- [x] Docker support

---

## 🔴 PRIORIDADE CRÍTICA - Bloqueadores de Produção

### **1. Sistema de Compras/Transações** 🛒
**Status:** ❌ NÃO INICIADO  
**Milestone:** 1.1.0  
**Estimativa:** 2-3 semanas  
**Dependências:** Sistema de Pagamentos

**Escopo:**
- [ ] Entity `Purchase` (com estados: PENDING, PAID, SHIPPED, COMPLETED, CANCELLED)
- [ ] `PurchaseController` (criar, listar, cancelar)
- [ ] `PurchaseService` (lógica de negócio, validações)
- [ ] Integração com `Offer` (converter oferta aceita em compra)
- [ ] Histórico de transações
- [ ] 20+ testes (unit + integration)

**Branch:** `feature/purchase-transaction-system`  
**Commit:** `feat: implement purchase and transaction management system`

---

### **2. Recuperação de Senha** 🔑
**Status:** ❌ NÃO INICIADO  
**Milestone:** 1.1.0  
**Estimativa:** 3-5 dias  
**Dependências:** Sistema de Email

**Escopo:**
- [ ] Entity `PasswordResetToken` (token único, expiração 1h)
- [ ] `POST /api/auth/forgot-password` (envia email)
- [ ] `POST /api/auth/reset-password` (valida token + define nova senha)
- [ ] Email template com link de reset
- [ ] Expiração automática de tokens
- [ ] 10+ testes

**Branch:** `feature/password-recovery`  
**Commit:** `feat: implement forgot password and reset functionality`

---

### **3. Verificação de Email** ✉️
**Status:** ❌ NÃO INICIADO  
**Milestone:** 1.1.0  
**Estimativa:** 3-5 dias  
**Dependências:** Sistema de Email

**Escopo:**
- [ ] Campo `emailVerified` na entidade `User`
- [ ] Entity `EmailVerificationToken`
- [ ] `POST /api/auth/verify-email/{token}`
- [ ] `POST /api/auth/resend-verification`
- [ ] Email template de boas-vindas
- [ ] Restrições para usuários não verificados (opcional)
- [ ] 8+ testes

**Branch:** `feature/email-verification`  
**Commit:** `feat: add email verification system for new users`

---

### **4. Sistema de Avaliações/Reviews** ⭐
**Status:** ❌ NÃO INICIADO  
**Milestone:** 1.2.0  
**Estimativa:** 1-2 semanas  
**Dependências:** Sistema de Compras

**Escopo:**
- [ ] Entity `Rating` (1-5 estrelas, comentário, compra associada)
- [ ] `RatingController` (criar, listar, responder)
- [ ] Avaliação de vendedores (reputação)
- [ ] Avaliação de materiais (qualidade)
- [ ] Média de avaliações (cache)
- [ ] Impedir múltiplas avaliações na mesma compra
- [ ] 15+ testes

**Branch:** `feature/rating-review-system`  
**Commit:** `feat: implement rating and review system for sellers and materials`

---

### **5. Notificações por Email** 📧
**Status:** ❌ NÃO INICIADO  
**Milestone:** 1.1.0  
**Estimativa:** 1 semana  
**Dependências:** Nenhuma

**Escopo:**
- [ ] Integração SendGrid/AWS SES
- [ ] `EmailService` (envio assíncrono com `@Async`)
- [ ] Templates de email (boas-vindas, nova mensagem, oferta recebida, etc.)
- [ ] Preferências de notificação do usuário
- [ ] Retry logic para falhas
- [ ] 12+ testes

**Branch:** `feature/email-notification-service`  
**Commit:** `feat: implement async email notification system with SendGrid`

---

## 🟡 PRIORIDADE ALTA - Essenciais para Escala

### **6. Dashboard/Analytics** 📊
**Status:** ❌ NÃO INICIADO  
**Milestone:** 1.2.0  
**Estimativa:** 1-2 semanas

**Escopo:**
- [ ] Dashboard do vendedor (vendas, visualizações, favoritos recebidos)
- [ ] Dashboard do comprador (compras, gastos)
- [ ] Endpoints de métricas (`/api/users/me/stats`)
- [ ] Gráficos de evolução temporal
- [ ] 10+ testes

**Branch:** `feature/user-dashboard-analytics`  
**Commit:** `feat: add user dashboard with sales and activity analytics`

---

### **7. Sistema de Admin/Moderação** 👮
**Status:** ⚠️ PARCIALMENTE IMPLEMENTADO (role existe, mas sem ferramentas)  
**Milestone:** 1.3.0  
**Estimativa:** 2 semanas

**Escopo:**
- [ ] Dashboard administrativo
- [ ] Moderação de listings (aprovar/rejeitar/destacar)
- [ ] Banimento de usuários (soft ban + motivo)
- [ ] Logs de atividades suspeitas
- [ ] Fila de denúncias
- [ ] 20+ testes

**Branch:** `feature/admin-moderation-panel`  
**Commit:** `feat: implement admin moderation panel and user management tools`

---

### **8. Sistema de Denúncias** 🚩
**Status:** ❌ NÃO INICIADO  
**Milestone:** 1.3.0  
**Estimativa:** 1 semana

**Escopo:**
- [ ] Entity `Report` (tipo: LISTING, USER, MESSAGE)
- [ ] `ReportController` (criar denúncia, listar para admin)
- [ ] Motivos padronizados (spam, fraude, conteúdo inapropriado)
- [ ] Workflow de moderação (pendente → resolvido)
- [ ] Notificações para admin
- [ ] 12+ testes

**Branch:** `feature/content-report-system`  
**Commit:** `feat: add reporting system for listings, users, and messages`

---

### **9. Geolocalização Avançada** 📍
**Status:** ⚠️ PARCIALMENTE IMPLEMENTADO (endereços existem, busca por proximidade não)  
**Milestone:** 1.4.0  
**Estimativa:** 1 semana

**Escopo:**
- [ ] Busca por raio (`/api/listings?lat=-23.5&lng=-46.6&radius=10`)
- [ ] Cálculo de distância Haversine
- [ ] Ordenação por proximidade
- [ ] Índice espacial no PostgreSQL (PostGIS)
- [ ] 8+ testes

**Branch:** `feature/geolocation-proximity-search`  
**Commit:** `feat: add proximity-based search with radius filtering`

---

### **10. Sistema de Frete** 🚚
**Status:** ❌ NÃO INICIADO  
**Milestone:** 1.4.0  
**Estimativa:** 2 semanas  
**Dependências:** Sistema de Compras

**Escopo:**
- [ ] Integração Correios API (cálculo de frete)
- [ ] Opções de entrega (retirar local, entrega, frete grátis)
- [ ] Rastreamento de pedidos (código tracking)
- [ ] 10+ testes

**Branch:** `feature/shipping-logistics-system`  
**Commit:** `feat: integrate shipping calculation and tracking with Correios API`

---

## 🟢 PRIORIDADE MÉDIA - Melhorias de UX

### **11. Busca Avançada/Elasticsearch** 🔍
**Status:** ❌ NÃO INICIADO  
**Milestone:** 2.0.0  
**Estimativa:** 2-3 semanas

**Escopo:**
- [ ] Integração Elasticsearch
- [ ] Full-text search (título, descrição)
- [ ] Autocomplete de busca
- [ ] Busca por relevância (scoring)
- [ ] Histórico de buscas do usuário

**Branch:** `feature/elasticsearch-integration`  
**Commit:** `feat: add Elasticsearch for advanced full-text search`

---

### **12. Sistema de Cupons/Promoções** 🎟️
**Status:** ❌ NÃO INICIADO  
**Milestone:** 2.1.0  
**Estimativa:** 1 semana

**Escopo:**
- [ ] Entity `Coupon` (código, desconto %, validade)
- [ ] Validação de cupom no checkout
- [ ] Uso único vs. múltiplos usos
- [ ] Cupons por categoria/vendedor

**Branch:** `feature/coupon-discount-system`  
**Commit:** `feat: implement promotional coupon and discount system`

---

## 🔵 PRIORIDADE BAIXA - Nice to Have

### **13. RabbitMQ/Event-Driven Architecture** 🐰
**Status:** ❌ NÃO INICIADO  
**Milestone:** 2.2.0  
**Estimativa:** 2 semanas

**Escopo:**
- [ ] Integração RabbitMQ
- [ ] Event publishers/listeners
- [ ] Processamento assíncrono de tarefas
- [ ] Fila de emails

---

### **14. Webhooks API** 🔗
**Status:** ❌ NÃO INICIADO  
**Milestone:** 2.3.0  
**Estimativa:** 1 semana

**Escopo:**
- [ ] Registro de webhooks por usuário
- [ ] Disparo de eventos (nova compra, oferta aceita)
- [ ] Retry logic
- [ ] Logs de webhooks

---

### **15. Múltiplos Idiomas (i18n)** 🌍
**Status:** ❌ NÃO INICIADO  
**Milestone:** 3.0.0  
**Estimativa:** 2 semanas

**Escopo:**
- [ ] Spring i18n support
- [ ] Traduções PT/EN/ES
- [ ] Preferência de idioma do usuário

---

## 📅 Timeline de Releases

### **Milestone 1.1.0 - "Transações & Segurança"** (ETA: Fevereiro 2026)
- Sistema de Compras/Transações
- Recuperação de Senha
- Verificação de Email
- Notificações por Email

### **Milestone 1.2.0 - "Confiança & Engajamento"** (ETA: Março 2026)
- Sistema de Avaliações/Reviews
- Dashboard/Analytics
- Integração de Pagamentos (Stripe)

### **Milestone 1.3.0 - "Moderação & Segurança"** (ETA: Abril 2026)
- Sistema de Admin/Moderação
- Sistema de Denúncias
- Logs de Auditoria

### **Milestone 1.4.0 - "Logística & UX"** (ETA: Maio 2026)
- Geolocalização Avançada
- Sistema de Frete
- Busca Avançada (Elasticsearch)

---

## 🎯 Métricas de Sucesso

| Métrica | Atual | Meta 1.1.0 | Meta 2.0.0 |
|---------|-------|------------|------------|
| **Cobertura de Testes** | 85% | 90% | 95% |
| **Funcionalidades Core** | 62% | 85% | 100% |
| **Tempo de Resposta API** | <200ms | <150ms | <100ms |
| **Uptime** | N/A | 99.5% | 99.9% |

---

## 🔄 Como Atualizar Este Roadmap

1. Após **merge de PR com nova feature**, atualizar seção "✅ Funcionalidades Implementadas"
2. Atualizar **data de "Última atualização"**
3. Commit: `docs: update roadmap after implementing [feature-name]`
4. Manter este arquivo **sempre no topo** das conversas do Space

---

## 💡 Contribuindo

Para sugerir novas funcionalidades ou prioridades, abra uma issue com label `roadmap-suggestion`.
