package com.noh.zup.security;

import com.noh.zup.domain.admin.AdminRole;

public record JwtAdminPrincipal(
        String email,
        AdminRole role
) {
}
