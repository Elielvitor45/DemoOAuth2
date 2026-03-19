# Documentação Completa — OAuth2 App (Linux)

## Visão geral

```
┌─────────────────────────────────────────────────────────────┐
│  FLUXO OAUTH2 COMPLETO                                       │
│                                                              │
│  1. Angular redireciona browser → Google/Facebook            │
│     (redirect_uri = http://localhost:4200/auth/callback)     │
│                                                              │
│  2. Provedor autentica o usuário e redireciona:             │
│     http://localhost:4200/auth/callback?code=ABC&state=XYZ  │
│                                                              │
│  3. Angular (CallbackComponent) envia o code ao backend:    │
│     POST /api/v1/auth/oauth/callback { provider, code }     │
│                                                              │
│  4. Backend troca code por access_token no provedor          │
│     Busca dados do usuário, salva no MySQL                   │
│     Gera JWT interno + refresh token                         │
│     Retorna { accessToken, refreshToken, expiresIn }         │
│                                                              │
│  5. Angular armazena tokens em memória                       │
│     Todas as chamadas usam: Authorization: Bearer <jwt>      │
└─────────────────────────────────────────────────────────────┘
```

---

## Pré-requisitos

### 1. Verificar Java

```bash
java -version
# Precisa ser Java 17 ou superior
# Se não tiver: sudo apt install openjdk-17-jdk
```

### 2. Verificar Maven

```bash
mvn -version
# Se não tiver: sudo apt install maven
```

### 3. Verificar Node.js e npm

```bash
node -v   # precisa ser 18+
npm -v

# Se não tiver Node 18+:
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
```

### 4. Instalar Angular CLI globalmente

```bash
npm install -g @angular/cli@17

# Verificar:
ng version
```

### 5. Verificar MySQL

```bash
mysql --version
# Se não tiver:
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

---

## PARTE 1 — Configurar MySQL

```bash
# Acessa o MySQL como root
sudo mysql

# Dentro do MySQL:
CREATE DATABASE oauth2_app_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Cria usuário (opcional — pode usar root mesmo em dev)
CREATE USER 'oauth2user'@'localhost' IDENTIFIED BY 'suasenha';
GRANT ALL PRIVILEGES ON oauth2_app_dev.* TO 'oauth2user'@'localhost';
FLUSH PRIVILEGES;

EXIT;
```

Verificar conexão:
```bash
mysql -u root -p oauth2_app_dev
# ou com o usuário criado:
mysql -u oauth2user -p oauth2_app_dev
```

---

## PARTE 2 — Configurar credenciais OAuth2

### Google

1. Acesse: https://console.cloud.google.com/apis/credentials
2. Crie um projeto (ou selecione um existente)
3. Clique em **"Criar credenciais" → "ID do cliente OAuth 2.0"**
4. Tipo: **"Aplicativo da Web"**
5. Em **"URIs de redirecionamento autorizados"**, adicione:
   ```
   http://localhost:4200/auth/callback
   ```
6. Copie o **Client ID** e o **Client Secret**

> ⚠️ O redirect URI nos consoles deve apontar para o **Angular** (`localhost:4200`),
> não para o backend. O Angular recebe o code e encaminha ao backend via POST.

### Facebook

1. Acesse: https://developers.facebook.com/apps
2. Crie um app → tipo **"Consumidor"** (ou "Nenhum")
3. Vá em: **Facebook Login → Configurações**
4. Em **"URIs de redirecionamento OAuth válidos"**, adicione:
   ```
   http://localhost:4200/auth/callback
   ```
5. Vá em **Configurações → Básico** e copie **ID do App** e **Chave Secreta do App**

---

## PARTE 3 — Configurar o Backend

```bash
cd backend

# Copiar arquivo de variáveis de ambiente
cp .env.example .env

# Editar com suas credenciais
nano .env
# ou:
gedit .env
```

Preencha o `.env`:

```dotenv
DB_USERNAME=root
DB_PASSWORD=sua_senha_mysql
JWT_SECRET=gere-com-openssl-rand-base64-64
GOOGLE_CLIENT_ID=seu-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-seu-secret
FACEBOOK_CLIENT_ID=123456789
FACEBOOK_CLIENT_SECRET=abcdef123456
CORS_ALLOWED_ORIGINS=http://localhost:4200
SPRING_PROFILES_ACTIVE=dev
```

Gerar um JWT_SECRET seguro:
```bash
openssl rand -base64 64
# Copie o resultado e cole em JWT_SECRET no .env
```

### Subir o backend

```bash
cd backend

