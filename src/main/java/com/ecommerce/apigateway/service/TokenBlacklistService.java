package com.ecommerce.apigateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

  private static final String BLACKLIST_PREFIX = "blacklist:";
  private final ReactiveStringRedisTemplate redisTemplate;

  // adiciona token na blacklist com TTL
  public Mono<Boolean> blacklist(String token, long ttlMillis) {
    String key = BLACKLIST_PREFIX + token;
    return redisTemplate.opsForValue()
        .set(key, "revoked", Duration.ofMillis(ttlMillis));
  }

  // verifica se token está na blacklist
  public Mono<Boolean> isBlacklisted(String token) {
    String key = BLACKLIST_PREFIX + token;
    return redisTemplate.hasKey(key);
  }
}