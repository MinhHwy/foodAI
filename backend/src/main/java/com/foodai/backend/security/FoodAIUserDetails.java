package com.foodai.backend.security;

import com.foodai.backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class FoodAIUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final User.Role role;
    private final User.AccountStatus accountStatus;
    private final Integer tokenVersion;

    private FoodAIUserDetails(User user) {
        this.userId = Objects.requireNonNull(user.getId(), "User ID is required.");
        this.email = Objects.requireNonNull(user.getEmail(), "User email is required.");
        this.passwordHash = Objects.requireNonNull(user.getPasswordHash(), "Password hash is required.");
        this.role = Objects.requireNonNull(user.getRole(), "User role is required.");
        this.accountStatus = Objects.requireNonNull(user.getAccountStatus(), "Account status is required.");
        this.tokenVersion = Objects.requireNonNull(user.getTokenVersion(), "Token version is required.");
    }

    public static FoodAIUserDetails from(User user) {
        return new FoodAIUserDetails(user);
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus == User.AccountStatus.ACTIVE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == User.AccountStatus.ACTIVE;
    }
}
