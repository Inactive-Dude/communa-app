package com.login.communa.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.login.communa.Entity.Users;
import java.util.Optional;

@Repository
public interface UsersRepo extends JpaRepository<Users, String> {

    Optional<Users> findByEmail(String email);

    Optional<Users> findByResetToken(String token);

    Optional<Users> findByVerificationToken(String token);

    /**
     * Fetches a user row with a pessimistic write lock (SELECT ... FOR UPDATE).
     * Used exclusively inside @Transactional write operations that need to prevent
     * race conditions — e.g., the password-reset cooldown check — where two concurrent
     * requests for the same email could simultaneously bypass the 60-second guard.
     *
     * Must only be called within an active @Transactional context.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Users u WHERE u.email = :email")
    Optional<Users> findByEmailForUpdate(@Param("email") String email);
}