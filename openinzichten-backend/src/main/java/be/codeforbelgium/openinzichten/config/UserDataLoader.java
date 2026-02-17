package be.codeforbelgium.openinzichten.config;

import be.codeforbelgium.openinzichten.domain.Account;
import be.codeforbelgium.openinzichten.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class UserDataLoader {

    private static final Logger log = LoggerFactory.getLogger(UserDataLoader.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String adminEmail;
    private final String adminPassword;

    public UserDataLoader(@Value("${app.admin.login.email}") String adminEmail, @Value("${app.admin.login.password}") String adminPassword) {
        // Do not allow empty admin credentials
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalArgumentException("Admin email must be provided in configuration (app.admin.login.email)");
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalArgumentException("Admin password must be provided in configuration (app.admin.login.password)");
        }

        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Bean
    protected CommandLineRunner commandLineRunner(AccountRepository accountRepository) {
        return args -> {
            var superAdminAccount = accountRepository.findByEmail(adminEmail);

            if (superAdminAccount.isPresent()) {
                log.info("Admin account already exists; skipping creation.");
                return;
            }

            var baseAdminAccount = Account.builder()
                    .roles(List.of("Admin", "User"))
                    .email(adminEmail)
                    .username("Administrator")
                    .password(passwordEncoder.encode(adminPassword))
                    .build();

            accountRepository.save(baseAdminAccount);

            log.info(
                    "Created default admin account with email 'admin@openinzicht.be' and password '0penInzicht?1PXL'.");
        };
    }
}
