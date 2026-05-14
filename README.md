# api-gateway-java

API Gateway do sistema e-commerce. Ponto de entrada único para todos os microserviços,
responsável por roteamento, autenticação JWT e blacklist de tokens.

## Tecnologias
- Java 21
- Spring Boot 4.0.6
- Spring Cloud Reactive Gateway
- Spring Security
- Spring Data Reactive Redis (blacklist JWT)
- Maven

## Pré-requisitos
- Java 21 instalado
- infra-local rodando (`docker-compose up`)

## Como rodar
```bash
./mvnw spring-boot:run
```

## Porta
Roda na porta `8080`

## Rotas
| Rota | Serviço destino | Porta |
|---|---|---|
| /api/orders/** | ms-java-order-service | 8081 |
| /api/inventory/** | ms-java-inventory-service | 8082 |

## Autenticação
| Método | Rota | Descrição |
|---|---|---|
| POST | /auth/login | Gera token JWT |
| POST | /auth/logout | Invalida token (blacklist Redis) |

## Fluxo de autenticação
1. Cliente faz POST /auth/login e recebe o JWT
2. Cliente envia o JWT no header Authorization: Bearer {token}
3. Gateway valida o token e verifica blacklist no Redis
4. Se válido, roteia para o serviço correspondente