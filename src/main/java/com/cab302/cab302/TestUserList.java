package com.cab302.cab302;

import com.cab302.cab302.controller.UserList;
import com.cab302.cab302.model.UserAccount;

import java.util.ArrayList;
import java.util.List;

public class TestUserList implements UserList {
    private static final List<UserAccount> users = new ArrayList<>();
    private static int nextId = 1;

    public TestUserList() {
        if (users.isEmpty()) {
            // this is demo user but can log in with anything as long as you sign up with same credentials
            UserAccount u = new UserAccount("Nick", "Tesch", "NickTesch@example.com", "1234");
            add(u);
        }
    }

    //potentially have to add database here to store users so that doesnt rest after closing the application and can instatly log in
    @Override
    public void add(UserAccount user) {
        user.setId(nextId++);
        users.add(user);
    }

    @Override
    public UserAccount getByEmail(String email) {
        if (email == null) return null;
        return users.stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst().orElse(null);
    }

    @Override
    public boolean existsByEmail(String email) {
        return getByEmail(email) != null;
    }
}
