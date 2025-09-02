module com.cab302.cab302 {
    requires javafx.controls;
    requires javafx.fxml;

    // allow FXML and JavaFX to access classes in your signin package
    opens com.cab302.cab302.controller to javafx.fxml, javafx.graphics;
    exports com.cab302.cab302;
    opens com.cab302.cab302 to javafx.fxml, javafx.graphics;
}
