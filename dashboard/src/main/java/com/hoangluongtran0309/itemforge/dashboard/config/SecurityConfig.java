package com.hoangluongtran0309.itemforge.dashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Form login with a single admin account. CSRF stays ENABLED (the default): mutating
 * requests go through htmx (see static/js/htmx-csrf.js for how the header is attached), and
 * this is a session-cookie admin tool, so disabling CSRF would be the wrong trade-off.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(registry -> registry
                        .requestMatchers("/css/**", "/js/**", "/webjars/**", "/icons/**", "/images/**", "/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(DashboardAdminProperties properties) {
        UserDetails admin = User.withUsername(properties.username())
                .password(properties.passwordHash())
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
