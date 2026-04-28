package com.noh.zup.global.seed;

import com.noh.zup.domain.admin.AdminRole;
import com.noh.zup.domain.admin.AdminUser;
import com.noh.zup.domain.admin.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalAdminUserSeedRunner implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public LocalAdminUserSeedRunner(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUserRepository.existsByEmail(adminEmail)) {
            return;
        }

        adminUserRepository.save(new AdminUser(
                adminEmail,
                passwordEncoder.encode(adminPassword),
                AdminRole.SUPER_ADMIN
        ));
    }
}
