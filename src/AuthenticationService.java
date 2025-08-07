import java.util.ArrayList;

public class AuthenticationService implements IAuthenticationService {
    private ArrayList<User> users;

    public AuthenticationService(ArrayList<User> users) {
        this.users = users;
    }

    @Override
    public User signUp(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return null; // Username already taken
            }
        }

        User newUser = new User(username, password);
        users.add(newUser);
        return newUser;
    }

    @Override
    public User logIn(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null; // Login failed
    }
}
