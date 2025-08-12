module com.cab302.cab302 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.cab302.cab302 to javafx.fxml;
    exports com.cab302.cab302;
}