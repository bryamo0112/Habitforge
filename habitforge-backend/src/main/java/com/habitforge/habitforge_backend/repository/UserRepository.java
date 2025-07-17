package com.habitforge.habitforge_backend.repository;

import com.habitforge.habitforge_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username); // Use this if case-sensitive usernames are OK

    // Optional alternative:
    // User findByUsernameIgnoreCase(String username);
}


