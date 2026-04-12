package org.example.arduino;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.arduino.model.Timer;

public class TimerConfigWindow {
    
    public static void showConfig(Timer timer) {
        Stage configStage = new Stage();
        configStage.initModality(Modality.APPLICATION_MODAL);
        configStage.setTitle("Настройки таймера");
        
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(20));
        mainBox.setStyle("-fx-background-color: #f5f5f5;");
        
        // Заголовок
        Label titleLabel = new Label("⏱️ Настройка таймера");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Статус
        Label statusLabel = new Label();
        updateStatusLabel(statusLabel, timer);
        
        // Настройка интервала
        Label intervalLabel = new Label("Интервал мигания (миллисекунды):");
        intervalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Spinner<Integer> intervalSpinner = new Spinner<>(100, 10000, (int)timer.getInterval(), 100);
        intervalSpinner.setPrefWidth(200);
        intervalSpinner.setEditable(true);
        
        // Обновляем интервал таймера при изменении
        intervalSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal >= 100 && newVal <= 10000) {
                timer.setInterval(newVal);
                updateStatusLabel(statusLabel, timer);
            }
        });
        
        // Описание
        Label descriptionLabel = new Label(
            "Интервал определяет, как часто таймер переключает сигнал.\n" +
            "• 100-500 мс - быстрое мигание\n" +
            "• 500-2000 мс - нормальное мигание\n" +
            "• 2000+ мс - медленное мигание"
        );
        descriptionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
        descriptionLabel.setWrapText(true);
        
        // Кнопки управления
        Button startButton = new Button("▶️ Старт");
        startButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        Button stopButton = new Button("⏹️ Стоп");
        stopButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        startButton.setOnAction(e -> {
            timer.setActive(true);
            timer.start();
            updateStatusLabel(statusLabel, timer);
        });
        
        stopButton.setOnAction(e -> {
            timer.setActive(false);
            timer.stop();
            updateStatusLabel(statusLabel, timer);
        });
        
        // Предустановленные интервалы
        Label presetsLabel = new Label("Быстрые настройки:");
        presetsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        
        Button fastButton = new Button("Быстро (200 мс)");
        fastButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        fastButton.setOnAction(e -> {
            intervalSpinner.getValueFactory().setValue(200);
            timer.setInterval(200);
            updateStatusLabel(statusLabel, timer);
        });
        
        Button normalButton = new Button("Нормально (1000 мс)");
        normalButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        normalButton.setOnAction(e -> {
            intervalSpinner.getValueFactory().setValue(1000);
            timer.setInterval(1000);
            updateStatusLabel(statusLabel, timer);
        });
        
        Button slowButton = new Button("Медленно (3000 мс)");
        slowButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white;");
        slowButton.setOnAction(e -> {
            intervalSpinner.getValueFactory().setValue(3000);
            timer.setInterval(3000);
            updateStatusLabel(statusLabel, timer);
        });
        
        HBox presetBox = new HBox(10);
        presetBox.getChildren().addAll(fastButton, normalButton, slowButton);
        
        HBox controlBox = new HBox(10);
        controlBox.getChildren().addAll(startButton, stopButton);
        
        // Кнопка закрытия
        Button closeButton = new Button("Закрыть");
        closeButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px;");
        closeButton.setOnAction(e -> configStage.close());
        
        mainBox.getChildren().addAll(
            titleLabel,
            statusLabel,
            new Separator(),
            intervalLabel,
            intervalSpinner,
            descriptionLabel,
            new Separator(),
            presetsLabel,
            presetBox,
            new Separator(),
            controlBox,
            closeButton
        );
        
        Scene scene = new Scene(mainBox, 400, 500);
        configStage.setScene(scene);
        configStage.setResizable(false);
        configStage.show();
    }
    
    private static void updateStatusLabel(Label label, Timer timer) {
        String status;
        String color;
        
        if (!timer.isActive()) {
            status = "Статус: ВЫКЛ";
            color = "#e74c3c";
        } else if (!timer.isRunning()) {
            status = "Статус: ОСТАНОВЛЕН";
            color = "#f39c12";
        } else {
            status = "Статус: РАБОТАЕТ | Интервал: " + timer.getInterval() + " мс | Выход: " + 
                    (timer.getOutputState() ? "ВКЛ" : "ВЫКЛ");
            color = "#27ae60";
        }
        
        label.setText(status);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }
}
