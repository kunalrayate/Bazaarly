package com.bazaarly.config;

import com.bazaarly.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Auth {
    private Auth() {}
    /** Logged-in user or null (public endpoints). */
    public static User current() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof User u ? u : null;
    }
    public static User require() {
        User u = current();
        if (u == null) throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Please log in");
        return u;
    }
}
