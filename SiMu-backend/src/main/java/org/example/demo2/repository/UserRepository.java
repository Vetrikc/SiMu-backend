package org.example.demo2.repository;

import org.example.demo2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
   Optional <User> findUserByUsername(String username);
   boolean existsByUsername(String username);
   boolean existsByEmail(String email);

}
