module org.example.CristalixAccountChanger {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;

    opens org.example to javafx.fxml;
    exports org.example;
}