package com.example.signin;

public interface UserList {
    void add(User user);
    User getByEmail(String email);
    boolean existsByEmail(String email);
}

