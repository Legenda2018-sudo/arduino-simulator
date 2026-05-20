module org.example.arduino {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires transitive okhttp3;
    requires transitive com.google.gson;

    opens org.example.arduino to javafx.fxml;
    opens org.example.arduino.model to com.google.gson;
    opens org.example.arduino.service to com.google.gson;

    exports org.example.arduino;
    exports org.example.arduino.model;
    exports org.example.arduino.service;
    exports org.example.arduino.util;
}