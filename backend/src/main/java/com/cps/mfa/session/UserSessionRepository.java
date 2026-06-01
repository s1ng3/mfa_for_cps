package com.cps.mfa.session;

import com.cps.mfa.common.SessionStatus;
import com.cps.mfa.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionTokenHash(String sessionTokenHash);

    List<UserSession> findByStatus(SessionStatus status);

    List<UserSession> findByUserAndStatus(User user, SessionStatus status);

    long countByUserAndStatus(User user, SessionStatus status);

    List<UserSession> findAllByOrderByCreatedAtDesc();
}
