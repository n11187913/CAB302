module com.cab302.cab302 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.cab302.cab302 to javafx.fxml;
    opens com.cab302.cab302.controller to javafx.fxml;
    opens com.cab302.cab302.Database to javafx.fxml;

    // add this line if controllers live in `ui`
    opens ui to javafx.fxml;

    exports com.cab302.cab302;
    exports com.cab302.cab302.controller;
    exports com.cab302.cab302.Database;
}
