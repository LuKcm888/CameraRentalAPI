package com.camerarental.backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.camerarental.backend.config.ValidationConstraints.*;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="user_id")
    @EqualsAndHashCode.Include
    private UUID userId;

    @NotBlank
    @Size(min= USERNAME_MIN, max = USERNAME_MAX)
    @Column(name="username", nullable = false, length = USERNAME_MAX)
    private String userName;

    @NotBlank
    @Size(max = EMAIL_MAX)
    @Email
    @Column(name="email", nullable = false, length = EMAIL_MAX)
    private String email;

    @NotBlank
    @Size(min = PASSWORD_MIN, max = HASHED_PASSWORD_MAX)
    @Column(name="password", nullable = false, length = HASHED_PASSWORD_MAX)
    private String password;

    public User(String userName, String email, String password) {
        this.userName = userName;
        this.email = email;
        this.password = password;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name="role_id"))
    private Set<Role> roles = new HashSet<>();
}
