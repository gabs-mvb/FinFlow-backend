# Onboarding Android e isolamento financeiro

O fluxo nativo foi integrado ao backend existente. A aplicação usa os mesmos casos de uso de contas, perfil, obrigações, transações, planejamento e autenticação. Não há backend paralelo nem dados de demonstração.

## Fluxo e persistência

Depois de autenticar, o Android consulta `GET /api/v1/onboarding`. A navegação principal só aparece quando a resposta informa `status=COMPLETED`, `currentStep=COMPLETED`, `completed=true` e `readyForDashboard=true`.

As etapas são boas-vindas, objetivo, renda, instituições, contas, cartões, perfil financeiro, automação e resumo. Cada avanço salva a etapa no servidor. Voltar mantém os campos preenchidos e permite revisar etapas anteriores; o servidor conserva a etapa mais avançada já salva. Concluir exige confirmação no resumo e validação de todos os dados obrigatórios pelo backend. A API antiga de conclusão não permite mais concluir apenas enviando um perfil.

`UserOnboarding` tem proprietário, estado, etapa, início, conclusão e atualização. Objetivos, fontes de renda, instituições, cartões e perfil inicial são registros tipados dentro desse agregado, persistidos como JSON decimal sem perda de precisão. A chave primária do agregado é o usuário e tem FK para `users`. A modelagem das listas permite evolução para múltiplas fontes e objetivos; o MVP valida um objetivo e uma fonte principal.

Contas são persistidas no módulo financeiro existente ao salvar a etapa. Seus identificadores locais estáveis tornam as repetições idempotentes. IDs de conta e obrigação recebidos do cliente são confrontados com o agregado pertencente ao usuário; não transferem propriedade. As despesas fixas se tornam obrigações recorrentes reais. Revisões atualizam as mesmas obrigações e cancelam as que foram removidas, sem duplicá-las. Contas já persistidas são mantidas durante o onboarding.

Cartões podem ser gerenciados depois em **Mais → Cartões**. O servidor impede remover um cartão com limite utilizado positivo. Instituições devem estar selecionadas no onboarding ou associadas a uma conta financeira do próprio usuário.

## Isolamento e problemas corrigidos

- Todas as rotas financeiras continuam resolvendo o proprietário pela autenticação JWT e consultando por usuário. GET individual, PUT, PATCH e DELETE de contas de outro usuário retornam 404.
- Registro cria `UserOnboarding.NOT_STARTED` na mesma transação do usuário. Não cria contas, metas ou transações fictícias.
- O fluxo anterior concluía ao salvar a renda. A conclusão passou a depender de todas as etapas, inclusive resumo e escolha explícita de automação.
- O Android anterior enviava eventos de confiança intermediária sem revisão. Agora a fila separa revisão, pendência, sincronização, falha, ignorados e sincronizados.
- Respostas e lotes em andamento não podem mudar de proprietário depois de uma troca de sessão. O cabeçalho interno de expectativa de usuário é verificado e removido pelo interceptor antes do HTTP; o backend continua usando exclusivamente o JWT.
- Navegação, ViewModels e dados locais são separados por sessão/usuário. A fila usa `(ownerId, fingerprint)`, rascunhos usam `ownerId`, consentimento é específico do usuário e cache usa `(ownerId, resource)`.
- As restrições globais antigas de contas, ativos, alocação e consentimentos causavam conflitos entre usuários. A migration V9 inclui o proprietário nessas identidades.
- A restrição original de recebimento até o dia 28 foi alinhada com a validação atual de dias 1 a 31.
- Registros antigos sem proprietário continuam sem proprietário. As migrations não os atribuem, copiam ou apresentam a novos usuários.

Categorias padrão continuam sendo o catálogo global existente (`TransactionCategory`). Não foi criado um cadastro global de categorias privadas.

## Notificações, confirmação e saldo

