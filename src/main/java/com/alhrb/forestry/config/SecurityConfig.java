package com.alhrb.forestry.config;

import com.alhrb.forestry.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Lazy
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private static final ObjectMapper OM = new ObjectMapper();

    private static void writeJsonFail(HttpServletResponse res, int status, String message) {
        try {
            res.setStatus(status);
            res.setCharacterEncoding("UTF-8");
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("message", message);
            body.put("data", null);

            OM.writeValue(res.getWriter(), body);
        } catch (Exception ignored) {
            // ignore
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Вместо csrf(csrf -> csrf.disable()) используем новый синтаксис
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1) Закрытое — первым!
                        .requestMatchers("/admin/**").hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/abgrid-engine/apanel/**").hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/apanel/**").hasAnyRole("SUPERADMIN", "ADMIN")

                        .requestMatchers("/css/**", "/js/**", "/webjars/**").permitAll()
                        .requestMatchers("/login", "/register", "/api/users/register").permitAll()
                        //    .requestMatchers("/api/common/**").permitAll()
                        .requestMatchers("/api/common/**").authenticated()
                        .requestMatchers("/api/territory/**").permitAll()
                        .requestMatchers("/api/ui-settings/**").permitAll()
                        .requestMatchers("/api/cutting-area/map-data").permitAll()
                        .requestMatchers("/api/cutting-area/map-data-filtered").permitAll()
                        .anyRequest().authenticated()
                )
                // ✅ Для API: не редирект на HTML-логин, а 401/403 JSON
                // Используем .requestMatchers() вместо AntPathRequestMatcher
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                (req, res, e) -> writeJsonFail(res, 401, "Сессия истекла\nТребуется авторизация"),
                                (request) -> request.getRequestURI().startsWith("/api/")  // ← замена AntPathRequestMatcher
                        )
                        .defaultAccessDeniedHandlerFor(
                                (req, res, e) -> writeJsonFail(res, 403, "Forbidden"),
                                (request) -> request.getRequestURI().startsWith("/api/")  // ← замена AntPathRequestMatcher
                        )
                )
                // Для страниц — обычный formLogin
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", false)
                        .failureHandler((req, res, ex2) -> {
                            if (ex2 instanceof DisabledException) {
                                res.sendRedirect("/login?disabled=1");
                            } else {
                                res.sendRedirect("/login?error=1");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}