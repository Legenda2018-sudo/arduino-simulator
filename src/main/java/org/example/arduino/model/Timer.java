package org.example.arduino.model;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
/**
 * Таймер для Arduino
 * Может генерировать периодические сигналы с заданным интервалом
 */
public class Timer extends Component {
    private long interval; // Интервал в миллисекундах
    private boolean isActive; // Активен ли таймер
    private long lastToggleTime; // Время последнего переключения
    private boolean outputState; // Текущее состояние выхода
    private boolean isRunning; // Запущен ли таймер

    private final Rectangle body;
    private final Text label;

    public Timer(double x, double y) {
        super(x, y, "Timer");
        this.interval = 1000; // По умолчанию 1 секунда
        this.isActive = false;
        this.outputState = false;
        this.isRunning = false;
        this.lastToggleTime = System.currentTimeMillis();
        
        Group group = new Group();
        body = new Rectangle(x - 36, y - 16, 72, 32);
        body.setArcWidth(10);
        body.setArcHeight(10);
        body.setFill(Color.web("#2C3E50"));
        body.setStroke(Color.web("#1A252F"));
        body.setStrokeWidth(2);

        label = new Text(formatInterval(interval));
        label.setFont(Font.font("Consolas", 11));
        label.setFill(Color.web("#ECF0F1"));
        centerLabel();

        group.getChildren().addAll(body, label);
        this.shape = group;
    }

    private String formatInterval(long ms) {
        if (ms >= 1000 && ms % 1000 == 0) {
            return (ms / 1000) + "s";
        }
        return ms + "ms";
    }

    private void centerLabel() {
        double w = label.getLayoutBounds().getWidth();
        if (w <= 0) {
            w = 24;
        }
        label.setX(x - w / 2);
        label.setY(y + 4);
    }

    @Override
    protected void updateShape() {
        body.setX(x - 36);
        body.setY(y - 16);
        centerLabel();
    }
    /**
     * Обновляет состояние таймера
     * Вызывается каждый кадр симуляции
     */
    public void update(long currentTime) {
        if (!isActive || !isRunning) {
            outputState = false;
            updateVisual();
            return;
        }

        if (currentTime - lastToggleTime >= interval) {
            outputState = !outputState;
            lastToggleTime = currentTime;
            updateVisual();
        }
    }

    /**
     * Обновляет визуальное отображение таймера
     */
    private void updateVisual() {
        if (!isActive || !isRunning) {
            body.setFill(Color.web("#2C3E50"));
            body.setStroke(Color.web("#1A252F"));
        } else if (outputState) {
            body.setFill(Color.web("#E67E22"));
            body.setStroke(Color.web("#CA6F1E"));
        } else {
            body.setFill(Color.web("#34495E"));
            body.setStroke(Color.web("#2C3E50"));
        }
    }
    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        if (interval < 100) {
            interval = 100; // Минимальный интервал 100 мс
        }
        this.interval = interval;
        label.setText(formatInterval(interval));
        centerLabel();
    }
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (active) {
            lastToggleTime = System.currentTimeMillis();
        }
        updateVisual();
    }

    public void toggle() {
        setActive(!isActive);
    }

    public void start() {
        this.isRunning = true;
        this.outputState = true;
        this.lastToggleTime = System.currentTimeMillis();
        updateVisual();
    }

    public void stop() {
        this.isRunning = false;
        this.outputState = false;
        updateVisual();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean getOutputState() {
        return outputState;
    }

    @Override
    public boolean getOutput() {
        return isActive && isRunning && outputState;
    }

    @Override
    public String getType() {
        return "Timer";
    }

    @Override
    public Footprint getFootprint() {
        return new Footprint(36, 16);
    }

    public String getStatusText() {
        if (!isActive) {
            return "Таймер ВЫКЛ";
        } else if (!isRunning) {
            return "Таймер ОСТАНОВЛЕН";
        } else {
            return "Интервал: " + interval + " мс, Выход: " + (outputState ? "ВКЛ" : "ВЫКЛ");
        }
    }
}

