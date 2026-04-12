package org.example.arduino.model;

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
        this.shape = new Circle(x, y, 18);
        this.shape.setFill(Color.RED);
        this.shape.setStroke(Color.BLACK);
        this.shape.setStrokeWidth(2);
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
                circle.setFill(Color.DARKGRAY);
                circle.setStroke(Color.BLACK);
            } else if (on) {
                circle.setFill(Color.YELLOW);
                circle.setStroke(Color.ORANGE);
            } else {
                circle.setFill(Color.RED);
                circle.setStroke(Color.BLACK);
            }
        }
    }
    
    public void burn() {
        this.isBurned = true;
        this.isOn = false;
        if (shape instanceof Circle) {
            Circle circle = (Circle) shape;
            circle.setFill(Color.DARKGRAY);
            circle.setStroke(Color.BLACK);
        }
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
}