# Exporta as variáveis do .env para o ambiente atual
export $(grep -v '^#' .env | xargs)

# Compila e inicia o Spring Boot
mvn spring-boot:run

# Deve aparecer:
# Started ApiApplication in X.XXX seconds
# Tomcat started on port 8080
```

O Flyway vai criar automaticamente as tabelas `users`, `user_providers` e `refresh_tokens`.

### Verificar que subiu

```bash
curl http://localhost:8080/actuator/health
# Resposta esperada: {"status":"UP"}
```

---

## PARTE 4 — Configurar o Frontend Angular

```bash
cd frontend

# Instalar dependências
npm install
```

### Configurar o environment

Edite `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: '/api',
  oauth2: {
    google: {
      clientId: 'SEU_GOOGLE_CLIENT_ID_AQUI',
      authorizationUri: 'https://accounts.google.com/o/oauth2/v2/auth',
      redirectUri: 'http://localhost:4200/auth/callback',
      scopes: 'openid profile email',
    },
    facebook: {
      clientId: 'SEU_FACEBOOK_APP_ID_AQUI',
      authorizationUri: 'https://www.facebook.com/v18.0/dialog/oauth',
      redirectUri: 'http://localhost:4200/auth/callback',
      scopes: 'public_profile,email',
    },
  },
  tokenRefreshThresholdMs: 60_000,
} as const;
```

### Subir o frontend

```bash
npm start
# ou:
ng serve

# Deve aparecer:
# Application bundle generation complete.
# Local: http://localhost:4200/
```

---

## PARTE 5 — Testar

### Ordem de inicialização

```
Terminal 1: MySQL (já rodando como serviço)
Terminal 2: Backend Spring Boot (porta 8080)
Terminal 3: Frontend Angular (porta 4200)
```

### Teste 1 — Cadastro com email

1. Abra `http://localhost:4200`
2. Clique em **"Criar conta"**
3. Preencha nome, email e senha (mínimo 8 caracteres)
4. Deve redirecionar para o dashboard com seu nome e badge **Email**

### Teste 2 — Login com Google

1. Vá para `http://localhost:4200/login`
2. Clique em **"Continuar com Google"**
3. O browser vai para `accounts.google.com`
4. Faça login com sua conta Google
5. O Google redireciona para `http://localhost:4200/auth/callback?code=...`
6. O Angular envia o code para o backend
7. O backend troca o code pelo token do Google, busca seus dados, salva no banco
8. O dashboard aparece com sua **foto do Google** e badge **Google**

### Teste 3 — Vincular Google e Facebook ao mesmo usuário

1. Faça login com Google
2. Faça logout
3. Faça login com o mesmo email via Facebook
4. O backend detecta que o email já existe (cadastrado via Google)
5. Vincula o Facebook ao mesmo usuário
6. O dashboard mostra badges: **Google** e **Facebook**

### Teste 4 — Verificar banco de dados

```bash
mysql -u root -p oauth2_app_dev

SELECT id, name, email, photo_url FROM users;
SELECT user_id, provider, provider_id FROM user_providers;
SELECT id, user_id, LEFT(token_hash,16), expires_at, revoked_at FROM refresh_tokens;
```

### Teste 5 — Verificar JWT via curl

```bash
# 1. Faça login e copie o accessToken do response de rede (DevTools)
# 2. Cole no lugar de SEU_JWT:
curl -H "Authorization: Bearer SEU_JWT" http://localhost:8080/api/v1/users/me
```

---

## Estrutura de arquivos gerada

