package com.vaultlink.repository;

import com.vaultlink.entity.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {

    Optional<AccessToken> findByToken(String token);

    List<AccessToken> findByDocumentId(Long documentId);
}