A allowlist inicial reutiliza os parsers de Nubank, Itaú, Inter, C6 e BTG. Escolher outro banco no onboarding não declara que ele já possui parser. Apps fora dessa lista são ignorados localmente. Notificações agendadas, canceladas, recusadas ou pendentes não são tratadas como movimentações executadas.

A permissão é solicitada na tela oficial do Android. Ela vale para o aparelho; por isso existe também um consentimento explícito por usuário FinFlow. Novas contas começam com captura desativada, mesmo que outro usuário já tenha concedido acesso ao Android. A captura fixa o proprietário que estava autorizado ao receber a notificação e descarta o evento se a sessão mudou antes de enfileirá-lo.

O texto completo não é enviado ao servidor. O parser produz tipo, valor `BigDecimal`, descrição semântica e eventual estabelecimento. Pix com padrão explícito e um único valor pode ter confiança alta; padrões intermediários e compras no crédito ficam para revisão. Em **Mais → Sincronização Inteligente**, o usuário pode editar valor/tipo, escolher sua conta e, quando necessário, cartão, confirmar ou ignorar. Quando existe ambiguidade entre contas do mesmo banco, não é escolhida silenciosamente a primeira conta.

O backend também exige confiança alta ou confirmação explícita. Eventos de crédito precisam indicar um cartão do próprio usuário e da instituição da conta. Compras no crédito aumentam o limite utilizado e entram no histórico sem debitar o caixa. Pagamento de fatura reduz o caixa e o limite utilizado; sua categoria é transferência, para não contar a compra novamente como despesa. Estorno ligado ao cartão devolve o limite sem inflar o saldo bancário.

Room preserva eventos não sincronizados. WorkManager tenta enviá-los com conexão, retentativa e chave estável de lote. O servidor serializa importações por usuário, verifica fingerprints no histórico e protege replays por chave de idempotência. Uma repetição não altera saldo nem limite novamente.

O Dashboard identifica o **saldo estimado**: saldo inicial informado + entradas reconhecidas − saídas reconhecidas. Patrimônio, saldo livre, limite diário, reserva, meta mensal, cartões, contas, compromissos, objetivos e resumo mensal usam exclusivamente dados do usuário. Reserva informada protege uma parte dos saldos registrados, sem criar um saldo fictício adicional. A meta de poupança mensal é protegida no planejamento inicial.

`FinancialDataSource` e o modelo de origens separam captura por notificações de eventos manuais, importações e uma futura integração Open Finance. Não foi implementada conexão bancária real.

## Offline

As consultas GET podem usar respostas reais da API previamente recebidas, criptografadas com a chave do Android Keystore. Esse cache é vinculado ao proprietário, recurso e hash do JWT que recebeu a resposta, com validade máxima de 24 horas e expiração do JWT. Outra conta ou novo token não reutiliza esse cache. Erros HTTP de autenticação não são convertidos em sucesso offline.

Não existe uma flag local independente que conclua o onboarding: a navegação usa o estado completo devolvido pelo servidor, inclusive quando a resposta consultada é a última resposta autenticada salva. Sem uma resposta anterior válida, o primeiro login e a liberação do Dashboard exigem internet. Conclusão, avanço e outros comandos não são simulados offline. Campos permanecem no rascunho local para retentar; eventos permanecem na fila para WorkManager. A interface identifica a consulta offline de dados salvos.

## Endpoints

### Autenticação do app e diagnóstico de 401

O cliente Kotlin usa `POST /api/auth/register` e `POST /api/auth/login`, ambos públicos. Essas duas chamadas não enviam um JWT anterior. O filtro do backend também ignora o bearer anterior nesses POSTs. O JWT retornado pelo login é salvo com Android Keystore e enviado como `Authorization: Bearer <token>` às rotas privadas, incluindo `/api/auth/me` e `/api/v1/**`. Não é necessário cadastrar o pacote Android no backend nem distribuir uma API key no APK.

