package com.noh.zup.domain.admin;

import com.noh.zup.common.exception.BusinessException;
import com.noh.zup.common.exception.ErrorCode;
import com.noh.zup.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser adminUser = adminUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid admin credentials"));

        if (!adminUser.isActive() || !passwordEncoder.matches(request.password(), adminUser.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid admin credentials");
        }

        String token = jwtTokenProvider.createToken(adminUser.getEmail(), adminUser.getRole());
        return new AdminLoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                adminUser.getEmail(),
                adminUser.getRole()
        );
    }
}
