# E-Commerce Microservices — Testing Guide

## Índice
1. [Subir os serviços](#subir-os-serviços)
2. [Autenticação (Login)](#autenticação-login)
3. [Endpoints disponíveis](#endpoints-disponíveis)
4. [Fluxo completo de teste](#fluxo-completo-de-teste)

---

## Subir os serviços

### Pré-requisitos
- Docker e Docker Compose instalados
- WSL 2 (se estiver no Windows)
- Os 4 repositórios clonados:
    - `ms-java-order-service`
    - `ms-java-inventory-service`
    - `ms-java-notification-service`
    - `api-gateway-java`
    - `infra-local`

### Passo 1: Gerar os JARs

Em cada repositório de microserviço, execute:

```bash
./mvnw clean package -DskipTests
```

Isso gera o arquivo `.jar` em `target/` que será usado pelo Docker.

### Passo 2: Subir a infraestrutura

```bash
cd infra-local
docker-compose up -d
```

Aguarde ~30-40 segundos para todos os containers iniciarem.

### Verificar status

```bash
docker-compose ps
```

Deve mostrar todos os serviços rodando:
```
NAME                      STATUS
order-service             Up 
inventory-service         Up
notification-service      Up
api-gateway               Up
order-db                  Up
inventory-db              Up
kafka                     Up
zookeeper                 Up
rabbitmq                  Up
redis                     Up
```

### Parar os serviços

```bash
docker-compose down
```

---

## Autenticação (Login)

### Credenciais padrão

```
Username: admin
Password: admin123
```

### Obter Token JWT

**Requisição:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Resposta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcyODM1MTIzNCwiZXhwIjoxNzI4MzU0ODM0fQ.xxx"
}
```

> **Importante:** Copie o token da resposta. Você vai usar em todas as outras requisições com o header `Authorization: Bearer {token}`

### Logout (Blacklist do token)

**Requisição:**
```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer {token}"
```

**Resposta (204 No Content):**
Sem body na resposta — apenas confirma que o token foi adicionado à blacklist.

---

## Endpoints disponíveis

### 1. Order Service

#### Listar todos os pedidos
```bash
curl -X GET http://localhost:8080/api/orders \
  -H "Authorization: Bearer {token}"
```

**Resposta (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "customerId": "customer-001",
    "status": "PENDING",
    "createdAt": "2026-06-01T10:30:00Z"
  }
]
```

#### Obter pedido por ID
```bash
curl -X GET http://localhost:8080/api/orders/{id} \
  -H "Authorization: Bearer {token}"
```

**Resposta (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "customer-001",
  "status": "PENDING",
  "createdAt": "2026-06-01T10:30:00Z"
}
```

#### Criar novo pedido
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-001",
    "items": [
      {
        "productId": "DELL-001",
        "quantity": 2,
        "price": 99.90
      },
      {
        "productId": "LOG-002",
        "quantity": 1,
        "price": 149.50
      }
    ]
  }'
```

**Resposta (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "customer-001",
  "status": "PENDING",
  "createdAt": "2026-06-01T10:35:00Z"
}
```

#### Atualizar status do pedido
```bash
curl -X PUT http://localhost:8080/api/orders/{id} \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED"
  }'
```

**Valores válidos para status:** `PENDING`, `CONFIRMED`, `CANCELLED`

**Resposta (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "customerId": "customer-001",
  "status": "CONFIRMED",
  "createdAt": "2026-06-01T10:35:00Z"
}
```

#### Deletar pedido
```bash
curl -X DELETE http://localhost:8080/api/orders/{id} \
  -H "Authorization: Bearer {token}"
```

**Resposta (204 No Content):**
Sem body — apenas confirma que foi deletado.

---

### 2. Inventory Service

#### Listar produtos
```bash
curl -X GET http://localhost:8080/api/inventory \
  -H "Authorization: Bearer {token}"
```

**Resposta (200 OK):**
```json
[
  {
    "id": "1",
    "sku": "DELL-001",
    "name": "Dell Monitor",
    "quantity": 50
  },
  {
    "id": "2",
    "sku": "LOG-002",
    "name": "Logitech Mouse",
    "quantity": 100
  }
]
```

#### Obter produto por SKU
```bash
curl -X GET http://localhost:8080/api/inventory/{sku} \
  -H "Authorization: Bearer {token}"
```

---

## Fluxo completo de teste

### Cenário: Criar um pedido e verificar o processamento

#### 1. Login e obter token
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

echo "Token: $TOKEN"
```

#### 2. Verificar estoque inicial
```bash
curl -X GET http://localhost:8080/api/inventory \
  -H "Authorization: Bearer $TOKEN" | jq
```

Você deve ver produtos com quantidade disponível.

#### 3. Criar um pedido
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "test-customer",
    "items": [
      {
        "productId": "DELL-001",
        "quantity": 2,
        "price": 99.90
      }
    ]
  }' | jq
```

Copie o `id` do pedido retornado.

#### 4. Verificar logs dos serviços

**Order Service** — deve mostrar que o pedido foi criado e evento publicado:
```bash
docker-compose logs order-service | grep -i "pedido\|order\|kafka"
```

**Inventory Service** — deve mostrar que o evento foi consumido e estoque debitado:
```bash
docker-compose logs inventory-service | grep -i "estoque\|stock\|kafka"
```

**Notification Service** — deve mostrar que a notificação foi enviada:
```bash
docker-compose logs notification-service | grep -i "notif\|notification\|rabbitmq"
```

#### 5. Verificar se o estoque foi reduzido
```bash
curl -X GET http://localhost:8080/api/inventory \
  -H "Authorization: Bearer $TOKEN" | jq
```

O DELL-001 deve estar com `quantity: 48` (50 - 2)

#### 6. Fazer logout (blacklist do token)
```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

#### 7. Tentar acessar com token blacklisted (deve retornar 401)
```bash
curl -X GET http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN"
```

Resposta esperada: `401 Unauthorized`

---

## Produtos disponíveis para teste

| SKU | Nome | Quantidade Inicial |
|---|---|---|
| DELL-001 | Dell Monitor | 50 |
| LOG-002 | Logitech Mouse | 100 |
| TEC-003 | Teclado Mecânico | 75 |

---

## Troubleshooting

### Erro: "Connection refused"
- Verifique se todos os containers estão rodando: `docker-compose ps`
- Aguarde 30+ segundos para os serviços ficarem prontos
- Verifique os logs: `docker-compose logs`

### Erro: "Token inválido"
- Gere um novo token com o `/auth/login`
- Verifique se o token ainda é válido (expira em 1 hora)
- Se fez logout, o token está na blacklist do Redis

### Erro: "Estoque insuficiente"
- Verifique a quantidade disponível em `/api/inventory`
- Se acabou, aguarde ou crie massa de dados novamente

### Ver tudo em tempo real
```bash
docker-compose logs -f
```

---

## Próximas melhorias

- [ ] Testes unitários (JAV-27, JAV-28)
- [ ] Swagger/OpenAPI documentation (JAV-30)
- [ ] Mais cenários de teste automatizados
