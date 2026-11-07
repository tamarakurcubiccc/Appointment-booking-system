package com.booking.user.repository;

import com.booking.user.entity.AccountToken;
import com.booking.user.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

    Optional<AccountToken> findByTokenAndType(String token, TokenType type);
}
