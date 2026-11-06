package br.com.andrepacheco.imoveis.config;

import jakarta.servlet.DispatcherType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll())
            .formLogin(login -> login.loginPage("/admin/login")
                    .loginProcessingUrl("/admin/login")
                    .usernameParameter("usuario").passwordParameter("senha")
                    .defaultSuccessUrl("/admin/imoveis", true).failureUrl("/admin/login?erro").permitAll())
            .logout(logout -> logout.logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?saiu")
                    .invalidateHttpSession(true).deleteCookies("JSESSIONID"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService users(@Value("${app.admin.username:admin}") String username,
                            @Value("${app.admin.password:}") String configuredPassword,
                            PasswordEncoder encoder) {
        String password = configuredPassword;
        if (password == null || password.isBlank()) {
            byte[] random = new byte[18];
            new SecureRandom().nextBytes(random);
            password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            LoggerFactory.getLogger(SecurityConfig.class).warn(
                    "Senha temporária do painel para {}: {}. Defina ADMIN_PASSWORD para manter uma senha entre reinícios.", username, password);
        }
        return new InMemoryUserDetailsManager(User.withUsername(username).password(encoder.encode(password)).roles("ADMIN").build());
    }
}
