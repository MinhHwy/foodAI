package com.foodai.backend.service;

import com.foodai.backend.entity.User;
import com.foodai.backend.exception.FoodApiException;
import com.foodai.backend.repository.UserRepository;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Adapter: auth module must expose a verified principal whose name is user email. */
@Component
public class CurrentFoodUser {
    private final UserRepository users;
    public CurrentFoodUser(UserRepository users) { this.users = users; }

    public User require(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new FoodApiException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        User user = users.findByEmail(principal.getName())
                .orElseThrow(() -> new FoodApiException(HttpStatus.UNAUTHORIZED, "Unknown user."));
        return requireActive(user);
    }

    private User requireActive(User user) {
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new FoodApiException(HttpStatus.FORBIDDEN, "Account is not active.");
        }
        return user;
    }
}
