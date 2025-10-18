package com.login.communa.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.login.communa.Entity.Users;
import java.util.Optional;

@Repository
public interface UsersRepo extends JpaRepository<Users, String> {
    Optional<Users> findByEmail(String email);
    Optional<Users> findByResetToken(String token);
}