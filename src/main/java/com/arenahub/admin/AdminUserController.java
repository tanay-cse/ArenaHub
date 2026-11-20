package com.arenahub.admin;

import com.arenahub.audit.AuditService;
import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.loyalty.LoyaltyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AdminUserController(UserRepository userRepository,
                               LoyaltyService loyaltyService,
                               AuditService auditService,
                               ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.loyaltyService = loyaltyService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public record AdminUserDTO(
            Long id,
            String email,
            String fullName,
            String phone,
            String roleName,
            boolean isActive,
            int pointsBalance
    ) {}

    @GetMapping
    public String listUsers(Model model) {
        List<AdminUserDTO> users = userRepository.findAll().stream()
                .map(user -> new AdminUserDTO(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getPhone(),
                        user.getRole() != null ? user.getRole().getName() : "STUDENT",
                        Boolean.TRUE.equals(user.getIsActive()),
                        loyaltyService.getBalance(user.getId())
                ))
                .toList();

        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/{id}/toggle-active")
    @Transactional
    public String toggleActive(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        boolean oldActive = Boolean.TRUE.equals(user.getIsActive());
        boolean newActive = !oldActive;
        user.setIsActive(newActive);
        userRepository.save(user);

        Long adminId = resolveAdminId(userDetails);
        auditService.log(
                adminId,
                "TOGGLE_USER_ACTIVE",
                "users",
                user.getId(),
                toJson(Map.of("isActive", oldActive)),
                toJson(Map.of("isActive", newActive))
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "User " + user.getEmail() + " status updated to " + (newActive ? "ACTIVE" : "INACTIVE (BANNED)") + "."
        );
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-points")
    @Transactional
    public String resetPoints(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        int currentBalance = loyaltyService.getBalance(user.getId());
        if (currentBalance != 0) {
            loyaltyService.addPoints(user.getId(), -currentBalance, "ADJUSTMENT", "Admin reset");
        }

        Long adminId = resolveAdminId(userDetails);
        auditService.log(
                adminId,
                "RESET_POINTS",
                "users",
                user.getId(),
                toJson(Map.of("pointsBalance", currentBalance)),
                toJson(Map.of("pointsBalance", 0))
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Points balance for " + user.getEmail() + " reset to 0 (deducted " + currentBalance + " points)."
        );
        return "redirect:/admin/users";
    }

    private Long resolveAdminId(UserDetails userDetails) {
        if (userDetails != null) {
            return userRepository.findByEmail(userDetails.getUsername())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
