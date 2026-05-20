package org.example.arduino;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class HelpWindow {
    
    public static void showHelp(String componentType) {
        Stage helpStage = new Stage();
        helpStage.initModality(Modality.APPLICATION_MODAL);
        helpStage.setTitle("Справка: " + componentType);
        
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #f5f5f5;");
        
        ScrollPane scrollPane = new ScrollPane(vbox);
        scrollPane.setFitToWidth(true);
        
        Label title = new Label("📖 " + componentType);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label description = new Label();
        Label usage = new Label();
        Label warning = new Label();
        
        switch (componentType) {
            case "LED":
                description.setText("💡 Светодиод (LED) - источник света в схеме.");
                description.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");
                
                usage.setText("Использование:\n" +
                    "• Подключается к кнопке через провода\n" +
                    "• Загорается желтым при подаче сигнала\n" +
                    "• БЕЗ РЕЗИСТОРА может перегореть!\n" +
                    "• Максимум 2-3 LED на одну кнопку без резистора");
                usage.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
                
                warning.setText("⚠️ ВНИМАНИЕ:\n" +
                    "• Без резистора LED перегорит при подключении более 2-3 лампочек\n" +
                    "• Перегоревший LED становится серым и не работает\n" +
                    "• Всегда используйте резистор для защиты!");
                warning.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-wrap-text: true;");
                break;
                
            case "Button":
                description.setText("🔘 Кнопка - управляет подачей тока в схему.");
                description.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");
                
                usage.setText("Использование:\n" +
                    "• Кликните по кнопке для нажатия/отпускания\n" +
                    "• При нажатии становится зеленой\n" +
                    "• Передает сигнал подключенным компонентам\n" +
                    "• Может управлять несколькими компонентами одновременно");
                usage.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
                
                warning.setText("💡 СОВЕТ:\n" +
                    "• Одна кнопка может управлять несколькими LED\n" +
                    "• Но без резистора слишком много LED перегорят!");
                warning.setStyle("-fx-font-size: 12px; -fx-text-fill: #3498db; -fx-wrap-text: true;");
                break;
                
            case "Resistor":
                description.setText("⚡ Резистор - ограничивает ток в цепи.");
                description.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");
                
                usage.setText("Использование:\n" +
                    "• Ставьте между кнопкой и LED\n" +
                    "• Защищает LED от перегрузки\n" +
                    "• Сопротивление: 1000 Ом (по умолчанию)\n" +
                    "• Пропускает сигнал, но ограничивает ток");
                usage.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
                
                warning.setText("✅ ЗАЩИТА:\n" +
                    "• С резистором можно подключить много LED\n" +
                    "• LED не перегорят при правильном использовании\n" +
                    "• Всегда используйте резистор в реальных схемах!");
                warning.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-wrap-text: true;");
                break;
                
            case "ArduinoUNO":
                description.setText("🔷 Arduino UNO - микроконтроллер, передает сигналы между компонентами.");
                description.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");
                
                usage.setText("Использование:\n" +
                    "• Кликните по Arduino для включения/выключения\n" +
                    "• При включении становится голубым (циан)\n" +
                    "• Можно подключить провода с любой стороны\n" +
                    "• Входные сигналы → Arduino → Выходные сигналы\n" +
                    "• Работает как промежуточный компонент в цепи");
                usage.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
                
                warning.setText("💡 КАК РАБОТАЕТ:\n" +
                    "• Подключите кнопку или таймер К Arduino (это вход)\n" +
                    "• Подключите LED ОТ Arduino (это выход)\n" +
                    "• Когда Arduino включена, она передает сигнал от входа к выходу\n" +
                    "• Без питания Arduino не работает - сигнал не проходит");
                warning.setStyle("-fx-font-size: 12px; -fx-text-fill: #3498db; -fx-wrap-text: true;");
                break;
                
            case "Timer":
                description.setText("⏱️ Таймер - генерирует периодические сигналы.");
                description.setStyle("-fx-font-size: 14px; -fx-text-fill: #34495e;");
                
                usage.setText("Использование:\n" +
                    "• Кликните по таймеру для включения/выключения\n" +
                    "• При включении становится голубым при активности\n" +
                    "• Генерирует периодические сигналы\n" +
                    "• Интервал по умолчанию: 1000 мс (1 секунда)\n" +
                    "• Подключайте к LED для мигания");
                usage.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-wrap-text: true;");
                
                warning.setText("🔄 РАБОТА:\n" +
                    "• Таймер запускается автоматически при симуляции\n" +
                    "• Используйте для создания мигающих LED\n" +
                    "• Можно подключить к Arduino для синхронизации\n" +
                    "• Останавливается при остановке симуляции");
                warning.setStyle("-fx-font-size: 12px; -fx-text-fill: #9b59b6; -fx-wrap-text: true;");
                break;
        }
        
        vbox.getChildren().addAll(title, description, usage, warning);
        
        Scene scene = new Scene(scrollPane, 400, 300);
        helpStage.setScene(scene);
        helpStage.show();
    }
}

