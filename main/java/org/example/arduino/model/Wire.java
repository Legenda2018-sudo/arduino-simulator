package org.example.arduino.model;

import javafx.scene.shape.Line;
import javafx.scene.paint.Color;

public class Wire {
    private Component from;
    private Component to;
    private Line line;
    private boolean isActive;

    public Wire(Component from, Component to) {
        this.from = from;
        this.to = to;
        this.isActive = false;
        createLine();
    }

    private void createLine() {
        line = new Line(from.getX(), from.getY(), to.getX(), to.getY());
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(3);
        updateLine();
    }

    public void updateLine() {
        if (line != null) {
            line.setStartX(from.getX());
            line.setStartY(from.getY());
            line.setEndX(to.getX());
            line.setEndY(to.getY());
            if (isActive) {
                line.setStroke(Color.RED);
            } else {
                line.setStroke(Color.BLACK);
            }
        }
    }

    public Component getFrom() {
        return from;
    }

    public Component getTo() {
        return to;
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
}

