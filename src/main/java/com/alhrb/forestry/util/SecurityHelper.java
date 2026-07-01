package com.alhrb.forestry.util;

import com.alhrb.forestry.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityHelper {

    /**
     * Получить ID текущего авторизованного пользователя
     */
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * Получить текущего авторизованного пользователя
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        return null;
    }

    /**
     * Получить имя текущего пользователя
     */
    public String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * Проверить, авторизован ли пользователь
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String);
    }
}