module com.cab302.cab302 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.cab302.cab302 to javafx.fxml;
    exports com.cab302.cab302;
    exports com.cab302.cab302.controller;
    opens com.cab302.cab302.controller to javafx.fxml;
}