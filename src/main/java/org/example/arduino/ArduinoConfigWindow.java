package org.example.arduino;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.arduino.model.ArduinoUNO;
import org.example.arduino.model.Component;
import org.example.arduino.model.LED;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.Timer;
import org.example.arduino.model.Wire;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Окно настройки Arduino UNO.
 */
public final class ArduinoConfigWindow {

    private static final double WINDOW_WIDTH = 500;
    private static final double CONTENT_WIDTH = WINDOW_WIDTH - 48;

    private ArduinoConfigWindow() {
    }

    public static void showConfig(ArduinoUNO arduino, List<Component> allComponents, List<Wire> allWires) {
        if (arduino == null) {
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Настройка Arduino UNO");

        VBox root = new VBox(14);
        root.setPadding(new Insets(22, 24, 24, 24));
        root.setFillWidth(true);
        root.setStyle("-fx-background-color: #252a31;");

        Label title = new Label("Arduino UNO");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Label subtitle = wrapLabel(
            "Плата передаёт сигнал от входных компонентов к выходным. "
                + "Включите питание и проверьте подключения проводами (режим «Соединить»).",
            "-fx-font-size: 12px; -fx-text-fill: #95a5a6;"
        );

        VBox previewBox = new VBox(12);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setPadding(new Insets(16, 14, 16, 14));
        previewBox.setMaxWidth(CONTENT_WIDTH);
        previewBox.setStyle("-fx-background-color: #1e2228; -fx-background-radius: 8;");

        Label boardLabel = new Label("UNO");
        boardLabel.setStyle(
            "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #00BCD4;"
                + " -fx-background-color: #00979D; -fx-background-radius: 8; -fx-padding: 12 36;"
        );

        Label powerLabel = new Label();
        powerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        Label signalLabel = new Label();
        signalLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #bdc3c7;");

        previewBox.getChildren().addAll(boardLabel, powerLabel, signalLabel);

        Button toggleButton = new Button();
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        toggleButton.setMinHeight(36);

        GridPane stats = new GridPane();
        stats.setHgap(10);
        stats.setVgap(10);
        ColumnConstraints halfCol = new ColumnConstraints();
        halfCol.setPercentWidth(50);
        halfCol.setHgrow(Priority.ALWAYS);
        stats.getColumnConstraints().addAll(halfCol, halfCol);

        VBox inputsCard = createStatCard("Входы", "0", "#3498DB");
        VBox outputsCard = createStatCard("Выходы", "0", "#9B59B6");
        Label inputsCount = valueLabelOf(inputsCard);
        Label outputsCount = valueLabelOf(outputsCard);
        stats.add(inputsCard, 0, 0);
        stats.add(outputsCard, 1, 0);

        Label inputsCaption = sectionCaption("Компоненты на входе");
        ListView<String> inputsList = createConnectionList();

        Label outputsCaption = sectionCaption("Компоненты на выходе");
        ListView<String> outputsList = createConnectionList();

        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(10, 12, 10, 12));
        statusRow.setMaxWidth(CONTENT_WIDTH);
        Region statusDot = new Region();
        statusDot.setMinSize(10, 10);
        statusDot.setMaxSize(10, 10);
        Label statusHint = wrapLabel("", "-fx-text-fill: #ecf0f1; -fx-font-size: 13px;");
        HBox.setHgrow(statusHint, Priority.ALWAYS);
        statusRow.getChildren().addAll(statusDot, statusHint);

        Button close = new Button("Готово");
        close.setMaxWidth(Double.MAX_VALUE);
        close.setMinHeight(36);
        close.setStyle(
            "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-background-radius: 6; -fx-padding: 10 16; -fx-font-size: 13px;"
        );
        close.setOnAction(e -> stage.close());

        Runnable refresh = () -> {
            boolean on = arduino.isPowered();
            powerLabel.setText(on ? "Питание: ВКЛ" : "Питание: ВЫКЛ");
            powerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + (on ? "#27AE60" : "#E74C3C") + ";");
            signalLabel.setText(arduino.getStatusText());

            toggleButton.setText(on ? "Выключить плату" : "Включить плату");
            toggleButton.setStyle(on ? actionStyle("#E74C3C") : actionStyle("#27AE60"));

            List<String> inputs = collectInputs(arduino, allWires);
            List<String> outputs = collectOutputs(arduino, allWires);
            inputsList.setItems(FXCollections.observableArrayList(inputs));
            outputsList.setItems(FXCollections.observableArrayList(outputs));
            inputsCount.setText(String.valueOf(inputs.size()));
            outputsCount.setText(String.valueOf(outputs.size()));

            String accent = on ? "#27AE60" : "#E74C3C";
            statusRow.setStyle(
                "-fx-background-color: " + accent + "22;"
                    + " -fx-background-radius: 8;"
                    + " -fx-border-color: " + accent + ";"
                    + " -fx-border-radius: 8;"
                    + " -fx-border-width: 1.5;"
            );
            statusDot.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 5;");
            if (on && outputs.isEmpty()) {
                statusHint.setText("Плата включена, но ни один компонент не подключён к выходу. Соедините UNO с кнопкой, таймером или LED.");
            } else if (!on) {
                statusHint.setText("Питание выключено — цепь не работает. Нажмите «Включить плату» или кликните по UNO на макетной плате.");
            } else {
                statusHint.setText("Плата готова. Запустите «Симуляцию» и проверьте сигнал на выходных компонентах.");
            }
        };

