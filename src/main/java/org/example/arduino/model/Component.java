package org.example.arduino.model;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import java.util.ArrayList;
import java.util.List;

public abstract class Component {
    protected double x;
    protected double y;
    protected String name;
    /** Визуальный узел на плате (Shape или Group). */
    protected Node shape;
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

    public Node getShape() {
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
        if (shape == null) {
            return;
        }
        Color stroke = selected ? Color.web("#F39C12") : defaultStrokeColor();
        double width = selected ? 3.5 : 2;
        if (shape instanceof Group) {
            Group group = (Group) shape;
            for (Node node : group.getChildren()) {
                if (node instanceof Shape && !(node instanceof javafx.scene.text.Text)) {
                    Shape childShape = (Shape) node;
                    childShape.setStroke(stroke);
                    childShape.setStrokeWidth(width);
                }
            }
            return;
        }
        if (shape instanceof Shape) {
            Shape simpleShape = (Shape) shape;
            simpleShape.setStroke(stroke);
            simpleShape.setStrokeWidth(width);
        }
    }

    private Color defaultStrokeColor() {
        return Color.web("#2D3436");
    }

    public boolean contains(double x, double y) {
        Footprint fp = getFootprint();
        double dx = Math.abs(x - this.x);
        double dy = Math.abs(y - this.y);
        return dx <= fp.halfWidth() + 2 && dy <= fp.halfHeight() + 2;
    }

    /** Прямоугольная область занятости на плате (совпадает с кликом и превью). */
    public Footprint getFootprint() {
        return new Footprint(25, 12);
    }

    public record Footprint(double halfWidth, double halfHeight) {
    }

    protected abstract void updateShape();

    public abstract String getType();

    public abstract boolean getOutput();
}

