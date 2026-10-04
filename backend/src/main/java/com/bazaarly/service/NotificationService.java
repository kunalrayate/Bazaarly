package com.bazaarly.service;

import com.bazaarly.entity.*;
import com.bazaarly.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepo repo;
    private final UserRepo users;

    public void notify(User u, String title, String message) {
        Notification n = new Notification(); n.setUser(u); n.setTitle(title); n.setMessage(message); repo.save(n);
    }
    public void notifyAdmins(String title, String message) {
        users.findByRole(Enums.Role.ADMIN).forEach(a -> notify(a, title, message));
    }
}
