package org.example.arduino;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("arduino-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Симулятор Arduino");
        stage.setScene(scene);
        
        // Получаем размеры экрана
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        
        // Устанавливаем разумные минимальные размеры (меньше для старых ноутбуков)
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        
        // Устанавливаем начальный размер окна (но не больше экрана)
        double initialWidth = Math.min(1400, bounds.getWidth() * 0.9);
        double initialHeight = Math.min(900, bounds.getHeight() * 0.9);
        stage.setWidth(initialWidth);
        stage.setHeight(initialHeight);
        
        // Центрируем окно на экране
        stage.setX((bounds.getWidth() - initialWidth) / 2);
        stage.setY((bounds.getHeight() - initialHeight) / 2);
        
        // ВАЖНО: Отключаем полноэкранный режим по умолчанию
        stage.setFullScreen(false);
        stage.setMaximized(false);
        
        // Разрешаем изменение размера окна
        stage.setResizable(true);
        
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}