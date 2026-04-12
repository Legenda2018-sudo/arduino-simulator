package org.example.arduino.model;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

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

    public Timer(double x, double y) {
        super(x, y, "Timer");
        this.interval = 1000; // По умолчанию 1 секунда
        this.isActive = false;
        this.outputState = false;
        this.isRunning = false;
        this.lastToggleTime = System.currentTimeMillis();
        
        // Создаем прямоугольную форму для таймера
        this.shape = new Rectangle(x - 35, y - 15, 70, 30);
        this.shape.setFill(Color.DARKBLUE);
        this.shape.setStroke(Color.BLACK);
        this.shape.setStrokeWidth(2);
    }

    @Override
    protected void updateShape() {
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            rect.setX(x - 35);
            rect.setY(y - 15);
        }
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
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            if (!isActive || !isRunning) {
                rect.setFill(Color.DARKBLUE);
            } else if (outputState) {
                rect.setFill(Color.LIGHTBLUE);
            } else {
                rect.setFill(Color.DARKBLUE);
            }
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

