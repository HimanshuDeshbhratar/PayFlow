package com.payflow.user.controller;

import com.payflow.auth.dto.UserResponse;
import com.payflow.auth.repository.UserRepository;
import com.payflow.auth.security.UserPrincipal;
import com.payflow.auth.service.AuthService;
import com.payflow.common.exception.PayFlowException;
import com.payflow.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;

    @GetMapping("/me")
    public UserResponse me() {
        UserPrincipal principal = SecurityUtils.currentUser();
        return userRepository.findById(principal.getId())
                .map(authService::toUserResponse)
                .orElseThrow(() -> PayFlowException.notFound("User not found"));
    }
}
