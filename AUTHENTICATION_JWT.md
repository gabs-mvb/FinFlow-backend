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
    ├── JwtSecurityConfigurer.kt (Beans de segurança)
    └── jwt/
        ├── JwtTokenProvider.kt (Geração e validação de tokens)
        ├── JwtAuthenticationFilter.kt (Filtro para extrair JWT)
        └── JwtAuthenticationEntryPoint.kt (Tratamento de erros)
```

### Endpoints Disponíveis

#### Registro de Usuário
```bash
POST /api/auth/register
Content-Type: application/json

{
  "name": "João Silva",
  "email": "joao@example.com",
  "password": "senha123"
}

# Resposta (201 Created)
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@example.com"
}
```

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "joao@example.com",
  "password": "senha123"
}

# Resposta (200 OK)
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@example.com",
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

### Usando o Token JWT

Incluir o token em requisições futuras:
```bash
GET /api/seu-endpoint
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### Configuração

#### Variáveis de Ambiente (application.yml)

```yaml
jwt:
  secret: ${JWT_SECRET:seu-secret-key-aqui}
  expiration: ${JWT_EXPIRATION:86400000}  # 24 horas em ms
```

#### Para Desenvolvimento

```bash
# Gerar uma secret segura (execute no terminal)
echo -n "sua-chave-segura" | sha256sum
```

### Segurança

- ✅ Senhas criptografadas com **BCrypt**
- ✅ Tokens JWT com expiração configurável (24h por padrão)
- ✅ Sessões **stateless**
- ✅ CORS configurado
- ✅ Endpoints públicos: `/api/auth/**`
- ✅ Endpoints privados: requerem token JWT válido

### Fluxo de Autenticação

1. **Registro**: Usuário se registra → senha é criptografada com BCrypt
2. **Login**: Credenciais são validadas → token JWT é gerado
3. **Requisições Autenticadas**: Cliente inclui token no header `Authorization: Bearer <token>`
4. **Validação**: Filtro JWT valida token em cada requisição
5. **Acesso**: Se token válido → acesso concedido

### Dependências Adicionadas

```gradle
implementation("io.jsonwebtoken:jjwt-api:0.11.5")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
```

### Banco de Dados

Arquivo: `src/main/resources/db/migration/V2__Create_users_table.sql`

Cria tabela `users` com:
- `id` - Chave primária
- `name` - Nome do usuário
- `email` - Email único
- `password` - Senha criptografada
- `created_at` - Data de criação
- `active` - Status do usuário (ativo/inativo)

### Integração com Segurança por API Key

O sistema de autenticação JWT **coexiste** com a segurança por API Key existente:

- **API Key** (`X-API-Key`): Usada para requisições de serviços/integrações
- **JWT** (`Authorization: Bearer`): Usada para autenticação de usuários

Ambas são suportadas independentemente.

### Tratamento de Erros

#### Token Expirado (401 Unauthorized)
```json
{
  "status": 401,
  "message": "Unauthorized: Token inválido ou expirado",
  "path": "/api/seu-endpoint"
}
```

#### Credenciais Inválidas (401 Unauthorized)
```json
{
  "status": 401,
  "message": "Usuário ou senha inválidos"
}
```

#### Usuário já existe (400 Bad Request)
```json
{
  "status": 400,
  "message": "User already registered with this email"
}
```

### Testes

O projeto inclui testes automatizados. Para executar:
```bash
./gradlew test
```

Configuração de testes usa H2 em memória com JWT habilitado para validação do módulo de autenticação.

