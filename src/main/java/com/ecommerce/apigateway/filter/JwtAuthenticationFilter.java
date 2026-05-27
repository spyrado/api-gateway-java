package com.ecommerce.apigateway.filter;

import com.ecommerce.apigateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private final JwtService jwtService;

  // rotas públicas que não precisam de autenticação
  private static final List<String> PUBLIC_ROUTES = List.of("/auth/login");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();

    // libera rotas públicas
    if (PUBLIC_ROUTES.stream().anyMatch(path::startsWith)) {
      return chain.filter(exchange);
    }
    exchange.getRequest().getPath().value();
    // busca o token no header Authorization
    String authHeader = exchange.getRequest()
        .getHeaders()
        .getFirst(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.warn("Requisição sem token JWT: {}", path);
      return unauthorized(exchange);
    }

    String token = authHeader.substring(7); // remove "Bearer "

    if (!jwtService.isTokenValid(token)) {
      log.warn("Token JWT inválido para: {}", path);
      return unauthorized(exchange);
    }

    log.info("Token JWT válido - usuário: {}", jwtService.extractUsername(token));
    return chain.filter(exchange);
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    return -1; // executa antes dos outros filtros
  }
}