Um 401 ao entrar indica rejeição da autenticação de e-mail/senha. A tela apresenta uma mensagem para conferir as credenciais; falhas de conexão recebem outra mensagem. Um 401 depois de entrar invalida a sessão local e exige novo login. Não registre senhas ou tokens ao investigar. Confirme que cadastro, login e chamadas privadas apontam para o mesmo ambiente e banco. Trocar `JWT_SECRET` invalida tokens já emitidos; as instâncias da mesma API devem usar a mesma chave.

CORS é aplicado às requisições com `Origin`, como as do navegador. O cliente Android nativo não envia esse cabeçalho e não depende de adicionar a URL/pacote do app a `ALLOWED_ORIGINS`. Os testes de segurança percorrem cadastro, login e leitura do onboarding com JWT, sem Origin e sem API key, além da rejeição de senha incorreta e bearer inválido nas rotas privadas.

| Operação | Contrato |
|---|---|
| `GET /api/v1/onboarding` | Status, etapa, dados e datas do próprio usuário |
| `POST /api/v1/onboarding/start` | Salva o início e avança às escolhas financeiras |
| `PUT /api/v1/onboarding/goal` | `{ "goals": ["ORGANIZE"] }` |
| `PUT /api/v1/onboarding/income` | `{ "incomes": [{ "name": "Principal", "amount": 5000, "schedule": "DAY_OF_MONTH", "payDay": 5 }] }` |
| `PUT /api/v1/onboarding/institutions` | `{ "institutions": ["Nubank", "Itaú"] }` |
| `PUT /api/v1/onboarding/accounts` | Lista com `localId`, instituição, nome, tipo, saldo decimal e referência retornada pelo servidor |
| `PUT /api/v1/onboarding/cards` | Lista com `localId`, instituição, apelido, limite, limite utilizado e dias |
| `PUT /api/v1/onboarding/profile` | Poupança mensal, reserva e despesas recorrentes |
| `PUT /api/v1/onboarding/automation` | `{ "enabled": false }`, escolha explícita e opcional de captura |
| `POST /api/v1/onboarding/complete` | Sem necessidade de body; valida e conclui uma única vez |
| `GET/PUT /api/v1/cards` | Consulta/atualização dos cartões próprios após onboarding |
| `GET/DELETE /api/v1/accounts/{id}` | Novas operações com verificação de propriedade |
| `POST /api/v1/financial-events/batch` | Contrato existente ampliado com `confidence`, `confirmed` e `cardLocalId` |

## Migrations e decisões

- **V8:** agregado de onboarding, preenchimento de estado para usuários existentes, preservação de jornadas legadas concluídas com perfil e conta próprios, e reinício seguro das flags incompletas.
- **V9:** unicidade financeira por proprietário, índices de consulta e dias de recebimento de 1 a 31.
- **Room 1 → 2:** rascunhos, cache de respostas, confirmação e vínculo de conta/cartão dos eventos; eventos antigos duvidosos são transferidos para revisão. Não usa migração destrutiva.
- Backend mantém o núcleo livre de Spring/JPA e transações na composição. Android usa Compose Material 3, MVVM, Hilt, Retrofit/OkHttp, Room/DataStore, coroutines/Flow e WorkManager já existentes.
- Renda variável usa horizonte estimado de um mês. Último dia útil é estimado por segunda a sexta, sem calendário de feriados; o plano apresenta essa limitação.
- Exclusão de contas com transações associadas é protegida pelo banco e retorna conflito, preservando o histórico.

## Validação e teste manual

