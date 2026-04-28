package com.noh.zup.domain.admin;

public record AdminLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String email,
        AdminRole role
) {
}
