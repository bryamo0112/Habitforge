package com.habitforge.habitforge_backend.repository;

import com.habitforge.habitforge_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    Optional<User> findByVerificationCode(String verificationCode);
    Optional<User> findByResetPasswordCode(String resetPasswordCode);

    
    Optional<User> findByLoginVerificationCode(String loginVerificationCode);
}








