package org.example.arduino.model;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class Resistor extends Component {
    private double resistance;
    private boolean input;

    public Resistor(double x, double y) {
        super(x, y, "Resistor");
        this.resistance = 1000.0;
        this.input = false;
        this.shape = new Rectangle(x - 30, y - 10, 60, 20);
        this.shape.setFill(Color.BEIGE);
        this.shape.setStroke(Color.BROWN);
        this.shape.setStrokeWidth(2);
    }

    @Override
    protected void updateShape() {
        if (shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            rect.setX(x - 30);
            rect.setY(y - 10);
        }
    }

    public double getResistance() {
        return resistance;
    }

    public void setResistance(double resistance) {
        this.resistance = resistance;
    }

    public void setInput(boolean input) {
        this.input = input;
    }

    public boolean getInput() {
        return input;
    }

    @Override
    public boolean getOutput() {
        return input;
    }

    @Override
    public String getType() {
        return "Resistor";
    }
}

