package org.example.arduino.model;

import javafx.scene.shape.Shape;
import java.util.ArrayList;
import java.util.List;

public abstract class Component {
    protected double x;
    protected double y;
    protected String name;
    protected Shape shape;
    protected List<Component> connections;
    protected boolean isSelected;

    public Component(double x, double y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.connections = new ArrayList<>();
        this.isSelected = false;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
        updateShape();
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
        updateShape();
    }

    public String getName() {
        return name;
    }

    public Shape getShape() {
        return shape;
    }

    public List<Component> getConnections() {
        return connections;
    }

    public void addConnection(Component component) {
        if (!connections.contains(component)) {
            connections.add(component);
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (shape != null) {
            shape.setStrokeWidth(selected ? 4 : 2);
        }
    }

    public boolean contains(double x, double y) {
        if (shape == null) return false;
        // Проверяем расстояние от центра компонента
        double dx = x - this.x;
        double dy = y - this.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (shape instanceof javafx.scene.shape.Circle) {
            javafx.scene.shape.Circle circle = (javafx.scene.shape.Circle) shape;
            return distance <= circle.getRadius() + 5; // Небольшой запас для удобства клика
        } else if (shape instanceof javafx.scene.shape.Rectangle) {
            javafx.scene.shape.Rectangle rect = (javafx.scene.shape.Rectangle) shape;
            return Math.abs(dx) <= rect.getWidth() / 2 + 5 && 
                   Math.abs(dy) <= rect.getHeight() / 2 + 5;
        }
        return false;
    }

    protected abstract void updateShape();

    public abstract String getType();

    public abstract boolean getOutput();
}

