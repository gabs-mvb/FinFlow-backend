# Logs de onboarding

Os endpoints `GET /api/v1/onboarding` e `POST /api/v1/onboarding/complete` registram o fluxo no console da aplicação. Cada chamada recebe um UUID gerado pelo servidor no header `X-Request-ID`; procure esse valor nos logs para acompanhar a chamada inteira.

Eventos em `INFO`:

- `onboarding.http.started`: requisição recebida, antes da autenticação e da validação HTTP.
- `onboarding.operation.started`: entrada no caso de uso (`status` ou `complete`).
- `onboarding.step`: consulta de status, solicitação/aquisição do bloqueio do usuário, início/fim da gravação do perfil e da marcação de conclusão.
- `ALREADY_COMPLETED`: nova chamada para um usuário que já concluiu; o perfil não é sobrescrito.
- `onboarding.operation.committed`: a transação retornou com sucesso, incluindo o commit. Os eventos de gravação anteriores são etapas e não garantem commit.
- `onboarding.http.finished`: status HTTP e duração total em milissegundos.

Respostas HTTP 4xx usam `WARN`; respostas 5xx, exceções e falhas de transação usam `ERROR`. Erros tratados pelo controller advice incluem o código da resposta, como `VALIDATION_ERROR` ou `MALFORMED_REQUEST_BODY`. Rejeições de segurança são identificadas pelo status HTTP. Se a transação falha, o evento é `onboarding.operation.failed`, sem evento `committed`.

As etapas incluem o ID interno do usuário e o indicador de conclusão. Os logs adicionados não incluem corpo, valores financeiros, e-mail, senha, token, query string ou mensagens de exceção que possam conter esses dados. O contexto de correlação é restaurado ao terminar cada requisição.

O `core` emite etapas pela porta `OnboardingEvents`; o adaptador SLF4J escreve os logs. Não é necessário habilitar `DEBUG`. Para configurar explicitamente o nível:

```yaml
logging:
  level:
    com.finflow.onboarding: INFO
```