        toggleButton.setOnAction(e -> {
            arduino.toggle();
            refresh.run();
        });

        root.getChildren().addAll(
            title,
            subtitle,
            previewBox,
            toggleButton,
            stats,
            inputsCaption,
            inputsList,
            outputsCaption,
            outputsList,
            statusRow,
            close
        );

        refresh.run();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: #252a31; -fx-background: #252a31;");

        stage.setScene(new Scene(scroll, WINDOW_WIDTH, 640));
        stage.setMinWidth(460);
        stage.setMinHeight(480);
        stage.setResizable(true);
        stage.showAndWait();
    }

    private static List<String> collectInputs(ArduinoUNO arduino, List<Wire> wires) {
        Set<String> items = new LinkedHashSet<>();
        for (Wire wire : wires) {
            if (wire.getTo() == arduino) {
                items.add(describeComponent(wire.getFrom()));
            }
        }
        for (Component connected : arduino.getConnections()) {
            items.add(describeComponent(connected));
        }
        if (items.isEmpty()) {
            items.add("Ничего не подключено");
        }
        return new ArrayList<>(items);
    }

    private static List<String> collectOutputs(ArduinoUNO arduino, List<Wire> wires) {
        Set<String> items = new LinkedHashSet<>();
        for (Wire wire : wires) {
            if (wire.getFrom() == arduino) {
                items.add(describeComponent(wire.getTo()));
            }
        }
        for (Component connected : arduino.getConnections()) {
            boolean isOutput = wires.stream().anyMatch(w -> w.getFrom() == arduino && w.getTo() == connected);
            if (isOutput) {
                items.add(describeComponent(connected));
            }
        }
        if (items.isEmpty()) {
            items.add("Ничего не подключено");
        }
        return new ArrayList<>(items);
    }

    private static String describeComponent(Component comp) {
        if (comp == null) {
            return "Неизвестный компонент";
        }
        String type = comp.getType();
        if (comp instanceof org.example.arduino.model.Button button) {
            return type + " — " + (button.isPressed() ? "нажата" : "отпущена");
        }
        if (comp instanceof LED led) {
            String state = led.isBurned() ? "перегорел" : (led.isOn() ? "горит" : "не горит");
            return type + " — " + state;
        }
        if (comp instanceof Timer timer) {
            return type + " — " + (timer.isActive() ? "вкл" : "выкл") + ", " + timer.getInterval() + " мс";
        }
        if (comp instanceof Resistor resistor) {
            return type + " — " + String.format("%.0f Ом", resistor.getResistance());
        }
        return type;
    }

    private static ListView<String> createConnectionList() {
        ListView<String> list = new ListView<>();
        list.setPrefHeight(110);
        list.setMaxHeight(130);
        list.setStyle(
            "-fx-background-color: #1e2228; -fx-control-inner-background: #1e2228;"
                + " -fx-background-radius: 8; -fx-border-color: #3d4654; -fx-border-radius: 8;"
        );
        return list;
    }

    private static Label sectionCaption(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #bdc3c7;");
        return label;
    }

    private static Label valueLabelOf(VBox card) {
        return (Label) card.getChildren().get(1);
    }

    private static VBox createStatCard(String caption, String value, String accent) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);
        card.setStyle(
            "-fx-background-color: #1e2228; -fx-background-radius: 8;"
                + " -fx-border-color: " + accent + "55; -fx-border-radius: 8; -fx-border-width: 1;"
        );
        Label cap = new Label(caption);
        cap.setWrapText(true);
        cap.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
        Label val = new Label(value);
        val.setWrapText(true);
        val.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");
        card.getChildren().addAll(cap, val);
        return card;
    }

    private static Label wrapLabel(String text, String style) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(CONTENT_WIDTH);
        label.setStyle(style);
        return label;
    }

    private static String actionStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold;"
            + " -fx-background-radius: 6; -fx-padding: 10 16; -fx-font-size: 13px;";
    }
}
