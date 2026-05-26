package com.ecommerce.apigateway.dto;

public record LoginRequestDTO(
    String username,
    String password
) {
}
