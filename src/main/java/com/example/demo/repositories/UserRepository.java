package com.example.demo.repositories;

import com.example.demo.entity.User;
import com.example.demo.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByRolesContaining(Role role);

}