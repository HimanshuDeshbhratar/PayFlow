package com.payflow.auth.repository;

import com.payflow.auth.entity.User;
import com.payflow.common.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByMerchantId(UUID merchantId);
    List<User> findByRole(UserRole role);
    long countByRole(UserRole role);
}
