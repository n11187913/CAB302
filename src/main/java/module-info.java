module com.cab302.cab302 {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // JDBC
    requires java.sql;
    requires org.xerial.sqlitejdbc;

 page-template-and-navigation-bar
=======
    // Open packages for FXML reflection
    opens com.cab302.cab302 to javafx.fxml;
main
    opens com.cab302.cab302.controller to javafx.fxml;
    opens com.cab302.cab302.Database to javafx.fxml;

page-template-and-navigation-bar
    opens com.cab302.cab302 to javafx.graphics, javafx.fxml;
=======
    // Exported packages
 main
    exports com.cab302.cab302;
    exports com.cab302.cab302.controller;
    exports com.cab302.cab302.Database;
}
