package com.vaultlink.repository;

import com.vaultlink.entity.RoleEntity;
import com.vaultlink.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleName(Role roleName);
}
