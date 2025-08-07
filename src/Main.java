import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    private static ArrayList<User> users = new ArrayList<>();
    private static IAuthenticationService authService = new AuthenticationService(users);
    private static boolean isRunning = true;

    public static void main(String[] args) {
        users.add(new User("test", "test")); // Default user

        while (isRunning) {
            showMenu();
        }
    }

    public static void showMenu() {
        System.out.println("Welcome to the To-Do List Application!");
        System.out.println("1. Log in");
        System.out.println("2. Sign up");
        System.out.println("3. Exit");
        System.out.print("Enter your choice: ");

        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline
        handleMenu(choice);
    }

    public static void handleMenu(int choice) {
        switch (choice) {
            case 1 -> onLogIn();
            case 2 -> onSignUp();
            case 3 -> onExit();
            default -> System.out.println("Invalid choice!");
        }
    }

    public static void onLogIn() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        User user = authService.logIn(username, password);

        if (user != null) {
            System.out.println("Welcome, " + user.getUsername() + "!");
            ToDoList list = new ToDoList(user);
            list.run();
        } else {
            System.out.println("Login failed. Please try again.");
        }
    }

    public static void onSignUp() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        User user = authService.signUp(username, password);

        if (user != null) {
            System.out.println("User " + user.getUsername() + " has been created successfully!");
        } else {
            System.out.println("The username is already taken!");
        }
    }

    public static void onExit() {
        System.out.println("Goodbye!");
        isRunning = false;
    }
}
