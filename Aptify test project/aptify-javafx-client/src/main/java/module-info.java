module com.example.aptifyjavafxclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens com.example.aptifyjavafxclient to javafx.fxml;
    opens com.example.aptifyjavafxclient.model to com.fasterxml.jackson.databind;
    exports com.example.aptifyjavafxclient;
}