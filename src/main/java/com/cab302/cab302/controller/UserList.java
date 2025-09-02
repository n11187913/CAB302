package com.cab302.cab302.controller;

import com.cab302.cab302.model.UserAccount;

public interface UserList {
    void add(UserAccount user);
    UserAccount getByEmail(String email);
    boolean existsByEmail(String email);
}

