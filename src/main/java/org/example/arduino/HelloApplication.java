package org.example.arduino;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    /** Панель ~90 + плата 750 + рамка и отступы ≈ 920 */
    private static final double PREFERRED_HEIGHT = 920;
    private static final double PREFERRED_WIDTH = 1280;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("arduino-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Симулятор Arduino");
        stage.setScene(scene);

        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        stage.setMinWidth(1180);
        stage.setMinHeight(860);

        double initialWidth = Math.min(PREFERRED_WIDTH, bounds.getWidth() - 12);
        double initialHeight = Math.min(PREFERRED_HEIGHT, bounds.getHeight() - 12);
        initialWidth = Math.max(stage.getMinWidth(), initialWidth);
        initialHeight = Math.max(stage.getMinHeight(), initialHeight);

        stage.setWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setX((bounds.getWidth() - initialWidth) / 2);
        stage.setY(Math.max(0, (bounds.getHeight() - initialHeight) / 2));

        stage.setFullScreen(false);
        stage.setMaximized(false);
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
