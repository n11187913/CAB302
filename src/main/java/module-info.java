module com.cab302.cab302 {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // JDBC
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // Open packages for FXML reflection
    opens com.cab302.cab302 to javafx.fxml;
    opens com.cab302.cab302.controller to javafx.fxml;
    opens com.cab302.cab302.Database to javafx.fxml;

    // Exported packages
    exports com.cab302.cab302;
    exports com.cab302.cab302.controller;
    exports com.cab302.cab302.Database;
}
