module com.cab302.cab302 {
    requires javafx.controls;
    requires javafx.fxml;

    // allow FXML and JavaFX to access classes in your signin package
    opens com.example.signin to javafx.fxml, javafx.graphics;
    exports com.example.signin;
}
