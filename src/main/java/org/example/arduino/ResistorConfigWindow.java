package org.example.arduino;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.arduino.model.Resistor;
import org.example.arduino.model.Wire;

import java.util.List;
import org.example.arduino.util.CircuitAnalyzer;
import org.example.arduino.util.CircuitPhysics;
import org.example.arduino.util.ResistorColorCode;

/**
 * Окно настройки номинала резистора.
 */
public final class ResistorConfigWindow {

    private static final double WINDOW_WIDTH = 500;
    private static final double CONTENT_WIDTH = WINDOW_WIDTH - 48;

    private ResistorConfigWindow() {
    }

    public static void showConfig(Resistor resistor) {
        showConfig(resistor, null, null);
    }

    public static void showConfig(Resistor resistor, List<org.example.arduino.model.Component> components, List<Wire> wires) {
        if (resistor == null) {
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Настройка резистора");

        VBox root = new VBox(14);
        root.setPadding(new Insets(22, 24, 24, 24));
        root.setFillWidth(true);
        root.setStyle("-fx-background-color: #252a31;");

        Label title = new Label("Резистор");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Label subtitle = wrapLabel(
            "Меняйте сопротивление стрелками у поля ввода или кнопками быстрого выбора. "
                + "Допустимы значения ряда E12 (220 → 270 → 330…).",
            "-fx-font-size: 12px; -fx-text-fill: #95a5a6;"
        );

        VBox previewBox = new VBox(10);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setPadding(new Insets(14, 14, 14, 14));
        previewBox.setMaxWidth(CONTENT_WIDTH);
        previewBox.setStyle("-fx-background-color: #1e2228; -fx-background-radius: 8;");

        Group bandPreview = new Group();
        HBox previewCenter = new HBox(bandPreview);
        previewCenter.setAlignment(Pos.CENTER);
        previewCenter.setPadding(new Insets(4, 0, 0, 0));
        previewBox.getChildren().add(previewCenter);

        Label valueLabel = new Label();
        valueLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #E67E22;");

        Label markingLabel = new Label();
        markingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #bdc3c7;");

        previewBox.getChildren().addAll(valueLabel, markingLabel);

        Label spinnerCaption = new Label("Сопротивление");
        spinnerCaption.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #bdc3c7;");

        SpinnerValueFactory.DoubleSpinnerValueFactory valueFactory =
            new SpinnerValueFactory.DoubleSpinnerValueFactory(10.0, 1_000_000.0, resistor.getResistance(), 10.0) {
                @Override
                public void decrement(int steps) {
                    setValue(ResistorColorCode.stepE12(getValue(), -steps));
                }

                @Override
                public void increment(int steps) {
                    setValue(ResistorColorCode.stepE12(getValue(), steps));
                }
            };
        Spinner<Double> ohmsSpinner = new Spinner<>(valueFactory);
        ohmsSpinner.setEditable(true);
        ohmsSpinner.setMaxWidth(Double.MAX_VALUE);
        ohmsSpinner.setStyle(
            "-fx-font-size: 16px; -fx-background-color: #1e2228; -fx-text-fill: #ecf0f1;"
        );

        Label presetsCaption = new Label("Быстрый выбор");
        presetsCaption.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #bdc3c7;");

        GridPane presets = new GridPane();
        presets.setHgap(8);
        presets.setVgap(8);
        ColumnConstraints presetCol = new ColumnConstraints();
        presetCol.setPercentWidth(25);
        presetCol.setHgrow(Priority.ALWAYS);
        presets.getColumnConstraints().addAll(presetCol, presetCol, presetCol, presetCol);

        Button[] presetButtons = new Button[4];
        double[] presetValues = { 220, 330, 470, 1000 };
        for (int i = 0; i < presetValues.length; i++) {
            double preset = presetValues[i];
            Button b = new Button(formatPreset(preset));
            b.setMaxWidth(Double.MAX_VALUE);
            b.setMinHeight(34);
            presetButtons[i] = b;
            presets.add(b, i, 0);
        }

        GridPane stats = new GridPane();
        stats.setHgap(10);
        stats.setVgap(10);
        ColumnConstraints halfCol = new ColumnConstraints();
        halfCol.setPercentWidth(50);
        halfCol.setHgrow(Priority.ALWAYS);
        stats.getColumnConstraints().addAll(halfCol, halfCol);

        VBox currentCard = createStatCard("Ток через LED", "—", "#1ABC9C");
        VBox statusCard = createStatCard("Оценка", "—", "#3498DB");
        Label currentCaption = captionLabelOf(currentCard);
        Label currentValue = valueLabelOf(currentCard);
        Label statusValue = valueLabelOf(statusCard);
        stats.add(currentCard, 0, 0);
        stats.add(statusCard, 1, 0);

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

        Runnable refresh = () -> {
            double r = ohmsSpinner.getValue() != null ? ohmsSpinner.getValue() : resistor.getResistance();
            resistor.setResistance(r);
            ohmsSpinner.getValueFactory().setValue(resistor.getResistance());

            updateBandPreview(bandPreview, resistor.getResistance());
            valueLabel.setText(formatResistance(resistor.getResistance()));
            markingLabel.setText("Цветовая маркировка: " + resistor.getColorCodeText());

            CircuitPhysics.CalcResult calc = CircuitPhysics.analyze(
                CircuitAnalyzer.parallelGroupOhms(resistor, components, wires));
            boolean parallelInCircuit = components != null && wires != null
                && Math.abs(calc.getResistorOhms() - resistor.getResistance()) > 0.5;
            currentCaption.setText(parallelInCircuit ? "Ток в цепи (R паралл.)" : "Ток через LED");
            currentValue.setText(formatCurrent(calc.getCurrentMa()));
            statusValue.setText(calc.getSafety().getMessage());

            String parallelHint = parallelInCircuit
                ? String.format(
                    "Параллельно с другим R → в цепи R ≈ %.0f Ом. ",
                    calc.getResistorOhms())
                : "";
            statusRow.setStyle(
                "-fx-background-color: " + calc.getSafety().getColor() + "22;"
                    + " -fx-background-radius: 8;"
                    + " -fx-border-color: " + calc.getSafety().getColor() + ";"
                    + " -fx-border-radius: 8;"
                    + " -fx-border-width: 1.5;"
            );
            statusDot.setStyle("-fx-background-color: " + calc.getSafety().getColor() + "; -fx-background-radius: 5;");
            statusHint.setText(parallelHint + String.format(
                "Для LED обычно ставят ≈ %.0f Ом (ток %.0f мА). Безопасный предел — до %.0f мА.",
                calc.getRecommendedOhms(),
                CircuitPhysics.LED_I_NOM_MA,
                CircuitPhysics.LED_I_MAX_MA
            ));

            double selected = resistor.getResistance();
            for (int i = 0; i < presetButtons.length; i++) {
                boolean active = Math.abs(presetValues[i] - selected) < 0.5;
                presetButtons[i].setStyle(active ? presetStyleActive() : presetStyleNormal());
            }
        };

        ohmsSpinner.valueProperty().addListener((o, oldV, newV) -> {
            if (newV != null) {
                resistor.setResistance(ResistorColorCode.snapToE12(newV));
                refresh.run();
            }
        });

        for (int i = 0; i < presetButtons.length; i++) {
            double preset = presetValues[i];
            presetButtons[i].setOnAction(e -> {
                ohmsSpinner.getValueFactory().setValue(preset);
                refresh.run();
            });
        }

        Button close = new Button("Готово");
        close.setMaxWidth(Double.MAX_VALUE);
        close.setMinHeight(36);
        close.setStyle(
            "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-background-radius: 6; -fx-padding: 10 16; -fx-font-size: 13px;"
        );
        close.setOnAction(e -> stage.close());

        root.getChildren().addAll(
            title,
            subtitle,
            previewBox,
            spinnerCaption,
            ohmsSpinner,
            presetsCaption,
            presets,
            stats,
            statusRow,
            close
        );

        refresh.run();

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: #252a31; -fx-background: #252a31;");

        stage.setScene(new Scene(scroll, WINDOW_WIDTH, 620));
        stage.setMinWidth(460);
        stage.setMinHeight(480);
        stage.setResizable(true);
        stage.showAndWait();
    }

    private static void updateBandPreview(Group group, double ohms) {
        group.getChildren().clear();
        Color[] colors = ResistorColorCode.bandsForOhms(ohms);

        Circle legLeft = new Circle(-12, 16, 5);
        legLeft.setFill(Color.web("#C0C0C0"));
        legLeft.setStroke(Color.web("#666666"));

        Circle legRight = new Circle(152, 16, 5);
        legRight.setFill(Color.web("#C0C0C0"));
        legRight.setStroke(Color.web("#666666"));

        Rectangle body = new Rectangle(0, 0, 140, 32);
        body.setArcWidth(8);
        body.setArcHeight(8);
        body.setFill(Color.web("#F5E6C8"));
        body.setStroke(Color.web("#8B6914"));
        body.setStrokeWidth(1.5);

        group.getChildren().addAll(legLeft, legRight, body);

        double startX = 22;
        for (int i = 0; i < colors.length; i++) {
            Rectangle band = new Rectangle(startX + i * 24, 6, 10, 20);
            band.setFill(colors[i]);
            band.setStroke(Color.web("#00000033"));
            group.getChildren().add(band);
        }
    }

    private static Label captionLabelOf(VBox card) {
        return (Label) card.getChildren().get(0);
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
        val.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");
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

    private static String formatResistance(double ohms) {
        if (ohms >= 1000) {
            return String.format("%.1f кОм", ohms / 1000.0);
        }
        return String.format("%.0f Ом", ohms);
    }

    private static String formatPreset(double ohms) {
        if (ohms >= 1000) {
            return (int) (ohms / 1000) + " кОм";
        }
        return (int) ohms + " Ом";
    }

    private static String formatCurrent(double currentMa) {
        if (Double.isInfinite(currentMa)) {
            return "∞ мА";
        }
        if (currentMa <= 0) {
            return "0 мА";
        }
        if (currentMa < 0.1) {
            return String.format("%.3f мА", currentMa);
        }
        return String.format("%.1f мА", currentMa);
    }

    private static String presetStyleNormal() {
        return "-fx-background-color: #3d4654; -fx-text-fill: #ecf0f1;"
            + " -fx-background-radius: 6; -fx-padding: 8 6; -fx-font-size: 12px;";
    }

    private static String presetStyleActive() {
        return "-fx-background-color: #E67E22; -fx-text-fill: white; -fx-font-weight: bold;"
            + " -fx-background-radius: 6; -fx-padding: 8 6; -fx-font-size: 12px;";
    }
}
