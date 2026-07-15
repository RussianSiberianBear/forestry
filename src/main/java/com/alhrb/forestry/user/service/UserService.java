package com.alhrb.forestry.user.service;

import com.alhrb.forestry.common.specification.DynamicSpecificationBuilder;
import com.alhrb.forestry.common.specification.GridPageableBuilder;
import com.alhrb.forestry.dto.abgrid.GridP;
import com.alhrb.forestry.user.User;
import com.alhrb.forestry.user.UserMapper;
import com.alhrb.forestry.user.UserRole;
import com.alhrb.forestry.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private static final Set<String> FILTER_FIELDS = Set.of(
            "id",
            "username",
            "email",
            "fullName",
            "phone",
            "role",
            "isActive",
            "isLocked",
            "lockedAt",
            "lockedUntil",
            "lockReason",
            "lockedBy",
            "lastLoginAt",
            "loginAttempts",
            "createdAt",
            "updatedAt",
            "createdBy",
            "updatedBy"
    );

    private static final Set<String> SORT_FIELDS = Set.of(
            "id",
            "username",
            "email",
            "fullName",
            "phone",
            "role",
            "isActive",
            "isLocked",
            "lockedAt",
            "lockedUntil",
            "lastLoginAt",
            "loginAttempts",
            "createdAt",
            "updatedAt",
            "createdBy",
            "updatedBy"
    );
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + username));
        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findAll(GridP params) {

        Specification<User> specification =
                DynamicSpecificationBuilder.build(
                        params.getFilter(),
                        FILTER_FIELDS
                );

        Pageable pageable =
                GridPageableBuilder.build(
                        params,
                        SORT_FIELDS
                );

        Page page = userRepository
                .findAll(specification, pageable)
                .map(mapper::toDto);

        Map<String, Object> data = Map.of(
                "rows", page.getContent(),
                "totalRecords", page.getTotalElements()
        );
        return Map.of("success", true, "message", "OK", "data", data);
    }

    @Transactional
    public User register(String username, String email, String password, String fullName, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Пользователь с таким именем уже существует");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setLoginAttempts(0);
        user.setRole(UserRole.USER);

        return userRepository.save(user);
    }

    @Transactional
    public User lockUser(Long userId, String reason, Integer minutes, Long lockedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.lock(reason, lockedBy, minutes);
        return userRepository.save(user);
    }

    @Transactional
    public User unlockUser(Long userId, Long unlockedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        user.unlock(unlockedBy);
        return userRepository.save(user);
    }

    @Transactional
    public User activate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setIsActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public User deactivate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        user.setIsActive(false);
        return userRepository.save(user);
    }

    @Transactional
    public Map<String, Object> createUser(GridP p) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> res = new HashMap<>();

        User me = userRepository.findByEmail(auth.getName()).orElseThrow();
        // На всякий случай. Никто кроме Суперадмина и админа не может создавать и редактировать пользователей
        if (me.getRole().ordinal() > UserRole.ADMIN.ordinal()) {
            res.put("success", false);
            res.put("message", "Нет прав на создание данной роли!");
            return res;
        }

        res.put("success", true);
        res.put("message", "Выполнено");
        //      res.put("rows", rows);
        return res;
    }

    @Transactional
    public Map<String, Object> updateUser(GridP p) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> res = new HashMap<>();
        Long userId;

        User me = userRepository.findByEmail(auth.getName()).orElseThrow();
        // На всякий случай. Никто кроме Суперадмина и админа не может создавать и редактировать пользователей
        if (me.getRole().ordinal() > UserRole.ADMIN.ordinal()) {
            res.put("success", false);
            res.put("message", "Нет прав на создание данной роли!");
            return res;
        }

        res.put("success", true);
        res.put("message", "Выполнено");
        //       res.put("rows", rows);
        return res;
    }

    @Transactional
    public Map<String, Object> deleteUser(GridP p) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> res = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        User me = userRepository.findByEmail(auth.getName()).orElseThrow();
        res.put("success", true);
        res.put("message", "Выполнено");
        data.put("opId", p.getOpId());
        res.put("data", data);

        // Никто кроме Суперадмина не может удалять пользователей
        if (me.getRole().ordinal() > UserRole.SUPERADMIN.ordinal()) {
            res.put("success", false);
            res.put("message", "Нет прав для удаления пользователей!");
            return res;
        }
        List<Long> ids = p.getRowIds();
        List<Long> longIds = ids.stream()
                .collect(toList());
        if (longIds.contains(me.getId())) {
            res.put("success", false);
            res.put("message", "Нельзя удалять самого себя!");
            return res;
        }
        if (!longIds.isEmpty()) {
            userRepository.deleteAllByIdInBatch(longIds);
        } else {
            res.put("success", false);
            res.put("message", "Не найдены идентификаторы пользователей для их удаления");
        }
        return res;
    }
}