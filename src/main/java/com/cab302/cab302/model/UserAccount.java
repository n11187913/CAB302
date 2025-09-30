package com.cab302.cab302.model;

/**
 * A model class representing a user account with an ID, first name,
 * last name, and email address.
 */
public class UserAccount {
    private long id;
    private String firstName, lastName, email;

    /**
     * Constructs a new UserAccount with the specified first name, last name, and email.
     *
     * @param firstName The first name of the user
     * @param lastName  The last name of the user
     * @param email     The email of the user
     */
    public UserAccount(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.email     = email;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

