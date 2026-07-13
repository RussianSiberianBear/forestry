package com.alhrb.forestry.user.service;

import com.alhrb.forestry.dto.GridP;
import com.alhrb.forestry.user.Role;
import com.alhrb.forestry.user.User;
import com.alhrb.forestry.user.UserMapper;
import com.alhrb.forestry.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

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

    public List<User> findAll() {
        return userRepository.findAll();
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
        user.setRole(Role.USER);

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

    List<User> findUserWithDynamicFilters(GridP p){
        Map<String, Object> data = Map.of(
                "rows", rows,
                "totalRecords", page.getTotalElements()
        );
        return Map.of("success", true, "message", "OK", "data", data);
    }

    @Transactional
    public Map<String, Object> createUser(GridP p) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> res = new HashMap<>();

        User me = userRepository.findByEmail(auth.getName()).orElseThrow();
        // На всякий случай. Никто кроме Суперадмина и админа не может создавать и редактировать пользователей
        if (me.getRole().ordinal() > Role.ADMIN.ordinal()) {
            res.put("success", false);
            res.put("message", "Нет прав на создание данной роли!");
            return res;
        }


        res.put("success", true);
        res.put("message", "Выполнено");
        res.put("rows", rows);
        return res;
    }

    @Transactional
    public Map<String, Object> updateUser(GridP p) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> res = new HashMap<>();
        Long userId;

        User me = userRepository.findByEmail(auth.getName()).orElseThrow();
        // На всякий случай. Никто кроме Суперадмина и админа не может создавать и редактировать пользователей
        if (me.getRole().ordinal() > Role.ADMIN.ordinal()) {
            res.put("success", false);
            res.put("message", "Нет прав на создание данной роли!");
            return res;
        }

        res.put("success", true);
        res.put("message", "Выполнено");
        res.put("rows", rows);
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
        if (me.getRole().ordinal() > Role.SUPERADMIN.ordinal()) {
            res.put("success", false);
            res.put("message", "Нет прав для удаления пользователей!");
            return res;
        }
        List<Integer> ids = p.getRowIds();
        List<Long> longIds = ids.stream()
                .map(Integer::longValue)
                //             .filter(id -> id != me.getId()) // Не даем удалить самого себя.
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