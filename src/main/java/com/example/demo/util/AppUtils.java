package com.example.demo.util;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.demo.entity.User;

public class AppUtils {

    public static User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}