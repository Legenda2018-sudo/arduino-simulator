package org.example.arduino;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.arduino.model.Battery;
import org.example.arduino.util.CircuitPhysics;

/**
 * Окно настройки напряжения батарейки (как номинал у резистора).
 */
public final class BatteryConfigWindow {

    private static final double WINDOW_WIDTH = 460;
    private static final double CONTENT_WIDTH = WINDOW_WIDTH - 48;

    private BatteryConfigWindow() {
    }

    public static void showConfig(Battery battery) {
        if (battery == null) {
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Настройка батарейки");

        VBox root = new VBox(14);
        root.setPadding(new Insets(22, 24, 24, 24));
        root.setFillWidth(true);
        root.setStyle("-fx-background-color: #252a31;");

        Label title = new Label("Батарейка");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Label subtitle = wrapLabel(
            "Задайте напряжение источника U. В формуле I = (U − V_LED) / R используется это значение.",
            "-fx-font-size: 12px; -fx-text-fill: #95a5a6;"
        );

        Label valueLabel = new Label();
        valueLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2ECC71;");

        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory =
            new SpinnerValueFactory.DoubleSpinnerValueFactory(1.5, 12.0, battery.getVoltage(), 0.5);
        Spinner<Double> voltsSpinner = new Spinner<>(valueFactory);
        voltsSpinner.setEditable(true);
        voltsSpinner.setMaxWidth(Double.MAX_VALUE);
        voltsSpinner.setStyle(
            "-fx-font-size: 16px; -fx-background-color: #1e2228; -fx-text-fill: #ecf0f1;"
        );

        GridPane presets = new GridPane();
        presets.setHgap(8);
        presets.setVgap(8);
        ColumnConstraints presetCol = new ColumnConstraints();
        presetCol.setPercentWidth(25);
        presetCol.setHgrow(Priority.ALWAYS);
        presets.getColumnConstraints().addAll(presetCol, presetCol, presetCol, presetCol);

        double[] presetValues = { 1.5, 3.0, 9.0, 12.0 };
        Button[] presetButtons = new Button[presetValues.length];
        for (int i = 0; i < presetValues.length; i++) {
            double preset = presetValues[i];
            Button b = new Button(formatVoltage(preset));
            b.setMaxWidth(Double.MAX_VALUE);
            b.setMinHeight(34);
            presetButtons[i] = b;
            presets.add(b, i, 0);
        }

        VBox formulaCard = createStatCard(
            "Закон Ома для LED",
            "I = (U − V_LED) / R",
            "#3498DB"
        );
        Label formulaValue = valueLabelOf(formulaCard);

        Runnable refresh = () -> {
            double u = voltsSpinner.getValue();
            valueLabel.setText(formatVoltage(u));
            if (u <= CircuitPhysics.LED_VF) {
                formulaValue.setText(String.format(
                    "U (%.1f В) ≤ V_LED (%.1f В) — LED не светится, I ≈ 0 мА",
                    u, CircuitPhysics.LED_VF
                ));
            } else {
                double i220 = CircuitPhysics.currentMilliAmps(u, 220, CircuitPhysics.LED_VF);
                formulaValue.setText(String.format(
                    "При R=220 Ом: I = (%.1f − %.1f) / 220 ≈ %.1f мА",
                    u, CircuitPhysics.LED_VF, i220
                ));
            }
            for (int i = 0; i < presetButtons.length; i++) {
                presetButtons[i].setStyle(
                    Math.abs(u - presetValues[i]) < 0.05 ? presetStyleActive() : presetStyleNormal()
                );
            }
        };

        for (int i = 0; i < presetButtons.length; i++) {
            double preset = presetValues[i];
            presetButtons[i].setOnAction(e -> {
                voltsSpinner.getValueFactory().setValue(preset);
                refresh.run();
            });
        }
        voltsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> refresh.run());
        refresh.run();

        Button applyBtn = new Button("Готово");
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setStyle(
            "-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-background-radius: 6; -fx-padding: 10 16; -fx-font-size: 14px;"
        );
        applyBtn.setOnAction(e -> {
            battery.setVoltage(voltsSpinner.getValue());
            stage.close();
        });

        HBox actions = new HBox(applyBtn);
        actions.setAlignment(Pos.CENTER);
        HBox.setHgrow(applyBtn, Priority.ALWAYS);

        root.getChildren().addAll(
            title,
            subtitle,
            valueLabel,
            wrapLabel("Напряжение U, В", "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #bdc3c7;"),
            voltsSpinner,
            wrapLabel("Быстрый выбор", "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #bdc3c7;"),
            presets,
            formulaCard,
            actions
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #252a31; -fx-background-color: #252a31;");

        Scene scene = new Scene(scroll, WINDOW_WIDTH, 420);
        stage.setScene(scene);
        stage.setMinWidth(420);
        stage.setMinHeight(380);
        stage.setResizable(true);
        stage.showAndWait();
    }

    private static VBox createStatCard(String caption, String value, String accent) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
            "-fx-background-color: #1e2228; -fx-background-radius: 8;"
                + " -fx-border-color: " + accent + "55; -fx-border-radius: 8; -fx-border-width: 1;"
        );
        Label cap = new Label(caption);
        cap.setWrapText(true);
        cap.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
        Label val = new Label(value);
        val.setWrapText(true);
        val.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");
        card.getChildren().addAll(cap, val);
        return card;
    }

    private static Label valueLabelOf(VBox card) {
        return (Label) card.getChildren().get(1);
    }

    private static Label wrapLabel(String text, String style) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(CONTENT_WIDTH);
        label.setStyle(style);
        return label;
    }

    private static String formatVoltage(double volts) {
        if (Math.abs(volts - Math.round(volts)) < 0.05) {
            return String.format("%.0f В", volts);
        }
        return String.format("%.1f В", volts);
    }

    private static String presetStyleNormal() {
        return "-fx-background-color: #3d4654; -fx-text-fill: #ecf0f1;"
            + " -fx-background-radius: 6; -fx-padding: 8 6; -fx-font-size: 12px;";
    }

    private static String presetStyleActive() {
        return "-fx-background-color: #27AE60; -fx-text-fill: white; -fx-font-weight: bold;"
            + " -fx-background-radius: 6; -fx-padding: 8 6; -fx-font-size: 12px;";
    }
}
