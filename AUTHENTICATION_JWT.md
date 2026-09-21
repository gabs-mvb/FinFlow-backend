# JWT Authentication - FinFlow Backend

## Implementação Realizada

Uma implementação completa de autenticação JWT foi adicionada ao projeto FinFlow-backend, baseada no projeto de referência `Login-JWT`.

### Estrutura Criada

```
src/main/kotlin/com/finflow/authentication/
├── domain/
│   ├── User.kt (JPA Entity)
│   ├── UserRepository.kt (Spring Data JPA)
│   └── dto/
│       ├── LoginRequestDto.kt
│       ├── LoginResponseDto.kt
│       ├── RegisterRequestDto.kt
│       └── UserResponseDto.kt
├── service/
│   ├── AuthenticationService.kt (Lógica de login e registro)
│   └── CustomUserDetailsService.kt (UserDetailsService)
├── controller/
│   └── AuthenticationController.kt (API endpoints)
└── security/
    ├── SecurityConfig.kt (Configuração Spring Security)
    └── jwt/
        ├── JwtTokenProvider.kt (Geração e validação de tokens)
        ├── JwtAuthenticationFilter.kt (Filtro para extrair JWT)
        └── JwtAuthenticationEntryPoint.kt (Tratamento de erros)
```

### Endpoints Disponíveis

- **POST /api/auth/register** - Registrar novo usuário
  ```json
  {
    "name": "João Silva",
    "email": "joao@example.com",
    "password": "senha123"
  }
  ```

- **POST /api/auth/login** - Realizar login
  ```json
  {
    "email": "joao@example.com",
    "password": "senha123"
  }
  ```
  
  Resposta:
  ```json
  {
    "id": 1,
    "name": "João Silva",
    "email": "joao@example.com",
    "token": "eyJhbGciOiJIUzUxMiJ9..."
  }
  ```

### Configuração

#### Variáveis de Ambiente (application.yml)

```yaml
jwt:
  secret: ${JWT_SECRET:dHJhY2tzYWZlZG9lc2VhcmxpZXJzaGVlcGZvb3RiYWxsb2JqZWN0cGxhaW5zaGVsdGVtZW4xZ3k5bW9weWM1ZzZnb3l6dmxwcXZ4YXpyZnF3aXVkeWF3eXN4dDZydGxpbmVxYnR5Ykl0ZWxxcnN3MA==}
  expiration: ${JWT_EXPIRATION:86400000}  # 24 horas em ms
```

### Uso do Token

Incluir o token em requisições autenticadas:
```
Authorization: Bearer <seu_token_aqui>
```

### Dependências Adicionadas

```gradle
implementation("io.jsonwebtoken:jjwt-api:0.11.5")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
```

### Fluxo de Autenticação

1. Usuário se registra → senha é criptografada com BCrypt
2. Usuário faz login → credenciais são validadas
3. Token JWT é gerado com expiração de 24 horas
4. Cliente inclui token em requisições futuras
5. Filtro JWT valida token em cada requisição
6. Acesso concedido se token válido

### Segurança

- Senhas criptografadas com BCrypt
- Sessões stateless (JWT)
- CORS configurado
- CSRF desabilitado (stateless)
- Endpoints públicos: `/api/auth/**`
- Endpoints privados: requerem token válido

### Migration do Banco

Arquivo: `src/main/resources/db/migration/V1__Create_users_table.sql`

Cria tabela `users` com:
- id (chave primária)
- name
- email (único)
- password
- created_at
- active (status do usuário)
