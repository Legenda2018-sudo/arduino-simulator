package org.example.arduino;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.arduino.model.Timer;

/**
 * Окно настройки таймера.
 */
public final class TimerConfigWindow {

    private static final double WINDOW_WIDTH = 500;
    private static final double CONTENT_WIDTH = WINDOW_WIDTH - 48;

    private TimerConfigWindow() {
    }

    public static void showConfig(Timer timer) {
        if (timer == null) {
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Настройка таймера");

        VBox root = new VBox(14);
        root.setPadding(new Insets(22, 24, 24, 24));
        root.setFillWidth(true);
        root.setStyle("-fx-background-color: #252a31;");

        Label title = new Label("Таймер");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        Label subtitle = wrapLabel(
            "Задаёт интервал мигания сигнала. Укажите период стрелками или кнопками, "
                + "затем нажмите «Старт» во время симуляции.",
            "-fx-font-size: 12px; -fx-text-fill: #95a5a6;"
        );

        VBox previewBox = new VBox(8);
        previewBox.setAlignment(Pos.CENTER);
        previewBox.setPadding(new Insets(16, 14, 16, 14));
        previewBox.setMaxWidth(CONTENT_WIDTH);
        previewBox.setStyle("-fx-background-color: #1e2228; -fx-background-radius: 8;");

        Region timerBody = new Region();
        timerBody.setMinSize(88, 36);
        timerBody.setMaxSize(88, 36);
        timerBody.setStyle("-fx-background-color: #2C3E50; -fx-background-radius: 10; -fx-border-color: #1A252F; -fx-border-radius: 10; -fx-border-width: 2;");

        Label intervalLabel = new Label();
        intervalLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #E67E22;");

        Label intervalSub = new Label();
        intervalSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #bdc3c7;");

        previewBox.getChildren().addAll(timerBody, intervalLabel, intervalSub);

        Label spinnerCaption = sectionCaption("Интервал, мс");
        Spinner<Integer> intervalSpinner = new Spinner<>(100, 10000, (int) timer.getInterval(), 100);
        intervalSpinner.setEditable(true);
        intervalSpinner.setMaxWidth(Double.MAX_VALUE);
        intervalSpinner.setStyle("-fx-font-size: 16px; -fx-background-color: #1e2228; -fx-text-fill: #ecf0f1;");
        intervalSpinner.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commitSpinnerEditorText(intervalSpinner);
            }
        });
        intervalSpinner.getEditor().setOnAction(e -> commitSpinnerEditorText(intervalSpinner));

        Label presetsCaption = sectionCaption("Быстрый выбор");
        GridPane presets = new GridPane();
        presets.setHgap(8);
        presets.setVgap(8);
        ColumnConstraints presetCol = new ColumnConstraints();
        presetCol.setPercentWidth(25);
        presetCol.setHgrow(Priority.ALWAYS);
        presets.getColumnConstraints().addAll(presetCol, presetCol, presetCol, presetCol);

        int[] presetValues = { 200, 500, 1000, 3000 };
        String[] presetLabels = { "200 мс", "500 мс", "1 с", "3 с" };
        Button[] presetButtons = new Button[presetValues.length];
        for (int i = 0; i < presetValues.length; i++) {
            Button b = new Button(presetLabels[i]);
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

        VBox stateCard = createStatCard("Состояние", "—", "#3498DB");
        VBox outputCard = createStatCard("Выход сейчас", "—", "#1ABC9C");
        Label stateValue = valueLabelOf(stateCard);
        Label outputValue = valueLabelOf(outputCard);
        stats.add(stateCard, 0, 0);
        stats.add(outputCard, 1, 0);

        HBox controls = new HBox(10);
        Button startButton = new Button("Старт");
        Button stopButton = new Button("Стоп");
        startButton.setMaxWidth(Double.MAX_VALUE);
        stopButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(startButton, Priority.ALWAYS);
        HBox.setHgrow(stopButton, Priority.ALWAYS);
        startButton.setMinHeight(36);
        stopButton.setMinHeight(36);
        startButton.setStyle(actionStyle("#27AE60"));
        stopButton.setStyle(actionStyle("#E74C3C"));
        controls.getChildren().addAll(startButton, stopButton);

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
        close.setStyle(actionStyle("#27AE60"));
        close.setOnAction(e -> stage.close());

        Runnable refresh = () -> {
            long ms = timer.getInterval();
            intervalLabel.setText(formatInterval(ms));
            intervalSub.setText(ms >= 1000 && ms % 1000 == 0
                ? "Период: " + (ms / 1000) + " секунды"
                : "Период: " + ms + " миллисекунд");

            updateTimerPreview(timerBody, timer);

            if (!timer.isActive()) {
                stateValue.setText("Выключен");
            } else if (!timer.isRunning()) {
                stateValue.setText("Остановлен");
            } else {
                stateValue.setText("Работает");
            }

            outputValue.setText(timer.getOutputState() ? "ВКЛ" : "ВЫКЛ");

            String accent;
            String hint;
            if (!timer.isActive()) {
                accent = "#E74C3C";
                hint = "Таймер выключен. Нажмите «Старт» и запустите «Симуляцию» на плате.";
            } else if (!timer.isRunning()) {
                accent = "#F39C12";
                hint = "Таймер включён, но не запущен. Нажмите «Старт» или запустите симуляцию.";
            } else {
                accent = "#27AE60";
                hint = "Таймер мигает с заданным интервалом. Подключите его к LED проводом.";
            }

            statusRow.setStyle(
                "-fx-background-color: " + accent + "22;"
                    + " -fx-background-radius: 8;"
                    + " -fx-border-color: " + accent + ";"
                    + " -fx-border-radius: 8;"
                    + " -fx-border-width: 1.5;"
            );
            statusDot.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 5;");
            statusHint.setText(hint);

            int selected = (int) timer.getInterval();
            for (int i = 0; i < presetButtons.length; i++) {
                boolean active = presetValues[i] == selected;
                presetButtons[i].setStyle(active ? presetStyleActive() : presetStyleNormal());
            }
        };

        intervalSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal >= 100 && newVal <= 10000) {
                timer.setInterval(newVal);
                refresh.run();
            }
        });

        for (int i = 0; i < presetButtons.length; i++) {
            int preset = presetValues[i];
            presetButtons[i].setOnAction(e -> {
                intervalSpinner.getValueFactory().setValue(preset);
                timer.setInterval(preset);
                refresh.run();
            });
        }

        startButton.setOnAction(e -> {
            timer.setActive(true);
            timer.start();
            refresh.run();
        });

        stopButton.setOnAction(e -> {
            timer.setActive(false);
            timer.stop();
            refresh.run();
        });

        root.getChildren().addAll(
            title,
            subtitle,
            previewBox,
            spinnerCaption,
            intervalSpinner,
            presetsCaption,
            presets,
            stats,
            controls,
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

    private static void updateTimerPreview(Region body, Timer timer) {
        String fill;
        String border;
        if (!timer.isActive() || !timer.isRunning()) {
            fill = "#2C3E50";
            border = "#1A252F";
        } else if (timer.getOutputState()) {
            fill = "#E67E22";
            border = "#CA6F1E";
        } else {
            fill = "#34495E";
            border = "#2C3E50";
        }
        body.setStyle(
            "-fx-background-color: " + fill + "; -fx-background-radius: 10;"
                + " -fx-border-color: " + border + "; -fx-border-radius: 10; -fx-border-width: 2;"
        );
    }

    private static void commitSpinnerEditorText(Spinner<Integer> spinner) {
        if (spinner == null || spinner.getEditor() == null || spinner.getValueFactory() == null) {
            return;
        }
        String text = spinner.getEditor().getText();
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 100) {
                value = 100;
            }
            if (value > 10000) {
                value = 10000;
            }
            spinner.getValueFactory().setValue(value);
        } catch (NumberFormatException ignored) {
            spinner.getEditor().setText(String.valueOf(spinner.getValue()));
        }
    }

    private static String formatInterval(long ms) {
        if (ms >= 1000 && ms % 1000 == 0) {
            return (ms / 1000) + " с";
        }
        return ms + " мс";
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

    private static String actionStyle(String color) {
        return "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold;"
            + " -fx-background-radius: 6; -fx-padding: 10 16; -fx-font-size: 13px;";
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
