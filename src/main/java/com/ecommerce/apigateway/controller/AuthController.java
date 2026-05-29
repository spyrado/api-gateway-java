package com.ecommerce.apigateway.controller;

import com.ecommerce.apigateway.dto.LoginRequestDTO;
import com.ecommerce.apigateway.dto.LoginResponseDTO;
import com.ecommerce.apigateway.service.JwtService;
import com.ecommerce.apigateway.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);
  private final JwtService jwtService;
  private final TokenBlacklistService tokenBlacklistService;

  @PostMapping("/login")
  public Mono<ResponseEntity<LoginResponseDTO>> login(@RequestBody LoginRequestDTO request) {
    // usuário hardcoded para aprendizado
    if("admin".equals(request.username()) && "admin123".equals(request.password())) {
      String token = jwtService.generateToken(request.username());
      log.info("Token gerado para o usuário: {}", request.username());
      return Mono.just(ResponseEntity.ok(new LoginResponseDTO(token)));
    }

    return Mono.just(ResponseEntity.status(401).build());
  }

  @PostMapping("/logout")
  public Mono<ResponseEntity<Void>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    String token = authHeader.substring(7);
    long ttl = jwtService.getRemainingTtl(token);

    return tokenBlacklistService.blacklist(token, ttl)
        .map(result -> ResponseEntity.noContent().<Void>build());
  }

}
