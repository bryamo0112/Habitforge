package com.habitforge.habitforge_backend.repository;

import com.habitforge.habitforge_backend.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findByEmail(String email);
    Optional<VerificationCode> findByEmailAndCode(String email, String code);
    void deleteByEmail(String email); // Cleanup existing codes if needed
}

