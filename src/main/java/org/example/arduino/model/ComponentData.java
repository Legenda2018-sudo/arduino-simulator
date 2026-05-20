package org.example.arduino.model;

public class ComponentData {
    private String type;
    private double x;
    private double y;
    private boolean state;
    private double resistance;

    public ComponentData() {
    }

    public ComponentData(String type, double x, double y, boolean state, double resistance) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.state = state;
        this.resistance = resistance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }

    public double getResistance() {
        return resistance;
    }

    public void setResistance(double resistance) {
        this.resistance = resistance;
    }
}

