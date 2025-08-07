import java.util.ArrayList;

public class User {
    private String username;
    private String password;
    private ArrayList<ToDoItem> toDoItems;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.toDoItems = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ArrayList<ToDoItem> getToDoItems() {
        return toDoItems;
    }

    public void addToDoItem(ToDoItem item) {
        toDoItems.add(item);
    }

    public boolean removeToDoItem(int index) {
        if (index >= 0 && index < toDoItems.size()) {
            toDoItems.remove(index);
            return true;
        }
        return false;
    }
}
