package org.example.arduino.model;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class LED extends Component {
    private boolean isOn;
    private boolean input;
    private boolean isBurned; // Перегорел ли LED

    public LED(double x, double y) {
        super(x, y, "LED");
        this.isOn = false;
        this.input = false;
        this.isBurned = false;
        Circle circle = new Circle(x, y, 16);
        circle.setFill(Color.web("#C0392B"));
        circle.setStroke(Color.web("#7B241C"));
        circle.setStrokeWidth(2.5);
        this.shape = circle;
    }

    @Override
    protected void updateShape() {
        if (shape instanceof Circle) {
            Circle circle = (Circle) shape;
            circle.setCenterX(x);
            circle.setCenterY(y);
        }
    }

    public void setOn(boolean on) {
        if (isBurned) {
            this.isOn = false;
            return; // Перегоревший LED не может включиться
        }
        this.isOn = on;
        if (shape instanceof Circle) {
            Circle circle = (Circle) shape;
            if (isBurned) {
                circle.setFill(Color.web("#636E72"));
                circle.setStroke(Color.web("#2D3436"));
                circle.setEffect(null);
            } else if (on) {
                circle.setFill(Color.web("#F1C40F"));
                circle.setStroke(Color.web("#F39C12"));
                DropShadow glow = new DropShadow(18, Color.web("#F39C12", 0.85));
                glow.setSpread(0.35);
                circle.setEffect(glow);
            } else {
                circle.setFill(Color.web("#C0392B"));
                circle.setStroke(Color.web("#7B241C"));
                circle.setEffect(null);
            }
        }
    }
    
    public void burn() {
        this.isBurned = true;
        this.isOn = false;
        if (shape instanceof Circle) {
            Circle circle = (Circle) shape;
            circle.setFill(Color.web("#636E72"));
            circle.setStroke(Color.web("#2D3436"));
            circle.setEffect(null);
        }
    }

    /** Сброс после перегорания (если схема снова безопасна). */
    public void resetBurn() {
        if (!isBurned) {
            return;
        }
        this.isBurned = false;
        setOn(false);
    }
    
    public boolean isBurned() {
        return isBurned;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setInput(boolean input) {
        this.input = input;
        setOn(input);
    }

    public boolean getInput() {
        return input;
    }

    @Override
    public boolean getOutput() {
        return isOn;
    }

    @Override
    public String getType() {
        return "LED";
    }

    @Override
    public Footprint getFootprint() {
        return new Footprint(18, 18);
    }
}

