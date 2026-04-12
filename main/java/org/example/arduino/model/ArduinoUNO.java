package org.example.arduino.model;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * Компонент Arduino UNO
 * Имеет цифровые пины (D0-D13) и аналоговые пины (A0-A5)
 */
public class ArduinoUNO extends Component {
    // Пины цифровые (D0-D13)
    private Map<Integer, Boolean> digitalPins;
    // Пины аналоговые (A0-A5)
    private Map<Integer, Double> analogPins;
    // Режимы пинов (true = OUTPUT, false = INPUT)
    private Map<Integer, Boolean> pinModes;
    private boolean isPowered; // Включена ли плата
    private Timer internalTimer; // Встроенный таймер

    public ArduinoUNO(double x, double y) {
        super(x, y, "Arduino UNO");
        this.isPowered = false;
        this.digitalPins = new HashMap<>();
        this.analogPins = new HashMap<>();
        this.pinModes = new HashMap<>();
        
        // Инициализируем все пины
        for (int i = 0; i <= 13; i++) {
            digitalPins.put(i, false);
            pinModes.put(i, true); // По умолчанию OUTPUT
        }
        for (int i = 0; i <= 5; i++) {
            analogPins.put(i, 0.0);
            pinModes.put(100 + i, true); // A0-A5 как 100-105
        }
        
        // Создаем прямоугольную форму для Arduino UNO (больше чем обычные компоненты)
        this.shape = new Rectangle(x - 50, y - 25, 100, 50);
        this.shape.setFill(Color.BLUE);
        this.shape.setStroke(Color.BLACK);
        this.shape.setStrokeWidth(3);
    }

    @Override
    protected void updateShape() {
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            rect.setX(x - 50);
            rect.setY(y - 25);
            
            // Обновляем цвет в зависимости от состояния питания
            if (isPowered) {
                rect.setFill(Color.CYAN);
                rect.setStroke(Color.DARKBLUE);
            } else {
                rect.setFill(Color.DARKBLUE);
                rect.setStroke(Color.BLACK);
            }
        }
    }

    /**
     * Включает/выключает питание Arduino
     */
    public void setPowered(boolean powered) {
        this.isPowered = powered;
        if (!powered) {
            // Выключаем все пины при отключении питания
            for (int i = 0; i <= 13; i++) {
                digitalPins.put(i, false);
            }
            for (int i = 0; i <= 5; i++) {
                analogPins.put(i, 0.0);
            }
        }
        updateShape();
    }

    public boolean isPowered() {
        return isPowered;
    }

    public void toggle() {
        setPowered(!isPowered);
    }

    /**
     * Устанавливает состояние цифрового пина
     */
    public void setDigitalPin(int pin, boolean value) {
        if (pin >= 0 && pin <= 13 && isPowered) {
            if (pinModes.getOrDefault(pin, true)) { // Только если режим OUTPUT
                digitalPins.put(pin, value);
            }
        }
    }

    /**
     * Получает состояние цифрового пина
     */
    public boolean getDigitalPin(int pin) {
        if (pin >= 0 && pin <= 13) {
            return digitalPins.getOrDefault(pin, false);
        }
        return false;
    }

    /**
     * Устанавливает значение аналогового пина
     */
    public void setAnalogPin(int pin, double value) {
        if (pin >= 0 && pin <= 5 && isPowered) {
            if (pinModes.getOrDefault(100 + pin, true)) { // A0-A5 как 100-105
                analogPins.put(pin, Math.max(0.0, Math.min(5.0, value))); // Ограничиваем 0-5В
            }
        }
    }

    /**
     * Получает значение аналогового пина
     */
    public double getAnalogPin(int pin) {
        if (pin >= 0 && pin <= 5) {
            return analogPins.getOrDefault(pin, 0.0);
        }
        return 0.0;
    }

    /**
     * Устанавливает режим пина (INPUT/OUTPUT)
     */
    public void setPinMode(int pin, boolean isOutput) {
        if (pin >= 0 && pin <= 13) {
            pinModes.put(pin, isOutput);
        } else if (pin >= 100 && pin <= 105) {
            pinModes.put(pin, isOutput);
        }
    }

    /**
     * Получает состояние пина для подключения к другим компонентам
     * Используется для получения выхода из конкретного пина
     */
    public boolean getPinOutput(int pin) {
        if (!isPowered) return false;
        
        if (pin >= 0 && pin <= 13) {
            return getDigitalPin(pin);
        }
        return false;
    }

    // Храним входной сигнал
    private boolean inputSignal = false;
    
    /**
     * Устанавливает входной сигнал от подключенных компонентов
     */
    public void setInputSignal(boolean signal) {
        this.inputSignal = signal;
    }
    
    /**
     * Получает выходной сигнал - просто передает входной, если включена
     */
    @Override
    public boolean getOutput() {
        if (!isPowered) return false;
        // Просто передаем входной сигнал на выход
        return inputSignal;
    }

    /**
     * Устанавливает вход на пине (INPUT режим)
     */
    public void setPinInput(int pin, boolean value) {
        if (!isPowered) return;
        
        if (pin >= 0 && pin <= 13) {
            if (!pinModes.getOrDefault(pin, true)) { // Только если режим INPUT
                // В реальном Arduino мы бы читали состояние пина
                // Здесь просто сохраняем для симуляции
            }
        }
    }

    public Timer getInternalTimer() {
        return internalTimer;
    }

    public void setInternalTimer(Timer timer) {
        this.internalTimer = timer;
    }

    @Override
    public String getType() {
        return "Arduino UNO";
    }

    public String getStatusText() {
        if (!isPowered) {
            return "Arduino UNO: ВЫКЛ (включите кликом)";
        }
        if (inputSignal) {
            return "Arduino UNO: ВКЛ, сигнал передается на выход";
        }
        return "Arduino UNO: ВКЛ, ожидает входного сигнала";
    }
}

