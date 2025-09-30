module com.cab302.cab302 {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;


    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires javafx.graphics;
    requires java.desktop;

    requires java.net.http;
    requires org.json;

    opens com.cab302.cab302 to javafx.fxml;
    opens com.cab302.cab302.controller to javafx.fxml;
    opens com.cab302.cab302.Database to javafx.fxml;


    exports com.cab302.cab302;
    exports com.cab302.cab302.controller;
    exports com.cab302.cab302.Database;
}
