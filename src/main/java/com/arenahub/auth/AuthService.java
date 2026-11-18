package com.arenahub.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerUser(RegisterRequest request) {
        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new DuplicateUserException("email", "An account with this email already exists");
        }

        String phone = (request.getPhone() != null && !request.getPhone().trim().isEmpty()) 
                ? request.getPhone().trim() 
                : null;

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new DuplicateUserException("phone", "An account with this phone number already exists");
        }

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("Default role STUDENT not found in database"));

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(trimmedEmail);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(studentRole);
        user.setIsActive(true);

        userRepository.save(user);
    }
}
