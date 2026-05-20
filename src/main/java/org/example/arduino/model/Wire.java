package org.example.arduino.model;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;

public class Wire {
    private final WireAnchor fromAnchor;
    private final WireAnchor toAnchor;
    private Line line;
    private boolean isActive;

    public Wire(WireAnchor fromAnchor, WireAnchor toAnchor) {
        this.fromAnchor = fromAnchor;
        this.toAnchor = toAnchor;
        this.isActive = false;
        createLine();
    }

    /** Совместимость: провод между центрами компонентов. */
    public Wire(Component from, Component to) {
        this(WireAnchor.componentCenter(from), WireAnchor.componentCenter(to));
    }

    private void createLine() {
        line = new Line(fromAnchor.getX(), fromAnchor.getY(), toAnchor.getX(), toAnchor.getY());
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        updateLine();
    }

    public void updateLine() {
        if (line != null) {
            line.setStartX(fromAnchor.getX());
            line.setStartY(fromAnchor.getY());
            line.setEndX(toAnchor.getX());
            line.setEndY(toAnchor.getY());
            if (isActive) {
                line.setStroke(Color.web("#FF6B35"));
                line.setStrokeWidth(4);
            } else if (fromAnchor.isRailPlus() || toAnchor.isRailPlus()) {
                line.setStroke(Color.web("#E74C3C"));
                line.setStrokeWidth(3);
            } else if (fromAnchor.isRailMinus() || toAnchor.isRailMinus()) {
                line.setStroke(Color.web("#3498DB"));
                line.setStrokeWidth(3);
            } else {
                line.setStroke(Color.web("#2C3E50"));
                line.setStrokeWidth(2.5);
            }
        }
    }

    public WireAnchor getFromAnchor() {
        return fromAnchor;
    }

    public WireAnchor getToAnchor() {
        return toAnchor;
    }

    public Component getFrom() {
        return fromAnchor.getComponent();
    }

    public Component getTo() {
        return toAnchor.getComponent();
    }

    public Line getLine() {
        return line;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        updateLine();
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean connects(WireAnchor a, WireAnchor b) {
        return (fromAnchor.isSameEndpoint(a) && toAnchor.isSameEndpoint(b))
            || (fromAnchor.isSameEndpoint(b) && toAnchor.isSameEndpoint(a));
    }
}

