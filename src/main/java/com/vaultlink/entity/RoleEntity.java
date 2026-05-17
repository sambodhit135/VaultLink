package com.vaultlink.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vaultlink.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "role_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Role roleName;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "roles")
    @JsonIgnore
    private Set<User> users;
}
