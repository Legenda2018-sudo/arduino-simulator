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
        String name;
        String text;
        switch (componentType) {
            case "LED" -> {
                name = "Светодиод LED";
                text = "Как подключить\n+5V → резистор 220 Ом → LED → кнопка → GND.\n"
                    + "Или: правый контакт UNO → резистор → LED → кнопка → левый контакт UNO.\n\n"
                    + "Как работает\nЗапустите симуляцию и нажмите кнопку. При достаточном токе LED светится жёлтым. Порядок последовательных элементов можно менять.\n\n"
                    + "Если LED не горит\nПроверьте замкнутую цепь, кнопку и питание UNO. С программой UNO проверьте D13: HIGH включает цепь, LOW выключает. Код для работы без программы не нужен. Причина также показывается в строке статуса.\n\n"
                    + "Защита\nПерегрузка зависит от тока, а не количества лампочек. Без резистора даже один LED может перегореть и стать серым. Для 5 В используйте 220 Ом, для батарейки 9 В — например, 470 Ом. Слишком большое сопротивление даёт слабый ток.\n\n"
                    + "Ограничения учебной модели\nУ LED один визуальный контакт: подключайте два провода. Полярность не учитывается. Общие цепи нескольких LED пока не рассчитываются. После исправления цепи безопасный режим восстанавливает LED автоматически.";
            }
            case "Button" -> {
                name = "Кнопка";
                text = "Как работает\nКлик переключает кнопку: зелёная — нажата, цепь замкнута; отпущена — цепь разомкнута. Для проверки запустите симуляцию и выйдите из режимов размещения, соединения и удаления.\n\n"
                    + "Как подключить\n+5V → резистор 220 Ом → LED → кнопка → GND. Кнопку можно поставить в другое место этой последовательной цепи.\n\n"
                    + "Питание\nКнопка сама не вырабатывает ток. Нужны шины +5V/GND, батарейка или включённый UNO. Провод только от кнопки к LED не создаёт замкнутую цепь.\n\n"
                    + "Кнопка вместе с UNO\nВ цепи от шин или батарейки включённый UNO без программы работает как учебный ключ. С программой нужен HIGH на D13. Выключенный UNO или LOW размыкает цепь независимо от кнопки.\n\n"
                    + "Если LED не горит\nПроверьте строку статуса: там указаны выключенный UNO, отпущенная кнопка, разрыв цепи или перегрузка. Не обходите резистор дополнительным проводом. Не объединяйте несколько LED в одну общую ветвь: эта схема пока не поддерживается.";
            }
            case "Resistor" -> {
                name = "Резистор";
                text = "Ограничивает ток LED. Подключайте провода к разным ножкам. Два провода на одной ножке обходят сопротивление. Для 5 В подойдёт 220 Ом, для 9 В — например, 470 Ом. Двойной клик открывает настройку номинала. Перегрузка определяется расчётным током.";
            }
            case "ArduinoUNO" -> {
                name = "Arduino UNO";
                text = "Клик включает и выключает UNO, двойной клик открывает настройки. Без внешнего источника включённый UNO даёт учебные 5 В. С шинами или батарейкой работает как управляемый ключ между разными боковыми контактами. Без программы включённый ключ замкнут; с программой управляется D13 HIGH/LOW. Это учебное упрощение, не схема настоящего Arduino.";
            }
            case "Timer" -> {
                name = "Таймер";
                text = "Периодически замыкает и размыкает цепь. Сам источником питания не является. Включите таймер и запустите симуляцию. Двойной клик открывает настройку интервала. Для LED нужен резистор и замкнутый путь к источнику питания. При остановке симуляции таймер останавливается.";
            }
            default -> { name = componentType; text = "Выберите компонент и проверьте подсказки в строке статуса."; }
        }
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Справка: " + name);
        Label title = new Label(name);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");
        title.setWrapText(true);
        Label body = new Label(text);
        body.setWrapText(true);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setStyle("-fx-font-size: 14px; -fx-text-fill: #ecf0f1;");
        VBox content = new VBox(16, title, body);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #252a31;");
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #252a31; -fx-background-color: #252a31;");
        stage.setScene(new Scene(scroll, 500, 580));
        stage.setMinWidth(360);
        stage.setMinHeight(300);
        stage.show();
    }
}