```
oauth2-app/
├── backend/
│   ├── pom.xml
│   ├── .env.example
│   └── src/main/
│       ├── java/com/example/api/
│       │   ├── ApiApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   └── JpaConfig.java
│       │   ├── domain/
│       │   │   ├── enums/AuthProvider.java        (GOOGLE, FACEBOOK, LOCAL)
│       │   │   ├── model/User.java
│       │   │   ├── model/UserProvider.java        (com columnDefinition fix)
│       │   │   ├── model/RefreshToken.java
│       │   │   └── repository/...
│       │   ├── application/
│       │   │   ├── auth/AuthService.java
│       │   │   └── user/UserService.java
│       │   ├── infrastructure/
│       │   │   ├── oauth/GoogleOAuthClient.java
│       │   │   ├── oauth/FacebookOAuthClient.java
│       │   │   └── security/JwtService.java + Filter
│       │   └── web/
│       │       ├── controller/AuthController.java
│       │       ├── controller/UserController.java
│       │       └── dto/ + exception/
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── db/migration/
│               ├── V1__create_users_table.sql
│               ├── V2__create_user_providers_table.sql
│               └── V3__create_refresh_tokens_table.sql
│
└── frontend/
    ├── package.json
    ├── angular.json
    ├── tsconfig.json
    ├── proxy.conf.json
    └── src/
        ├── main.ts
        ├── index.html
        ├── styles.scss
        ├── environments/environment.ts
        └── app/
            ├── app.component.ts
            ├── app.config.ts
            ├── app.routes.ts
            ├── core/
            │   ├── models/auth.model.ts
            │   ├── models/user.model.ts
            │   ├── services/auth.service.ts
            │   ├── interceptors/auth.interceptor.ts
            │   └── guards/auth.guard.ts
            └── features/
                ├── auth/
                │   ├── login.component.ts      (Google + Facebook + email/senha)
                │   ├── register.component.ts
                │   └── callback.component.ts
                └── dashboard/
                    └── dashboard.component.ts  (foto + badges de providers)
```

---

## Erros comuns e soluções

### "wrong column type encountered in column [provider]"

**Causa:** Hibernate 6 valida o tipo da coluna. A migration criou `VARCHAR(20)` mas
o Hibernate esperava `ENUM`. 

**Solução aplicada:** `UserProvider.java` usa:
```java
@Column(nullable = false, columnDefinition = "VARCHAR(20)")
```
Isso instrui o Hibernate a aceitar VARCHAR sem tentar criar ENUM.

---

### "Port 8080 already in use"

```bash
# Encontra e mata o processo
sudo lsof -ti:8080 | xargs kill -9
# ou:
sudo fuser -k 8080/tcp
```

---

### "Port 4200 already in use"

```bash
sudo lsof -ti:4200 | xargs kill -9
```

---

### Erro de CORS no browser

Verifique se `CORS_ALLOWED_ORIGINS=http://localhost:4200` está no `.env`
e se o backend foi reiniciado após a mudança.

---

### "Access blocked: redirect_uri_mismatch" (Google)

O redirect URI no Google Console não bate com o enviado pelo Angular.
Certifique-se de que no console está cadastrado exatamente:
```
http://localhost:4200/auth/callback
```
(sem barra no final, sem https, com o path correto)

---

### Foto do Google não aparece

O Google retorna URLs do tipo `https://lh3.googleusercontent.com/...`.
Adicione `referrerpolicy="no-referrer"` na tag `<img>` — já está no DashboardComponent.

---

### Facebook não retorna email

O Facebook requer que o app passe por revisão para acessar o email de usuários
que não são administradores do app. Em desenvolvimento, use sua própria conta
de administrador do app para testar.

---

## Comandos de desenvolvimento

```bash
# ── Backend ──────────────────────────────────────────────────

# Compilar sem rodar
mvn compile

# Rodar testes
mvn test

# Gerar JAR
mvn package -DskipTests

# Ver logs em tempo real (se já rodando em background)
tail -f logs/spring.log

# Restartar após mudança de código
# Ctrl+C no terminal do mvn spring-boot:run, depois:
export $(grep -v '^#' .env | xargs) && mvn spring-boot:run

# ── Frontend ─────────────────────────────────────────────────

# Instalar dependências
npm install

# Rodar em dev (com proxy para backend)
npm start

# Build de produção
npm run build:prod

# Ver quais versões estão instaladas
ng version
```

---

## Resumo dos endpoints do backend

| Método | Endpoint                    | Auth | Descrição                         |
|--------|-----------------------------|------|-----------------------------------|
| POST   | /api/v1/auth/register       | ❌   | Cadastro email + senha            |
| POST   | /api/v1/auth/login          | ❌   | Login email + senha               |
| POST   | /api/v1/auth/oauth/callback | ❌   | Troca code OAuth2 por JWT         |
| POST   | /api/v1/auth/refresh        | ❌   | Renova access token               |
| POST   | /api/v1/auth/logout         | ✅   | Revoga refresh tokens             |
| GET    | /api/v1/users/me            | ✅   | Dados do usuário autenticado      |
