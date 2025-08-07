import java.util.ArrayList;
import java.util.Scanner;

public class ToDoList {
    private User user;
    private Scanner scanner;

    // ✅ Constructor that accepts a User
    public ToDoList(User user) {
        this.user = user;
        this.scanner = new Scanner(System.in);
    }

    // ✅ Method called from Main.java
    public void run() {
        boolean running = true;
        while (running) {
            System.out.println("\n== To-Do List Menu ==");
            System.out.println("1. View to-do list");
            System.out.println("2. Add new item");
            System.out.println("3. Remove item");
            System.out.println("4. Mark item as done");
            System.out.println("5. Mark item as not done");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1:
                    displayItems();
                    break;

                case 2:
                    System.out.print("Enter a new to-do item: ");
                    String desc = scanner.nextLine();
                    user.addToDoItem(new ToDoItem(desc));
                    System.out.println("Item added.");
                    break;

                case 3:
                    System.out.print("Enter the number of the item to remove: ");
                    int removeIndex = Integer.parseInt(scanner.nextLine()) - 1;
                    if (user.removeToDoItem(removeIndex)) {
                        System.out.println("Item removed.");
                    } else {
                        System.out.println("Invalid item number.");
                    }
                    break;

                case 4:
                    System.out.print("Enter the number of the item to mark as done: ");
                    int doneIndex = Integer.parseInt(scanner.nextLine()) - 1;
                    markItem(doneIndex, true);
                    break;

                case 5:
                    System.out.print("Enter the number of the item to mark as not done: ");
                    int undoneIndex = Integer.parseInt(scanner.nextLine()) - 1;
                    markItem(undoneIndex, false);
                    break;

                case 6:
                    running = false;
                    System.out.println("Goodbye!");
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void displayItems() {
        System.out.println("\n== Your To-Do List ==");
        ArrayList<ToDoItem> items = user.getToDoItems();
        if (items.isEmpty()) {
            System.out.println("Your to-do list is empty.");
        } else {
            for (int i = 0; i < items.size(); i++) {
                System.out.println((i + 1) + ". " + items.get(i));
            }
        }
    }

    private void markItem(int index, boolean done) {
        ArrayList<ToDoItem> items = user.getToDoItems();
        if (index >= 0 && index < items.size()) {
            items.get(index).setDone(done);
            System.out.println("Item marked as " + (done ? "done." : "not done."));
        } else {
            System.out.println("Invalid item number.");
        }
    }
}
