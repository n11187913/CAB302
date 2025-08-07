public class ToDoItem {
    private String description;
    private boolean done;

    public ToDoItem(String description) {
        this.description = description;
        this.done = false;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public String toString() {
        return (done ? "[x] " : "[ ] ") + description;
    }
}