Backend: `gradlew.bat test bootJar`. Android: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug` no projeto `FinFlow-app`.

Resultado da validação: 63 testes no backend (48 na aplicação e 15 no núcleo) e 52 no Android, sem falhas. JAR e APK debug foram gerados.

Os testes cobrem isolamento A/B, leitura/atualização/exclusão cruzadas, injeção de referência financeira, sequência obrigatória, persistência, replays, validação de conclusão, transações atômicas, migração de registros legados, dinheiro decimal, confirmação, cartão sem débito duplo, rascunhos e política de cache por proprietário/token/expiração. Permanecem os testes de navegação, autenticação e módulos financeiros existentes.

1. Publique este backend e aplique V8/V9 antes de usar o APK novo contra a API hospedada. O APK mantém a URL HTTPS configurável por `FINFLOW_BASE_URL`.
2. Crie A, percorra o onboarding com renda R$ 5.000 e uma conta Nubank de R$ 2.000. Cadastre opcionalmente cartão/despesa, escolha automação e confirme o resumo. O Dashboard deve mostrar seus dados e saldo estimado.
3. Saia em **Mais → Sair da conta**. Cadastre B. B deve começar em boas-vindas, sem contas, metas, renda, cartões ou transações de A. Também deve autorizar separadamente a captura de notificações.
4. Interrompa B em uma etapa, feche/reabra e entre novamente com conexão. O servidor deve devolver a última etapa salva. Voltar deve preservar os campos.
5. Teste requests com JWT de B e ID financeiro de A: GET, PUT, PATCH e DELETE de conta retornam 404. Enviar `userId` no body não transfere propriedade.
6. Ative o acesso oficial a notificações e o consentimento desta conta. Verifique que uma notificação não financeira não aparece na fila. Compra no crédito deve aparecer para confirmação; escolha a conta e seu cartão antes de confirmar.
7. Depois de carregar dados online, desligue a internet com JWT ainda válido. Consultas já salvas devem aparecer identificadas como offline; eventos novos devem ficar no Room. Restabeleça a rede e confirme que cada evento altera os dados uma única vez.
8. Troque de conta com eventos pendentes ou com uma requisição em andamento. B não deve herdar fila, cache, rascunho, consentimento ou resposta de A.

Compilação e testes automatizados não substituem a verificação visual e de permissões em um aparelho Android. O backend e as migrations não foram publicados automaticamente.

## Inventário de arquivos

### Backend — criados

- `core/src/main/kotlin/com/finflow/onboarding/application/ProgressiveOnboardingService.kt`
- `core/src/main/kotlin/com/finflow/onboarding/application/port/inbound/ProgressiveOnboardingUseCases.kt`
- `core/src/main/kotlin/com/finflow/onboarding/application/port/outbound/UserOnboardingRepository.kt`
- `core/src/main/kotlin/com/finflow/onboarding/domain/UserOnboarding.kt`
- `core/src/test/kotlin/com/finflow/planning/IncomeScheduleTest.kt`
- `docs/ANDROID_ONBOARDING.md`
- `src/main/kotlin/com/finflow/onboarding/adapter/inbound/http/CreditCardController.kt`
- `src/main/kotlin/com/finflow/onboarding/adapter/outbound/persistence/UserOnboardingEntity.kt`
- `src/main/resources/db/migration/V8__progressive_user_onboarding.sql`
- `src/main/resources/db/migration/V9__scope_financial_uniqueness_and_payday.sql`
- `src/test/kotlin/com/finflow/OnboardingMigrationTest.kt`
- `src/test/kotlin/com/finflow/OnboardingTestJourney.kt`
- `src/test/kotlin/com/finflow/ProgressiveOnboardingTest.kt`

### Backend — alterados/removidos

- `docs/AUTHENTICATION_JWT.md`
- `src/main/kotlin/com/finflow/authentication/adapter/inbound/security/JwtAuthenticationFilter.kt`
- `src/test/kotlin/com/finflow/shared/security/JwtSecurityTest.kt`

- `README.md`
- `core/src/main/kotlin/com/finflow/account/application/FinancialAccountService.kt`
- `core/src/main/kotlin/com/finflow/account/application/port/inbound/FinancialAccountUseCases.kt`
- `core/src/main/kotlin/com/finflow/account/application/port/outbound/FinancialAccountRepository.kt`
- `core/src/main/kotlin/com/finflow/account/domain/AccountType.kt`
- `core/src/main/kotlin/com/finflow/financialevent/application/FinancialEventService.kt`
- `core/src/main/kotlin/com/finflow/financialevent/application/model/FinancialEventItem.kt`
- `core/src/main/kotlin/com/finflow/onboarding/application/OnboardingService.kt`
- `core/src/main/kotlin/com/finflow/onboarding/application/model/OnboardingStatus.kt`
- `core/src/main/kotlin/com/finflow/planning/application/FinancialPlanService.kt`
- `core/src/main/kotlin/com/finflow/planning/domain/FinancialPlanner.kt`
- `core/src/main/kotlin/com/finflow/planning/domain/IncomeDate.kt`
- `core/src/main/kotlin/com/finflow/planning/domain/PlannerInput.kt`
- `core/src/main/kotlin/com/finflow/transaction/application/FinancialTransactionService.kt`
- `core/src/main/kotlin/com/finflow/transaction/application/model/ImportTransactionsRequest.kt`
- `core/src/test/kotlin/com/finflow/account/FinancialAccountUseCasesTest.kt`
- `src/main/kotlin/com/finflow/account/adapter/inbound/http/FinancialAccountController.kt`
- `src/main/kotlin/com/finflow/account/adapter/outbound/persistence/FinancialAccountEntity.kt`
- `src/main/kotlin/com/finflow/account/adapter/outbound/persistence/JpaFinancialAccountRepository.kt`
- `src/main/kotlin/com/finflow/account/adapter/outbound/persistence/SpringDataFinancialAccountRepository.kt`
- `src/main/kotlin/com/finflow/composition/SecurityConfig.kt`
- `src/main/kotlin/com/finflow/composition/UseCaseConfiguration.kt`
- `src/main/kotlin/com/finflow/financialevent/adapter/inbound/http/ImportFinancialEventsRequestBody.kt`
- `src/main/kotlin/com/finflow/onboarding/adapter/inbound/http/OnboardingController.kt`
- `src/main/kotlin/com/finflow/openfinance/adapter/outbound/persistence/OpenFinanceConsentEntity.kt`
- `src/main/kotlin/com/finflow/portfolio/adapter/outbound/persistence/PortfolioPositionEntity.kt`
- `src/test/kotlin/com/finflow/HexagonalJourneyTest.kt`
- `src/test/kotlin/com/finflow/TransactionBoundaryTest.kt`
- `src/test/kotlin/com/finflow/financialevent/FinancialEventControllerTest.kt`

### Android — criados

- `app/src/test/java/com/finflow/mobile/feature/auth/AuthViewModelTest.kt`

- `docs/ONBOARDING.md`
- `app/src/main/java/com/finflow/mobile/core/database/ApiCache.kt`
- `app/src/main/java/com/finflow/mobile/core/database/OnboardingDraft.kt`
- `app/src/main/java/com/finflow/mobile/core/network/DecimalJsonAdapter.kt`
- `app/src/main/java/com/finflow/mobile/core/network/OfflineApiCacheInterceptor.kt`
- `app/src/main/java/com/finflow/mobile/core/security/NotificationConsentStore.kt`
- `app/src/main/java/com/finflow/mobile/data/remote/OnboardingApi.kt`
- `app/src/main/java/com/finflow/mobile/data/repository/OnboardingRepositoryImpl.kt`
- `app/src/main/java/com/finflow/mobile/domain/model/Onboarding.kt`
- `app/src/main/java/com/finflow/mobile/domain/repository/FinancialDataSource.kt`
- `app/src/main/java/com/finflow/mobile/domain/repository/OnboardingRepository.kt`
- `app/src/main/java/com/finflow/mobile/feature/finance/CardsScreen.kt`
- `app/src/main/java/com/finflow/mobile/service/banking_notifications/NotificationFinancialDataSource.kt`
- `app/src/test/java/com/finflow/mobile/core/common/TokenExpiryTest.kt`
- `app/src/test/java/com/finflow/mobile/data/mapper/FinancialEventReviewTest.kt`
- `app/src/test/java/com/finflow/mobile/data/remote/ProgressiveOnboardingContractTest.kt`
- `app/schemas/com.finflow.mobile.core.database.FinFlowDatabase/2.json`

### Android — alterados

- `app/src/main/java/com/finflow/mobile/feature/auth/AuthViewModel.kt`
- `app/src/test/java/com/finflow/mobile/support/FakeFinFlowRepository.kt`

- `app/build.gradle.kts`
- `README.md`
- `app/src/main/java/com/finflow/mobile/core/common/EventFingerprint.kt`
- `app/src/main/java/com/finflow/mobile/core/database/DatabaseModule.kt`
- `app/src/main/java/com/finflow/mobile/core/database/FinFlowDatabase.kt`
- `app/src/main/java/com/finflow/mobile/core/database/PendingFinancialEventDao.kt`
- `app/src/main/java/com/finflow/mobile/core/database/PendingFinancialEventEntity.kt`
- `app/src/main/java/com/finflow/mobile/core/network/AuthInterceptor.kt`
- `app/src/main/java/com/finflow/mobile/core/network/NetworkModule.kt`
- `app/src/main/java/com/finflow/mobile/core/security/SessionStore.kt`
- `app/src/main/java/com/finflow/mobile/data/mapper/FinancialEventMapper.kt`
- `app/src/main/java/com/finflow/mobile/data/remote/Dtos.kt`
- `app/src/main/java/com/finflow/mobile/data/remote/FinFlowApi.kt`
- `app/src/main/java/com/finflow/mobile/data/repository/FinancialEventRepositoryImpl.kt`
- `app/src/main/java/com/finflow/mobile/data/repository/FinFlowRepositoryImpl.kt`
- `app/src/main/java/com/finflow/mobile/data/repository/RepositoryModule.kt`
- `app/src/main/java/com/finflow/mobile/domain/model/FinancialEvent.kt`
- `app/src/main/java/com/finflow/mobile/domain/repository/FinancialEventRepository.kt`
- `app/src/main/java/com/finflow/mobile/domain/repository/FinFlowRepository.kt`
- `app/src/main/java/com/finflow/mobile/domain/usecase/CaptureFinancialEvent.kt`
- `app/src/main/java/com/finflow/mobile/feature/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/finflow/mobile/feature/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/finflow/mobile/feature/finance/MoreScreen.kt`
- `app/src/main/java/com/finflow/mobile/feature/notification_sync/NotificationSyncScreen.kt`
- `app/src/main/java/com/finflow/mobile/feature/notification_sync/NotificationSyncViewModel.kt`
- `app/src/main/java/com/finflow/mobile/feature/onboarding/OnboardingScreen.kt`
- `app/src/main/java/com/finflow/mobile/feature/onboarding/OnboardingViewModel.kt`
- `app/src/main/java/com/finflow/mobile/navigation/FinFlowRoot.kt`
- `app/src/main/java/com/finflow/mobile/navigation/RootViewModel.kt`
- `app/src/main/java/com/finflow/mobile/service/banking_notifications/AbstractBankNotificationParser.kt`
- `app/src/main/java/com/finflow/mobile/service/banking_notifications/BankingNotificationListenerService.kt`
- `app/src/main/java/com/finflow/mobile/service/banking_notifications/FinancialEventSyncWorker.kt`
- `app/src/test/java/com/finflow/mobile/feature/onboarding/OnboardingViewModelTest.kt`
- `app/src/test/java/com/finflow/mobile/service/banking_notifications/NubankNotificationParserTest.kt`
