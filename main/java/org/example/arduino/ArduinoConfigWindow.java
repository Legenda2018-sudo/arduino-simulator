package org.example.arduino;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.arduino.model.ArduinoUNO;
import org.example.arduino.model.Component;
import org.example.arduino.model.Wire;

import java.util.List;

public class ArduinoConfigWindow {
    
    public static void showConfig(ArduinoUNO arduino, List<Component> allComponents, List<Wire> allWires) {
        Stage configStage = new Stage();
        configStage.initModality(Modality.APPLICATION_MODAL);
        configStage.setTitle("Настройки Arduino UNO");
        
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(20));
        mainBox.setStyle("-fx-background-color: #f5f5f5;");
        
        // Статус питания
        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        if (arduino.isPowered()) {
            statusLabel.setText("🔷 Arduino UNO: ВКЛ");
            statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        } else {
            statusLabel.setText("🔷 Arduino UNO: ВЫКЛ");
            statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }
        
        // Кнопка включения/выключения
        Button toggleButton = new Button(arduino.isPowered() ? "Выключить" : "Включить");
        toggleButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 14px;");
        toggleButton.setOnAction(e -> {
            arduino.toggle();
            if (arduino.isPowered()) {
                statusLabel.setText("🔷 Arduino UNO: ВКЛ");
                statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
                toggleButton.setText("Выключить");
            } else {
                statusLabel.setText("🔷 Arduino UNO: ВЫКЛ");
                statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                toggleButton.setText("Включить");
            }
        });
        
        // Список подключенных компонентов (входы)
        Label inputsLabel = new Label("📥 Входные сигналы (подключено К Arduino):");
        inputsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        
        ObservableList<String> inputComponents = FXCollections.observableArrayList();
        
        // Находим компоненты, подключенные К Arduino (входы)
        for (Wire wire : allWires) {
            if (wire.getTo() == arduino) {
                Component from = wire.getFrom();
                String componentInfo = getComponentInfo(from);
                if (!inputComponents.contains(componentInfo)) {
                    inputComponents.add(componentInfo);
                }
            }
        }
        
        // Прямые соединения (входы)
        for (Component connected : arduino.getConnections()) {
            String componentInfo = getComponentInfo(connected);
            if (!inputComponents.contains(componentInfo)) {
                inputComponents.add(componentInfo);
            }
        }
        
        if (inputComponents.isEmpty()) {
            inputComponents.add("Нет подключенных компонентов");
        }
        
        ListView<String> inputsList = new ListView<>(inputComponents);
        inputsList.setPrefHeight(150);
        inputsList.setStyle("-fx-background-color: white;");
        
        // Список подключенных компонентов (выходы)
        Label outputsLabel = new Label("📤 Выходные сигналы (подключено ОТ Arduino):");
        outputsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        
        ObservableList<String> outputComponents = FXCollections.observableArrayList();
        
        // Находим компоненты, подключенные ОТ Arduino (выходы)
        for (Wire wire : allWires) {
            if (wire.getFrom() == arduino) {
                Component to = wire.getTo();
                String componentInfo = getComponentInfo(to);
                if (!outputComponents.contains(componentInfo)) {
                    outputComponents.add(componentInfo);
                }
            }
        }
        
        // Прямые соединения (выходы)
        for (Component connected : arduino.getConnections()) {
            // Проверяем, есть ли провод от Arduino к этому компоненту
            boolean isOutput = allWires.stream().anyMatch(w -> w.getFrom() == arduino && w.getTo() == connected);
            if (isOutput) {
                String componentInfo = getComponentInfo(connected);
                if (!outputComponents.contains(componentInfo)) {
                    outputComponents.add(componentInfo);
                }
            }
        }
        
        if (outputComponents.isEmpty()) {
            outputComponents.add("Нет подключенных компонентов");
        }
        
        ListView<String> outputsList = new ListView<>(outputComponents);
        outputsList.setPrefHeight(150);
        outputsList.setStyle("-fx-background-color: white;");
        
        // Информация
        Label infoLabel = new Label(
            "ℹ️ Arduino UNO передает сигналы от входных компонентов к выходным.\n" +
            "Для подключения используйте режим 'Соединить'."
        );
        infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
        infoLabel.setWrapText(true);
        
        // Кнопка закрытия
        Button closeButton = new Button("Закрыть");
        closeButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 14px;");
        closeButton.setOnAction(e -> configStage.close());
        
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(toggleButton, closeButton);
        
        mainBox.getChildren().addAll(
            statusLabel,
            buttonBox,
            inputsLabel,
            inputsList,
            outputsLabel,
            outputsList,
            infoLabel
        );
        
        Scene scene = new Scene(mainBox, 500, 600);
        configStage.setScene(scene);
        configStage.setResizable(false);
        configStage.show();
    }
    
    private static String getComponentInfo(Component comp) {
        if (comp == null) return "Неизвестный компонент";
        
        String type = comp.getType();
        String location = String.format("(%.0f, %.0f)", comp.getX(), comp.getY());
        
        if (comp instanceof org.example.arduino.model.Button) {
            org.example.arduino.model.Button button = (org.example.arduino.model.Button) comp;
            return "🔘 " + type + " " + location + " - " + (button.isPressed() ? "НАЖАТА" : "отпущена");
        } else if (comp instanceof org.example.arduino.model.LED) {
            org.example.arduino.model.LED led = (org.example.arduino.model.LED) comp;
            String state = led.isBurned() ? "ПЕРЕГОРЕЛ" : (led.isOn() ? "горит" : "не горит");
            return "💡 " + type + " " + location + " - " + state;
        } else if (comp instanceof org.example.arduino.model.Timer) {
            org.example.arduino.model.Timer timer = (org.example.arduino.model.Timer) comp;
            String state = timer.isActive() ? "ВКЛ" : "ВЫКЛ";
            return "⏱️ " + type + " " + location + " - " + state + " (интервал: " + timer.getInterval() + " мс)";
        } else if (comp instanceof org.example.arduino.model.Resistor) {
            org.example.arduino.model.Resistor resistor = (org.example.arduino.model.Resistor) comp;
            return "⚡ " + type + " " + location + " - " + String.format("%.0f Ом", resistor.getResistance());
        }
        
        return "📦 " + type + " " + location;
    }
}
