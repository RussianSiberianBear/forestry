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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
                // Отключаем CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // Настройка авторизации
                .authorizeHttpRequests(auth -> auth
                        // 1) Закрытые endpoints
                        .requestMatchers("/admin/**").hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("SUPERADMIN", "ADMIN")
                    //    .requestMatchers("/api/uploadForestStand").hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/abgrid-engine/apanel/**").hasAnyRole("SUPERADMIN", "ADMIN")
                        .requestMatchers("/apanel/**").hasAnyRole("SUPERADMIN", "ADMIN")

                        // 2) Публичные endpoints
                        .requestMatchers("/api/uploadForestStand").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/webjars/**").permitAll()
                        .requestMatchers("/login", "/register", "/api/users/register").permitAll()
                        .requestMatchers("/api/territory/**").permitAll()
                        .requestMatchers("/api/ui-settings/**").permitAll()
                        .requestMatchers("/api/cutting-area/map-data").permitAll()
                        .requestMatchers("/api/cutting-area/map-data-filtered").permitAll()

                        // 3) Требуют аутентификации
                        .requestMatchers("/api/common/**").authenticated()

                        // 4) Все остальное требует аутентификации
                        .anyRequest().authenticated()
                )

                // ⭐ ИСПРАВЛЕНИЕ: Правильная обработка исключений для API
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Проверяем, является ли запрос AJAX/API
                            String requestedWith = request.getHeader("X-Requested-With");
                            String accept = request.getHeader("Accept");

                            // Если это API запрос или AJAX - возвращаем JSON
                            if (request.getRequestURI().startsWith("/api/") ||
                                    "XMLHttpRequest".equals(requestedWith) ||
                                    (accept != null && accept.contains("application/json"))) {
                                writeJsonFail(response, 401, "Сессия истекла. Требуется авторизация");
                            } else {
                                // Иначе - редирект на страницу логина
                                response.sendRedirect("/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String requestedWith = request.getHeader("X-Requested-With");
                            String accept = request.getHeader("Accept");

                            if (request.getRequestURI().startsWith("/api/") ||
                                    "XMLHttpRequest".equals(requestedWith) ||
                                    (accept != null && accept.contains("application/json"))) {
                                writeJsonFail(response, 403, "Доступ запрещен");
                            } else {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                            }
                        })
                )

                // Настройка form login для HTML страниц
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

                // Настройка logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // Настройка сессий
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .expiredUrl("/login?expired")
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